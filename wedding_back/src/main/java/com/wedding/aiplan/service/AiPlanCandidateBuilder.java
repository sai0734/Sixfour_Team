package com.wedding.aiplan.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.wedding.company.domain.StudioDetail;
import com.wedding.company.repository.CompanyPackageRepository;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.company.repository.HallDetailRepository;
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
    private final StudioDetailRepository studioDetailRepository;
    private final DressItemRepository dressItemRepository;

    private static final int MAX_CANDIDATES = 5;
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

    public AiPlanQuickResultDTO recommend(String region, Long budget, AiPlanCategoryPreferences prefs) {

        if (prefs.isEmpty()) {
            List<CompanyPackage> fittingPackages = findFittingPackages(region, budget);

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

        ComboResult combo = buildIndividualCombo(region, budget, prefs);

        if (combo == null) {
            return AiPlanQuickResultDTO.builder()
                    .candidates(List.of())
                    .regionRelaxed(false)
                    .message("추천할 수 있는 업체가 아직 없어요.")
                    .build();
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

        return AiPlanQuickResultDTO.builder()
                .candidates(List.of(combo.dto))
                .regionRelaxed(combo.usedRegionFallback)
                .message(message.toString())
                .build();
    }

    // ── 패키지 경로 (빠르게 모드, 취향 없을 때만) ──────────────────────────

    private List<CompanyPackage> findFittingPackages(String region, Long budget) {

        List<CompanyPackage> pool = region != null
                ? companyPackageRepository.findAllActiveByHallRegion(region)
                : companyPackageRepository.findAllActive();

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

        return AiPlanPackageCandidateDTO.builder()
                .pno(p.getPno())
                .name(p.getName())
                .description(p.getDescription())
                .packagePrice(p.getPackagePrice())
                .distanceKm(p.getDistanceKm())
                .hallCmno(p.getHallCompany().getCmno())
                .hallName(p.getHallCompany().getName())
                .hallImageUrl(firstImage(p.getHallCompany()))
                .dressCmno(p.getDressCompany().getCmno())
                .dressName(p.getDressCompany().getName())
                .dressImageUrl(dressOptionImage(p.getDressCompany()))
                .studioCmno(p.getStudioCompany().getCmno())
                .studioName(p.getStudioCompany().getName())
                .studioImageUrl(firstImage(p.getStudioCompany()))
                .makeupCmno(p.getMakeupCompany().getCmno())
                .makeupName(p.getMakeupCompany().getName())
                .makeupImageUrl(firstImage(p.getMakeupCompany()))
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

    private ComboResult buildIndividualCombo(String region, Long budget, AiPlanCategoryPreferences prefs) {

        Pick hall = pickBestCompany(CompanyCategory.HALL, region, allocate(budget, HALL_RATIO), prefs);
        Pick dress = pickBestCompany(CompanyCategory.DRESS, region, allocate(budget, DRESS_RATIO), prefs);
        Pick studio = pickBestCompany(CompanyCategory.STUDIO, region, allocate(budget, STUDIO_RATIO), prefs);
        Pick makeup = pickBestCompany(CompanyCategory.MAKEUP, region, allocate(budget, MAKEUP_RATIO), prefs);

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

        BigDecimal totalPrice = List.of(hall, dress, studio, makeup).stream()
                .map(pick -> pick.company.getPriceAvg() != null ? pick.company.getPriceAvg() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AiPlanPackageCandidateDTO dto = AiPlanPackageCandidateDTO.builder()
                .pno(null)
                .name(prefs.isEmpty() ? "개별 조합 추천" : "취향 맞춤 개별 조합")
                .description("패키지 대신 홀/드레스/스튜디오/메이크업을 각각 조건에 맞게 골라 조합했어요.")
                .packagePrice(totalPrice)
                .distanceKm(null)
                .hallCmno(hall.company.getCmno())
                .hallName(hall.company.getName())
                .hallImageUrl(firstImage(hall.company))
                .dressCmno(dress.company.getCmno())
                .dressName(dress.company.getName())
                .dressImageUrl(dressOptionImage(dress.company))
                .studioCmno(studio.company.getCmno())
                .studioName(studio.company.getName())
                .studioImageUrl(firstImage(studio.company))
                .makeupCmno(makeup.company.getCmno())
                .makeupName(makeup.company.getName())
                .makeupImageUrl(firstImage(makeup.company))
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
                picks.put(category, new Pick(upgraded, current.usedRegionFallback, current.usedStyleFallback));
                leftover -= gained;
            }
        }
    }

    private boolean hasPreference(CompanyCategory category, AiPlanCategoryPreferences prefs) {
        return switch (category) {
            case HALL -> prefs.getHallType() != null;
            case STUDIO -> prefs.getStudioKeyword() != null;
            case DRESS -> prefs.getDressKeyword() != null;
            case MAKEUP -> prefs.getMakeupKeyword() != null;
        };
    }

    // 홀/스튜디오/메이크업 대표 이미지 - 업체 대표 이미지 첫 장 (Company.imageList ord=0).
    static String firstImage(Company company) {
        if (company == null || company.getImageList() == null || company.getImageList().isEmpty()) {
            return null;
        }
        return company.getImageList().get(0).getFileName();
    }

    // 드레스는 업체 대표 이미지가 아니라 "옵션(드레스 아이템) 이미지" - 그 업체 드레스 아이템 중 ord가 가장 앞선 것.
    String dressOptionImage(Company dressCompany) {
        if (dressCompany == null) {
            return null;
        }
        return dressItemRepository.findByCompany_CmnoOrderByOrdAsc(dressCompany.getCmno()).stream()
                .findFirst()
                .map(DressItem::getImageUrl)
                .orElse(null);
    }

    // 6단계 리파인 대화에서 카테고리 하나만 다시 찾을 때 씀 - 폴백 플래그는 필요 없어서 Company만 반환.
    Company pickOne(CompanyCategory category, String region, Long budgetForCategory, AiPlanCategoryPreferences prefs) {
        Pick pick = pickBestCompany(category, region, budgetForCategory, prefs);
        return pick == null ? null : pick.company;
    }

    // 카테고리별로 취향이 있으면 취향 우선 탐색, 없으면 바로 기본(지역/예산) 탐색으로
    private Pick pickBestCompany(CompanyCategory category, String region, Long allocatedBudget,
                                 AiPlanCategoryPreferences prefs) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;

        boolean hasPreference = hasPreference(category, prefs);

        if (hasPreference) {
            Company styled = findStyled(category, region, maxPrice, prefs);
            if (styled != null) {
                return new Pick(styled, false, false);
            }

            if (region != null) {
                Company styledNoRegion = findStyled(category, null, maxPrice, prefs);
                if (styledNoRegion != null) {
                    return new Pick(styledNoRegion, true, false);
                }
            }
        }

        Pick plain = pickBestCompanyPlain(category, region, allocatedBudget);

        if (plain == null) {
            return null;
        }

        return hasPreference
                ? new Pick(plain.company, plain.usedRegionFallback, true)
                : plain;
    }

    private Company findStyled(CompanyCategory category, String region, BigDecimal maxPrice,
                               AiPlanCategoryPreferences prefs) {

        return switch (category) {
            case HALL -> {
                List<HallDetail> found = hallDetailRepository
                        .searchByType(prefs.getHallType(), region, maxPrice);
                yield found.isEmpty() ? null : found.get(0).getCompany();
            }
            case STUDIO -> {
                List<StudioDetail> found = studioDetailRepository
                        .searchByThemeKeyword(prefs.getStudioKeyword(), region, maxPrice);
                yield found.isEmpty() ? null : found.get(0).getCompany();
            }
            case DRESS -> {
                List<DressItem> found = dressItemRepository
                        .searchByStyleKeyword(prefs.getDressKeyword(), region, maxPrice);
                yield findFirstDistinctCompany(found);
            }
            case MAKEUP -> {
                List<Company> found = companyRepository.searchByCategoryDescriptionKeyword(
                        CompanyCategory.MAKEUP, prefs.getMakeupKeyword(), region, maxPrice);
                yield found.isEmpty() ? null : found.get(0);
            }
        };
    }

    private Pick pickBestCompanyPlain(CompanyCategory category, String region, Long allocatedBudget) {

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

    private Company searchOne(CompanyCategory category, String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                              Sort sort) {

        List<Company> content = companyRepository
                .searchList(category, keyword, minPrice, maxPrice, PageRequest.of(0, 1, sort))
                .getContent();

        return content.isEmpty() ? null : content.get(0);
    }

    // AiPlanAiServiceImpl(AI 모드)의 예산 근접도 보정에서 재사용 - 가격 구간 안에서 가장 비싼 후보 하나.
    Company findMostExpensiveWithin(CompanyCategory category, String region, BigDecimal minPrice, BigDecimal maxPrice) {
        return searchOne(category, region, minPrice, maxPrice, Sort.by("priceAvg").descending());
    }

    private Company findFirstDistinctCompany(List<DressItem> items) {

        Map<Long, Company> byCmno = new LinkedHashMap<>();
        for (DressItem item : items) {
            Company c = item.getCompany();
            byCmno.putIfAbsent(c.getCmno(), c);
        }
        return byCmno.isEmpty() ? null : byCmno.values().iterator().next();
    }

    private static class Pick {
        private final Company company;
        private final boolean usedRegionFallback;
        private final boolean usedStyleFallback;

        private Pick(Company company, boolean usedRegionFallback, boolean usedStyleFallback) {
            this.company = company;
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
