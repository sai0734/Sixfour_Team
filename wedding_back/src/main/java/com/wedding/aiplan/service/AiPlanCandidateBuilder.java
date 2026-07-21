package com.wedding.aiplan.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.aiplan.dto.AiPlanPackageCandidateDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.CompanyPackage;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.HallType;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.domain.StudioDetail;
import com.wedding.company.repository.CompanyPackageRepository;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.company.repository.HallDetailRepository;
import com.wedding.company.repository.HallItemRepository;
import com.wedding.company.repository.MakeupPackageRepository;
import com.wedding.company.repository.StudioDetailRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 빠르게/자세히 모드 둘 다 이걸 통해서 후보를 뽑음 (AiPlanQuickServiceImpl / AiPlanDetailServiceImpl에서 호출).
// 패키지(CompanyPackage)는 팀에서 취소했던 아이디어라 실제 운영 기능이 아니고, 카테고리별 취향까지
// 반영할 방법이 없어서 - 취향이 하나라도 있으면 패키지는 건너뛰고 바로 개별 조합으로 감.
@Component
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class AiPlanCandidateBuilder {

    private final CompanyPackageRepository companyPackageRepository;
    private final CompanyRepository companyRepository;
    private final HallDetailRepository hallDetailRepository;
    private final HallItemRepository hallItemRepository;
    private final StudioDetailRepository studioDetailRepository;
    private final DressItemRepository dressItemRepository;
    private final MakeupPackageRepository makeupPackageRepository;

    private static final int MAX_CANDIDATES = 5;

    // 메이크업 패키지 타입 매칭 후보 풀 크기 - 현재 더미데이터가 메이크업 업체 10곳뿐이라 넉넉히 잡음
    private static final int MAKEUP_POOL_SIZE = 30;
    private static final double BUDGET_WEIGHT = 0.7;
    private static final double DISTANCE_WEIGHT = 0.3;

    // 예산의 110%까지는 "적합한 패키지"로 인정
    private static final double PACKAGE_FIT_TOLERANCE = 1.1;

    // 개별 조합 시 카테고리별 예산 배분 비율 - 데모용 임의 가정치
    // (AiPlanAiServiceImpl / AiPlanRefineServiceImpl에서도 재사용)
    static final double HALL_RATIO = 0.45;
    static final double DRESS_RATIO = 0.25;
    static final double STUDIO_RATIO = 0.15;
    static final double MAKEUP_RATIO = 0.15;

    // 개별 조합 합계가 예산보다 이만큼(500만원) 넘게 모자라면, 카테고리 캡을 넘어서라도
    // 더 비싼 대안으로 갈아타서 합계를 예산 가까이로 끌어올린다.
    // AiPlanAiServiceImpl(AI 모드)도 동일한 업그레이드 폭/순서를 재사용한다 (같은 패키지라 접근 가능하게 package-private).
    static final long BUDGET_TOLERANCE = 5_000_000L;
    static final List<CompanyCategory> UPGRADE_ORDER =
            List.of(CompanyCategory.HALL, CompanyCategory.DRESS, CompanyCategory.STUDIO, CompanyCategory.MAKEUP);

    public AiPlanQuickResultDTO recommend(String region, Long budget, Integer guestCount,
                                          AiPlanCategoryPreferences prefs) {
        return recommend(region, budget, guestCount, prefs, 0);
    }

    // relaxationStage: 예산 초과로 조건을 내려놓고 재귀 호출한 횟수(0=원래 요청, 1=메이크업만 남김,
    // 2=완전히 다 내려놓음). 메이크업 취향을 우선 지키려고 재귀를 한 단계 늘리면서, prefs.isEmpty()
    // 만으로 "더 내려놓을 게 없는 상태"를 판단할 수 없게 됐다(메이크업만 남은 prefs는 절대 empty가
    // 아니라서 무한 재귀에 빠짐) - 그래서 이 카운터로 직접 재귀 깊이를 2단계로 못박아 둔다.
    private AiPlanQuickResultDTO recommend(String region, Long budget, Integer guestCount,
                                           AiPlanCategoryPreferences prefs, int relaxationStage) {

        if (prefs.isEmpty()) {
            List<CompanyPackage> fittingPackages = findFittingPackages(region, budget, guestCount);

            if (!fittingPackages.isEmpty()) {
                List<AiPlanPackageCandidateDTO> candidates = fittingPackages.stream()
                        .sorted(Comparator.comparingDouble(p -> packageScore(p, budget)))
                        .limit(MAX_CANDIDATES)
                        .map(p -> toPackageCandidateDTO(p, budget))
                        .toList();

                return AiPlanQuickResultDTO.builder()
                        .candidates(candidates)
                        .regionRelaxed(false)
                        .message(null)
                        .build();
            }
        }

        log.info("AiPlan: no fitting package or preferences given (region='{}', budget={}, prefsEmpty={}), "
                + "assembling individual combo", region, budget, prefs.isEmpty());

        ComboResult combo = buildIndividualCombo(region, budget, guestCount, prefs);

        if (combo == null) {
            return AiPlanQuickResultDTO.builder()
                    .candidates(List.of())
                    .regionRelaxed(false)
                    .message("추천할 수 있는 업체가 아직 없어요.")
                    .build();
        }

        // 하객수/취향(홀타입, 스타일 등) 같은 조건 때문에 합계가 예산을 톨러런스 넘게 초과했으면,
        // 이 조합을 바로 보여주는 대신 예산 안에서(하객수/취향은 다 내려놓고) 고른 대안을 먼저
        // 보여주고 얼마를 더 잡아야 원래 조건에 맞는 곳을 볼 수 있는지 알려준다. 이미 하객수도
        // 없고 취향도 없는 상태(=이미 최대한 완화된 상태)에서도 초과했다면 더 내려놓을 게 없다는
        // 뜻이라(무한 재귀 방지 겸) 그냥 지나간다 - 아래 appendBudgetGapMessage가 안내한다.
        // 프론트 "예산 늘려서 다시 찾기" 버튼이 그 금액으로 예산을 채워 원래 조건 그대로 재요청하면,
        // 이번에 계산한 이 조합이 그대로 나온다.
        boolean alreadyFullyRelaxed = relaxationStage >= 2 || (guestCount == null && prefs.isEmpty());
        if (!alreadyFullyRelaxed && budget != null && budget > 0) {
            long gap = combo.dto.getPackagePrice().longValue() - budget;
            if (gap > BUDGET_TOLERANCE) {
                // 홀타입/스튜디오/드레스 취향은 가격 영향이 커서 예산 초과 시 다 내려놓지만, 메이크업
                // 패키지 타입은 업체마다 가격 차이가 몇 십만원 수준이라 웬만하면 계속 지킬 수 있다.
                // 다 내려놓고 재검색하면(기존 방식) "FULL 요청했는데 전혀 상관없는 패키지가 나온다"는
                // 문제가 생기므로, 1단계(relaxationStage 0→1)에서는 메이크업 취향만은 유지한다.
                // 이마저도 예산을 못 맞추면 2단계(1→2)에서 완전히 다 내려놓고 재검색한다 -
                // relaxationStage가 2에 도달하면 위 alreadyFullyRelaxed가 더 이상 재귀를 안 하므로
                // 무한 재귀는 안 생긴다.
                AiPlanCategoryPreferences relaxed = relaxationStage == 0
                        ? AiPlanCategoryPreferences.of(List.of(), List.of(), List.of(), prefs.getMakeupType())
                        : AiPlanCategoryPreferences.empty();
                AiPlanQuickResultDTO budgetFit = recommend(region, budget, null, relaxed, relaxationStage + 1);
                if (!budgetFit.getCandidates().isEmpty()) {
                    budgetFit.setSuggestedBudget(budget + gap);
                    budgetFit.setMessage(String.format(
                            "우선 예산에 맞는 조합으로 보여드렸어요. 예산을 %,d원 더 늘리면 요청하신 조건에 "
                                    + "더 맞는 곳을 찾아드릴 수 있어요.",
                            gap));
                    return budgetFit;
                }
            }
        }

        StringBuilder message = new StringBuilder();
        if (prefs.isEmpty()) {
            message.append("조건에 맞는 패키지가 없어서 업체를 개별로 조합해 추천했어요.");
        } else {
            message.append("입력해주신 취향에 맞춰 업체를 개별로 조합해 추천했어요.");
        }
        if (combo.usedRegionFallback) {
            message.append(" 일부는 지역 조건 밖에서 골랐어요.");
        }
        if (combo.usedStyleFallback) {
            message.append(" 일부는 취향과 딱 맞는 곳이 없어서 예산/지역 기준으로 대신 골랐어요.");
        }
        appendBudgetGapMessage(message, combo.dto.getPackagePrice(), budget);

        return AiPlanQuickResultDTO.builder()
                .candidates(List.of(combo.dto))
                .regionRelaxed(combo.usedRegionFallback)
                .message(message.toString())
                .build();
    }

    // 하객수처럼 양보 못 하는 조건이나 예산 자체가 너무 적을 때, 합계가 예산을 톨러런스(500만원)
    // 넘게 초과할 수 있다 - fillTowardBudget은 "부족한 예산을 더 쓰는" 방향의 업그레이드만 하고
    // 이미 초과한 걸 깎진 않으므로, 여기서 초과분을 계산해서 "이만큼 더 잡으면 이 조합으로 예약
    // 가능해요"라고 알려준다. AiPlanAiServiceImpl(AI 모드)도 같은 문구를 재사용 (package-private).
    void appendBudgetGapMessage(StringBuilder message, BigDecimal totalPrice, Long budget) {
        if (budget == null || budget <= 0 || totalPrice == null) {
            return;
        }
        long gap = totalPrice.longValue() - budget;
        if (gap > BUDGET_TOLERANCE) {
            message.append(String.format(
                    " 입력하신 조건에 맞는 조합을 찾다 보니 예산보다 %,d원 더 필요해요. 예산을 이만큼 늘리면 이 조합으로 예약할 수 있어요.",
                    gap));
        }
    }

    // ── 패키지 경로 (빠르게 모드, 취향 없을 때만) ──────────────────────────

    private List<CompanyPackage> findFittingPackages(String region, Long budget, Integer guestCount) {

        List<CompanyPackage> pool = region != null
                ? companyPackageRepository.findAllActiveByHallRegion(region)
                : companyPackageRepository.findAllActive();

        if (guestCount != null) {
            pool = pool.stream().filter(p -> fitsGuestCount(p.getHallCompany(), guestCount)).toList();
        }

        if (budget == null || budget <= 0) {
            return pool;
        }

        BigDecimal upperLimit = BigDecimal.valueOf(budget).multiply(BigDecimal.valueOf(PACKAGE_FIT_TOLERANCE));

        // 상한선만 있으면 예산보다 한참 싼 패키지도 "적합"으로 통과해버린다. 패키지는 미리 고정된
        // 묶음이라 개별조합처럼 부족한 만큼 더 비싼 대안으로 갈아탈 수가 없으니, 하한선(예산-500만원)도
        // 걸어서 너무 싼 건 후보에서 뺀다. 그 결과 남는 패키지가 없으면 recommend()에서 자동으로
        // 개별조합 경로(예산 근접도 보정 있음, fillTowardBudget)로 넘어간다.
        BigDecimal lowerLimit = BigDecimal.valueOf(Math.max(budget - BUDGET_TOLERANCE, 0));

        return pool.stream()
                .filter(p -> p.getPackagePrice() != null
                        && p.getPackagePrice().compareTo(upperLimit) <= 0
                        && p.getPackagePrice().compareTo(lowerLimit) >= 0)
                .toList();
    }

    // 홀 하나가 하객수를 수용 가능한지 - maxCapacity가 아직 안 채워진(null) 홀은 "수용 가능"으로 간주
    private boolean fitsGuestCount(Company hallCompany, Integer guestCount) {
        if (guestCount == null || hallCompany == null) {
            return true;
        }
        return hallDetailRepository.findByCompany_Cmno(hallCompany.getCmno())
                .map(hd -> hd.getMaxCapacity() == null || hd.getMaxCapacity() >= guestCount)
                .orElse(true);
    }

    private double packageScore(CompanyPackage p, Long budget) {

        double distanceScore = p.getDistanceKm() != null ? p.getDistanceKm() : 999.0;

        if (budget == null || budget <= 0) {
            return distanceScore;
        }

        BigDecimal price = p.getPackagePrice() != null ? p.getPackagePrice() : BigDecimal.ZERO;
        double budgetDiffRatio = price.subtract(BigDecimal.valueOf(budget)).abs().doubleValue() / budget;

        return budgetDiffRatio * BUDGET_WEIGHT + (distanceScore / 10.0) * DISTANCE_WEIGHT;
    }

    private AiPlanPackageCandidateDTO toPackageCandidateDTO(CompanyPackage p, Long budget) {

        DressItem dressItem = resolveDressItem(p.getDressCompany(), List.of());
        HallItem hallItem = resolveHallItem(p.getHallCompany());

        return AiPlanPackageCandidateDTO.builder()
                .pno(p.getPno())
                .name(p.getName())
                .description(p.getDescription())
                .packagePrice(p.getPackagePrice())
                .distanceKm(p.getDistanceKm())
                .hallCmno(p.getHallCompany().getCmno())
                .hallName(p.getHallCompany().getName())
                .hallRoomName(hallItem != null ? hallItem.getItemName() : null)
                .hallImageUrl(hallItem != null ? hallItem.getImageUrl() : firstImage(p.getHallCompany()))
                .hallPrice(hallItem != null ? hallItem.getPrice() : p.getHallCompany().getPriceAvg())
                .dressCmno(p.getDressCompany().getCmno())
                .dressName(p.getDressCompany().getName())
                .dressItemId(dressItem != null ? dressItem.getDressItemId() : null)
                .dressOptionName(dressItem != null ? dressItem.getItemName() : null)
                .dressImageUrl(dressItem != null ? dressItem.getImageUrl() : null)
                .dressPrice(dressItem != null ? dressItem.getPrice() : p.getDressCompany().getPriceAvg())
                .studioCmno(p.getStudioCompany().getCmno())
                .studioName(p.getStudioCompany().getName())
                .studioImageUrl(firstImage(p.getStudioCompany()))
                .studioPrice(p.getStudioCompany().getPriceAvg())
                .makeupCmno(p.getMakeupCompany().getCmno())
                .makeupName(p.getMakeupCompany().getName())
                .makeupImageUrl(firstImage(p.getMakeupCompany()))
                .makeupPrice(p.getMakeupCompany().getPriceAvg())
                .reason(buildPackageReason(p, budget))
                .sourceType("PACKAGE")
                .build();
    }

    private String buildPackageReason(CompanyPackage p, Long budget) {

        StringBuilder sb = new StringBuilder();

        if (p.getPackagePrice() != null) {
            boolean withinBudget = budget != null && p.getPackagePrice().compareTo(BigDecimal.valueOf(budget)) <= 0;
            sb.append(withinBudget ? "예산 내 " : "예산과 근접한 ")
                    .append(String.format("%,d원", p.getPackagePrice().longValue()));
        }

        if (p.getDistanceKm() != null) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(String.format("%.1fkm", p.getDistanceKm()));
        }

        return sb.toString();
    }

    // ── 개별 조합 경로 (패키지 대체 / 취향 반영) ──────────────────────────

    private ComboResult buildIndividualCombo(String region, Long budget, Integer guestCount,
                                             AiPlanCategoryPreferences prefs) {

        Pick hall = pickBestCompany(CompanyCategory.HALL, region, allocate(budget, HALL_RATIO), guestCount, prefs);
        Pick dress = pickBestCompany(CompanyCategory.DRESS, region, allocate(budget, DRESS_RATIO), guestCount, prefs);
        Pick studio = pickBestCompany(CompanyCategory.STUDIO, region, allocate(budget, STUDIO_RATIO), guestCount, prefs);
        Pick makeup = pickBestCompany(CompanyCategory.MAKEUP, region, allocate(budget, MAKEUP_RATIO), guestCount, prefs);

        if (hall == null || dress == null || studio == null || makeup == null) {
            return null;
        }

        if (budget != null && budget > 0) {
            Map<CompanyCategory, Pick> picks = new EnumMap<>(CompanyCategory.class);
            picks.put(CompanyCategory.HALL, hall);
            picks.put(CompanyCategory.DRESS, dress);
            picks.put(CompanyCategory.STUDIO, studio);
            picks.put(CompanyCategory.MAKEUP, makeup);

            fillTowardBudget(picks, region, budget, prefs);

            hall = picks.get(CompanyCategory.HALL);
            dress = picks.get(CompanyCategory.DRESS);
            studio = picks.get(CompanyCategory.STUDIO);
            makeup = picks.get(CompanyCategory.MAKEUP);
        }

        boolean usedRegionFallback = hall.usedRegionFallback || dress.usedRegionFallback
                || studio.usedRegionFallback || makeup.usedRegionFallback;
        boolean usedStyleFallback = hall.usedStyleFallback || dress.usedStyleFallback
                || studio.usedStyleFallback || makeup.usedStyleFallback;

        DressItem dressItem = resolveDressItem(dress.company, prefs.getDressKeywords());
        HallItem hallItem = resolveHallItem(hall.company);

        // 요청한 패키지 타입과 정확히 일치하는 업체를 찾았을 때만(취향 폴백이 아닐 때만) 채운다 -
        // 폴백으로 고른 업체는 그 패키지를 실제로 안 가지고 있을 수 있어서.
        String makeupPackageType = (prefs.getMakeupType() != null && !makeup.usedStyleFallback)
                ? prefs.getMakeupType().name()
                : null;

        // 홀/드레스는 구체적인 옵션(아이템) 가격이 있으면 그 값을, 없으면 업체 평균가로 - 합계도
        // 카드에 표시하는 카테고리별 가격(hallPrice/dressPrice)과 실제로 더한 값이 맞도록 맞춘다.
        BigDecimal hallAmount = hallItem != null ? hallItem.getPrice() : hall.company.getPriceAvg();
        BigDecimal dressAmount = dressItem != null ? dressItem.getPrice() : dress.company.getPriceAvg();
        BigDecimal totalPrice = Stream.of(hallAmount, dressAmount, studio.company.getPriceAvg(),
                        makeup.company.getPriceAvg())
                .map(v -> v != null ? v : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AiPlanPackageCandidateDTO dto = AiPlanPackageCandidateDTO.builder()
                .pno(null)
                .name(prefs.isEmpty() ? "개별 조합 추천" : "취향 맞춤 개별 조합")
                .description("패키지 대신 홀/드레스/스튜디오/메이크업을 각각 조건에 맞게 골라 조합했어요.")
                .packagePrice(totalPrice)
                .distanceKm(null)
                .hallCmno(hall.company.getCmno())
                .hallName(hall.company.getName())
                .hallRoomName(hallItem != null ? hallItem.getItemName() : null)
                .hallImageUrl(hallItem != null ? hallItem.getImageUrl() : firstImage(hall.company))
                .hallPrice(hallItem != null ? hallItem.getPrice() : hall.company.getPriceAvg())
                .dressCmno(dress.company.getCmno())
                .dressName(dress.company.getName())
                .dressItemId(dressItem != null ? dressItem.getDressItemId() : null)
                .dressOptionName(dressItem != null ? dressItem.getItemName() : null)
                .dressImageUrl(dressItem != null ? dressItem.getImageUrl() : null)
                .dressPrice(dressItem != null ? dressItem.getPrice() : dress.company.getPriceAvg())
                .studioCmno(studio.company.getCmno())
                .studioName(studio.company.getName())
                .studioImageUrl(firstImage(studio.company))
                .studioPrice(studio.company.getPriceAvg())
                .makeupCmno(makeup.company.getCmno())
                .makeupName(makeup.company.getName())
                .makeupImageUrl(firstImage(makeup.company))
                .makeupPrice(makeup.company.getPriceAvg())
                .makeupPackageType(makeupPackageType)
                .reason(buildComboReason(totalPrice, budget))
                .sourceType("INDIVIDUAL_COMBO")
                .build();

        ComboResult result = new ComboResult();
        result.dto = dto;
        result.usedRegionFallback = usedRegionFallback;
        result.usedStyleFallback = usedStyleFallback;
        return result;
    }

    private String buildComboReason(BigDecimal totalPrice, Long budget) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("합계 %,d원", totalPrice.longValue()));

        if (budget != null && budget > 0) {
            boolean withinBudget = totalPrice.compareTo(BigDecimal.valueOf(budget)) <= 0;
            sb.append(withinBudget ? " (예산 내)" : " (예산 초과)");
        }

        return sb.toString();
    }

    private Long allocate(Long budget, double ratio) {
        return budget != null && budget > 0 ? Math.round(budget * ratio) : null;
    }

    // 카테고리별 예산을 ratio로 미리 쪼개서 고르면, 더미데이터가 부실할 때 카테고리 각각은
    // "그 캡 안에서 최선"이어도 합계가 전체 예산보다 한참 못 미칠 수 있다. 오차가 톨러런스보다
    // 크면 남은 여유 예산을 카테고리 순서대로 흘려보내서 더 비싼 대안이 있으면 갈아탄다.
    // 취향(prefs)이 실제로 매칭된 카테고리는 예산 때문에 임의로 바꾸지 않는다.
    // (하객수 필터는 여기선 다시 안 걺 - 홀을 갈아탈 때도 이미 처음에 걸러진 풀 안에서만 고르도록
    // 하려면 searchOne을 홀 전용으로 다시 짜야 하는데, 예산 여유 재분배는 이미 수용 가능한 홀들 중
    // 더 비싼 곳으로 갈아타는 것뿐이라 사실상 문제되는 경우가 드묾)
    private void fillTowardBudget(Map<CompanyCategory, Pick> picks, String region, Long budget,
                                  AiPlanCategoryPreferences prefs) {

        long total = picks.values().stream()
                .mapToLong(p -> p.company.getPriceAvg() != null ? p.company.getPriceAvg().longValue() : 0L)
                .sum();
        long leftover = budget - total;

        for (CompanyCategory category : UPGRADE_ORDER) {
            if (leftover <= BUDGET_TOLERANCE) {
                break;
            }

            Pick current = picks.get(category);
            if (hasPreference(category, prefs) && !current.usedStyleFallback) {
                continue; // 취향에 맞춰 고른 곳은 예산 때문에 임의로 안 바꿈
            }

            BigDecimal currentPrice = current.company.getPriceAvg() != null
                    ? current.company.getPriceAvg() : BigDecimal.ZERO;
            BigDecimal newCap = currentPrice.add(BigDecimal.valueOf(leftover));
            String searchRegion = current.usedRegionFallback ? null : region;

            Company upgraded = searchOne(category, searchRegion,
                    currentPrice.add(BigDecimal.ONE), newCap, Sort.by("priceAvg").descending());

            if (upgraded != null && upgraded.getPriceAvg() != null
                    && upgraded.getPriceAvg().compareTo(currentPrice) > 0) {
                long gained = upgraded.getPriceAvg().longValue() - currentPrice.longValue();
                picks.put(category, new Pick(upgraded, current.dressItemId, current.usedRegionFallback,
                        current.usedStyleFallback));
                leftover -= gained;
            }
        }
    }

    private boolean hasPreference(CompanyCategory category, AiPlanCategoryPreferences prefs) {
        return switch (category) {
            case HALL -> !prefs.getHallTypes().isEmpty();
            case STUDIO -> !prefs.getStudioKeywords().isEmpty();
            case DRESS -> !prefs.getDressKeywords().isEmpty();
            case MAKEUP -> prefs.getMakeupType() != null;
        };
    }

    // 홀/스튜디오/메이크업 대표 이미지 - 업체 대표 이미지 첫 장 (Company.imageList ord=0).
    static String firstImage(Company company) {
        if (company == null || company.getImageList() == null || company.getImageList().isEmpty()) {
            return null;
        }
        return company.getImageList().get(0).getFileName();
    }

    // 5단계 - 드레스 "옵션(아이템)" 하나를 결정. 업체를 고르는 로직(예산/지역 매칭)은 그대로 두고,
    // 이건 "그 업체 안에서 어떤 구체적인 아이템을 보여줄지"만 정한다. 스타일 키워드가 있으면 그
    // 태그를 가진 첫 아이템, 없으면(또는 안 맞으면) ord가 가장 앞선 대표 아이템.
    // 세션에 별도로 저장하지 않고 (company, styleKeyword)가 같으면 항상 같은 결과가 나오도록
    // 결정적으로 다시 계산한다 - 리파인/되돌리기/새로고침 복원 어디서 불러도 결과가 흔들리지 않음.
    // 홀도 드레스처럼 업체 안에 구체적인 대관 옵션(HallItem, 예: "플로렌스홀")이 여러 개 있을 수 있다.
    // 업체 선택 로직(예산/지역/하객수 매칭)은 그대로 Company/HallDetail 단위로 하고, 이건 그 업체의
    // 대표 대관 옵션 하나(ord가 가장 앞선 것)를 표시용으로 꺼내 쓴다 - dressOptionImage와 같은 패턴.
    HallItem resolveHallItem(Company hallCompany) {
        if (hallCompany == null) {
            return null;
        }
        return hallItemRepository.findByCompany_CmnoOrderByOrdAsc(hallCompany.getCmno()).stream()
                .findFirst()
                .orElse(null);
    }

    // 칩 중복선택을 허용해서 styleKeywords가 여러 개일 수 있음 - 전부 가진(AND) 아이템을 우선 찾고,
    // 없으면 대표 아이템(ord 0)으로 폴백.
    DressItem resolveDressItem(Company dressCompany, List<String> styleKeywords) {
        if (dressCompany == null) {
            return null;
        }
        List<DressItem> items = dressItemRepository.findByCompany_CmnoOrderByOrdAsc(dressCompany.getCmno());
        if (items.isEmpty()) {
            return null;
        }
        List<String> keywords = styleKeywords == null ? List.of() : styleKeywords;
        if (!keywords.isEmpty()) {
            return items.stream()
                    .filter(i -> i.getStyleTags() != null
                            && keywords.stream().allMatch(k -> i.getStyleTags().contains(k)))
                    .findFirst()
                    .orElse(items.get(0));
        }
        return items.get(0);
    }

    // 6단계 리파인 대화/사이드패널에서 카테고리 하나만 다시 찾을 때 씀.
    // Pick은 private 중첩 클래스라 바깥(AiPlanRefineServiceImpl)에 그대로 못 넘기니, 필요한 값만
    // PickResult 레코드로 옮겨 담아서 반환한다.
    PickResult pickOne(CompanyCategory category, String region, Long budgetForCategory, AiPlanCategoryPreferences prefs) {
        Pick pick = pickBestCompany(category, region, budgetForCategory, null, prefs);
        return pick == null ? null : new PickResult(pick.company, pick.dressItemId);
    }

    // pickOne()의 반환 타입 - 업체 cmno와(드레스일 때만) 골라진 구체적 아이템 id
    record PickResult(Company company, Long dressItemId) {
    }

    // 카테고리별로 취향이 있으면 취향 우선 탐색, 없으면 바로 기본(지역/예산) 탐색으로.
    // 드레스는 업체가 아니라 아이템 단위로 골라야 해서 처음부터 별도 경로(pickDress)로 분리.
    private Pick pickBestCompany(CompanyCategory category, String region, Long allocatedBudget,
                                 Integer guestCount, AiPlanCategoryPreferences prefs) {

        if (category == CompanyCategory.DRESS) {
            return pickDress(region, allocatedBudget, prefs.getDressKeywords());
        }

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;
        boolean hasPreference = hasPreference(category, prefs);

        if (hasPreference) {
            Company styled = findStyled(category, region, maxPrice, guestCount, prefs);
            if (styled != null) {
                return new Pick(styled, false, false);
            }

            if (region != null) {
                Company styledNoRegion = findStyled(category, null, maxPrice, guestCount, prefs);
                if (styledNoRegion != null) {
                    return new Pick(styledNoRegion, true, false);
                }
            }
        }

        Pick plain = pickBestCompanyPlain(category, region, allocatedBudget, guestCount);

        if (plain == null) {
            return null;
        }

        return hasPreference
                ? new Pick(plain.company, plain.usedRegionFallback, true)
                : plain;
    }

    // 드레스 전용 선택 경로 - 스타일 취향(칩 중복선택 가능)이 있으면 그 태그를 전부(AND) 가진 아이템 중
    // 최선, 없으면(또는 매칭 실패시) 예산/지역만으로 아이템을 고른다. 어느 경로든 "업체"가 아니라
    // 구체적인 아이템을 반환. DB 쿼리는 첫 키워드로만 걸고(searchByStyleKeyword가 단일 키워드 전용이라),
    // 키워드가 2개 이상이면 나머지는 Java에서 추가로 AND 필터링한다.
    private Pick pickDress(String region, Long allocatedBudget, List<String> styleKeywords) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;

        if (!styleKeywords.isEmpty()) {
            String primaryKeyword = styleKeywords.get(0);

            List<DressItem> found = filterByAllStyleKeywords(
                    dressItemRepository.searchByStyleKeyword(primaryKeyword, region, maxPrice), styleKeywords);
            if (!found.isEmpty()) {
                DressItem item = found.get(0);
                return new Pick(item.getCompany(), item.getDressItemId(), false, false);
            }
            List<DressItem> foundNoRegion = filterByAllStyleKeywords(
                    dressItemRepository.searchByStyleKeyword(primaryKeyword, null, maxPrice), styleKeywords);
            if (!foundNoRegion.isEmpty()) {
                DressItem item = foundNoRegion.get(0);
                return new Pick(item.getCompany(), item.getDressItemId(), true, false);
            }
        }

        Pick plain = pickDressPlain(region, allocatedBudget);
        if (plain == null) {
            return null;
        }

        return !styleKeywords.isEmpty()
                ? new Pick(plain.company, plain.dressItemId, plain.usedRegionFallback, true)
                : plain;
    }

    // 키워드가 하나뿐이면 이미 SQL에서 걸러졌으니 그대로 두고, 여러 개면 나머지 키워드까지 전부
    // 포함하는지(AND) Java에서 추가로 걸러낸다.
    private List<DressItem> filterByAllStyleKeywords(List<DressItem> items, List<String> keywords) {
        if (keywords.size() <= 1) {
            return items;
        }
        return items.stream()
                .filter(i -> i.getStyleTags() != null
                        && keywords.stream().allMatch(k -> i.getStyleTags().contains(k)))
                .toList();
    }

    private Pick pickDressPlain(String region, Long allocatedBudget) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;

        List<DressItem> found = dressItemRepository.searchByBudget(region, maxPrice, Sort.by("price").descending());
        if (!found.isEmpty()) {
            DressItem item = found.get(0);
            return new Pick(item.getCompany(), item.getDressItemId(), false, false);
        }

        if (region != null) {
            found = dressItemRepository.searchByBudget(null, maxPrice, Sort.by("price").descending());
            if (!found.isEmpty()) {
                DressItem item = found.get(0);
                return new Pick(item.getCompany(), item.getDressItemId(), true, false);
            }
        }

        found = dressItemRepository.searchByBudget(null, null, Sort.by("price").ascending());
        if (!found.isEmpty()) {
            DressItem item = found.get(0);
            return new Pick(item.getCompany(), item.getDressItemId(), region != null, false);
        }

        return null;
    }

    private Company findStyled(CompanyCategory category, String region, BigDecimal maxPrice,
                               Integer guestCount, AiPlanCategoryPreferences prefs) {

        return switch (category) {
            case HALL -> findStyledHall(prefs.getHallTypes(), region, maxPrice, guestCount);
            case STUDIO -> findStyledStudio(prefs.getStudioKeywords(), region, maxPrice);
            case DRESS -> throw new IllegalStateException("DRESS는 pickDress()에서 별도 처리됨");
            case MAKEUP -> findStyledMakeup(region, maxPrice, prefs.getMakeupType());
        };
    }

    // 홀은 업체 하나에 타입이 하나뿐이라 칩 중복선택시 "그 중 하나라도 맞으면"(OR)으로 찾는다.
    // 타입별로 따로 조회해서 합친 뒤, 예산에 가장 근접한(비싼) 것 하나를 고른다.
    private Company findStyledHall(List<HallType> hallTypes, String region, BigDecimal maxPrice,
                                   Integer guestCount) {
        Company best = null;
        BigDecimal bestPrice = null;
        for (HallType type : hallTypes) {
            List<HallDetail> found = hallDetailRepository.searchByType(type, region, maxPrice, guestCount);
            if (found.isEmpty()) {
                continue;
            }
            Company candidate = found.get(0).getCompany();
            BigDecimal candidatePrice = candidate.getPriceAvg();
            if (best == null || (candidatePrice != null && (bestPrice == null || candidatePrice.compareTo(bestPrice) > 0))) {
                best = candidate;
                bestPrice = candidatePrice;
            }
        }
        return best;
    }

    // 스튜디오는 태그 여러 개가 한 업체에 같이 붙어있는 값이라 칩 중복선택시 "전부 가진"(AND)으로 찾는다.
    // DB 쿼리는 첫 키워드로만 걸고, 나머지는 Java에서 추가로 AND 필터링한다 (드레스와 동일한 패턴).
    private Company findStyledStudio(List<String> keywords, String region, BigDecimal maxPrice) {
        if (keywords.isEmpty()) {
            return null;
        }
        List<StudioDetail> found = studioDetailRepository.searchByThemeKeyword(keywords.get(0), region, maxPrice);
        return found.stream()
                .filter(sd -> sd.getThemeTags() != null
                        && keywords.stream().allMatch(k -> sd.getThemeTags().contains(k)))
                .findFirst()
                .map(StudioDetail::getCompany)
                .orElse(null);
    }

    // 메이크업 패키지 타입(MakeupPackageType)을 보유한 업체 중, 예산/지역 조건에 맞는 곳 하나.
    // MakeupPackageRepository로 먼저 타입이 맞는 업체 cmno들을 구하고, 그 안에서 예산/지역 순으로
    // 고른다 (HALL/STUDIO/DRESS처럼 조건 하나로 바로 쿼리하지 않는 이유는, "패키지 타입 보유 여부"가
    // Company가 아니라 그 하위 MakeupPackage 엔티티에 있는 값이라 조인 쿼리보다 이 편이 간단해서).
    private Company findStyledMakeup(String region, BigDecimal maxPrice, MakeupPackageType type) {
        if (type == null) {
            return null;
        }
        List<Long> matchedCmnos = makeupPackageRepository.findCompanyCmnosByPackageTypeIn(List.of(type));
        if (matchedCmnos.isEmpty()) {
            return null;
        }
        Set<Long> matchedSet = new HashSet<>(matchedCmnos);
        List<Company> pool = companyRepository
                .searchList(CompanyCategory.MAKEUP, region, null, maxPrice,
                        PageRequest.of(0, MAKEUP_POOL_SIZE, Sort.by("priceAvg").descending()))
                .getContent();
        return pool.stream().filter(c -> matchedSet.contains(c.getCmno())).findFirst().orElse(null);
    }

    private Pick pickBestCompanyPlain(CompanyCategory category, String region, Long allocatedBudget,
                                      Integer guestCount) {

        if (category == CompanyCategory.HALL) {
            return pickHallPlain(region, allocatedBudget, guestCount);
        }

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;

        Company found = searchOne(category, region, null, maxPrice, Sort.by("priceAvg").descending());
        if (found != null) {
            return new Pick(found, false, false);
        }

        if (region != null) {
            found = searchOne(category, null, null, maxPrice, Sort.by("priceAvg").descending());
            if (found != null) {
                return new Pick(found, true, false);
            }
        }

        found = searchOne(category, null, null, null, Sort.by("priceAvg").ascending());
        if (found != null) {
            return new Pick(found, region != null, false);
        }

        return null;
    }

    // 홀 전용 plain 경로 - 하객수 필터를 걸어야 해서 CompanyRepository 대신 HallDetailRepository로 검색.
    private Pick pickHallPlain(String region, Long allocatedBudget, Integer guestCount) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;

        List<HallDetail> found = hallDetailRepository
                .searchByCapacity(region, maxPrice, guestCount, Sort.by("company.priceAvg").descending());
        if (!found.isEmpty()) {
            return new Pick(found.get(0).getCompany(), false, false);
        }

        if (region != null) {
            found = hallDetailRepository
                    .searchByCapacity(null, maxPrice, guestCount, Sort.by("company.priceAvg").descending());
            if (!found.isEmpty()) {
                return new Pick(found.get(0).getCompany(), true, false);
            }
        }

        found = hallDetailRepository.searchByCapacity(null, null, guestCount, Sort.by("company.priceAvg").ascending());
        if (!found.isEmpty()) {
            return new Pick(found.get(0).getCompany(), region != null, false);
        }

        return null;
    }

    private Company searchOne(CompanyCategory category, String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                              Sort sort) {

        List<Company> content = companyRepository
                .searchList(category, keyword, minPrice, maxPrice, PageRequest.of(0, 1, sort))
                .getContent();

        return content.isEmpty() ? null : content.get(0);
    }

    // AiPlanAiServiceImpl(AI 모드)의 예산 근접도 보정에서 재사용 - 가격 구간 안에서 가장 비싼 후보 하나.
    // (드레스도 여기선 업체 단위로 갈아타고, 최종 표시 아이템은 resolveDressItem이 그 업체 기준으로 다시 정함)
    Company findMostExpensiveWithin(CompanyCategory category, String region, BigDecimal minPrice, BigDecimal maxPrice) {
        return searchOne(category, region, minPrice, maxPrice, Sort.by("priceAvg").descending());
    }

    private static class Pick {
        private final Company company;
        private final Long dressItemId;
        private final boolean usedRegionFallback;
        private final boolean usedStyleFallback;

        private Pick(Company company, boolean usedRegionFallback, boolean usedStyleFallback) {
            this(company, null, usedRegionFallback, usedStyleFallback);
        }

        private Pick(Company company, Long dressItemId, boolean usedRegionFallback, boolean usedStyleFallback) {
            this.company = company;
            this.dressItemId = dressItemId;
            this.usedRegionFallback = usedRegionFallback;
            this.usedStyleFallback = usedStyleFallback;
        }
    }

    private static class ComboResult {
        private AiPlanPackageCandidateDTO dto;
        private boolean usedRegionFallback;
        private boolean usedStyleFallback;
    }
}
