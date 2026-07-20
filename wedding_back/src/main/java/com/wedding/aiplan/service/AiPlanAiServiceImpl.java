package com.wedding.aiplan.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.aiplan.dto.AiPlanPackageCandidateDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.HallDetailRepository;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 문서 4번 매칭 파이프라인 그대로: (1) SQL로 후보 좁히기 (2) AI 호출 - 그 목록 안에서만 고르고
// JSON으로 응답 강제 (3) 서버 재검증 - AI가 준 cmno가 실제 후보 목록에 있는지 확인, 없으면 버림.
// AI 호출/파싱/그라운딩 중 어느 단계든 실패하면 AiPlanCandidateBuilder(4단계 로직)로 조용히 폴백한다 -
// AI가 죽어도 추천 자체는 항상 되게 하기 위함. (fallback 경로도 6단계 세션 생성 대상에 포함)
@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class AiPlanAiServiceImpl implements AiPlanAiService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final HallDetailRepository hallDetailRepository;
    private final AiPlanCandidateBuilder candidateBuilder;
    private final AiPlanSessionSupport sessionSupport;

    private static final int POOL_SIZE = 6;

    // 자유 요청사항에 이 단어들 중 하나와 업체명이 같은 구절(마침표/쉼표/줄바꿈 단위)에 같이 나오면
    // 그 업체는 "제외 요청"으로 보고 AI에게 보여주는 후보 목록에서 아예 뺀다. AI에게 "빼달라"고
    // 부탁만 하는 것보다 애초에 선택지에서 지우는 쪽이 그라운딩 관점에서 더 확실하다.
    private static final List<String> EXCLUDE_CUES = List.of(
            "빼고", "빼줘", "빼주세요", "빼줄래", "제외", "말고", "말고요", "아니고", "넣지 말", "안 넣");

    private static final String SYSTEM_PROMPT =
            "당신은 웨딩 준비 추천 엔진입니다. 아래 각 카테고리(HALL/STUDIO/DRESS/MAKEUP)마다 제공되는 후보 "
                    + "목록 안에서만 골라야 합니다. 목록에 없는 cmno는 절대 사용하지 마세요.\n"
                    + "먼저 사용자의 자유 요청사항을 읽고, \"이미 정했다\", \"내가 알아서 한다\", \"필요 없다\", "
                    + "\"빼고 찾아줘\" 처럼 이번 탐색에서 그 카테고리를 아예 안 찾아도 되는 이유가 있으면 "
                    + "그 카테고리의 exclude를 true로 하고 cmno/reason은 생략하세요. 그런 이유가 없는 카테고리는 "
                    + "exclude를 false로 하고 후보 목록 안에서 정확히 하나를 골라 cmno와, 왜 그 업체를 골랐는지 "
                    + "한국어로 한 문장 이유를 채우세요.\n"
                    + "exclude가 아닌 카테고리인데 취향과 정확히 맞는 후보가 없으면, 그나마 가장 가까운 후보를 "
                    + "고르고 excludedNote에 그 사정을 설명하세요.\n"
                    + "반드시 아래 JSON 형식으로만 응답하세요. 다른 설명, 마크다운, 코드블록 없이 순수 JSON만 출력합니다:\n"
                    + "{\"hall\":{\"exclude\":false,\"cmno\":0,\"reason\":\"...\"},"
                    + "\"studio\":{\"exclude\":false,\"cmno\":0,\"reason\":\"...\"},"
                    + "\"dress\":{\"exclude\":false,\"cmno\":0,\"reason\":\"...\"},"
                    + "\"makeup\":{\"exclude\":false,\"cmno\":0,\"reason\":\"...\"},"
                    + "\"excludedNote\":\"...\"}";

    @Override
    public AiPlanQuickResultDTO getAiRecommendations(AiPlanDetailRequestDTO requestDTO) {

        String region = blankToNull(requestDTO.getRegion());
        Long budget = requestDTO.getBudget();

        Map<CompanyCategory, List<Company>> pools =
                buildPools(region, budget, requestDTO.getGuestCount(), requestDTO.getFreeText());

        if (pools.values().stream().anyMatch(List::isEmpty)) {
            log.warn("AiPlan AI: at least one category has no candidates at all, skipping AI call");
            return fallback(region, budget, requestDTO);
        }

        String raw;
        try {
            OpenAiResponseDTO response = openAiClient.getJsonChatCompletion(List.of(
                    OpenAiMessageDTO.of("system", SYSTEM_PROMPT),
                    OpenAiMessageDTO.of("user", buildPrompt(pools, requestDTO))));

            raw = response.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("AiPlan AI call failed, falling back to non-AI recommendation", e);
            return fallback(region, budget, requestDTO);
        }

        AiPlanPackageCandidateDTO combo = parseAndValidate(raw, pools, requestDTO.getDressStyle(), requestDTO.getMakeupStyle());

        if (combo == null) {
            log.warn("AiPlan AI: response failed parsing/grounding validation, falling back. raw={}", raw);
            return fallback(region, budget, requestDTO);
        }

        if (budget != null && budget > 0) {
            fillComboTowardBudget(combo, region, budget);
        }

        boolean anyExcluded = combo.getHallName() == null || combo.getStudioName() == null
                || combo.getDressName() == null || combo.getMakeupName() == null;
        StringBuilder message = new StringBuilder("AI가 취향과 자유 입력을 반영해서 골라줬어요.");
        if (anyExcluded) {
            message.append(" 요청하신 카테고리는 빼고 나머지만 담았어요.");
        }
        candidateBuilder.appendBudgetGapMessage(message, combo.getPackagePrice(), budget);

        return attachSession(AiPlanQuickResultDTO.builder()
                .candidates(List.of(combo))
                .regionRelaxed(false)
                .message(message.toString())
                .build(), budget, region, requestDTO.getWeddingDate(), "AI");
    }

    // 규칙 기반 경로(AiPlanCandidateBuilder)에 넣은 것과 같은 방식 - AI가 고른 조합도 합계가 예산보다
    // 500만원 넘게 모자라면, 카테고리 순서대로 남은 여유 예산 안에서 더 비싼 대안이 있으면 갈아탄다.
    // 사용자가 통째로 빼달라고 한 카테고리(cmno null)는 건드리지 않는다.
    private void fillComboTowardBudget(AiPlanPackageCandidateDTO combo, String region, Long budget) {

        long total = combo.getPackagePrice() != null ? combo.getPackagePrice().longValue() : 0L;
        long leftover = budget - total;

        for (CompanyCategory category : AiPlanCandidateBuilder.UPGRADE_ORDER) {
            if (leftover <= AiPlanCandidateBuilder.BUDGET_TOLERANCE) {
                break;
            }

            Long currentCmno = cmnoOf(combo, category);
            if (currentCmno == null) {
                continue; // 제외된 카테고리는 안 건드림
            }

            Company current = companyRepository.findById(currentCmno).orElse(null);
            if (current == null || current.getPriceAvg() == null) {
                continue;
            }

            BigDecimal newCap = current.getPriceAvg().add(BigDecimal.valueOf(leftover));
            Company upgraded = candidateBuilder.findMostExpensiveWithin(
                    category, region, current.getPriceAvg().add(BigDecimal.ONE), newCap);

            if (upgraded == null || upgraded.getPriceAvg() == null
                    || upgraded.getPriceAvg().compareTo(current.getPriceAvg()) <= 0) {
                continue;
            }

            long gained = upgraded.getPriceAvg().longValue() - current.getPriceAvg().longValue();
            applyUpgrade(combo, category, upgraded);
            leftover -= gained;
            total += gained;
        }

        combo.setPackagePrice(BigDecimal.valueOf(total));
    }

    private Long cmnoOf(AiPlanPackageCandidateDTO combo, CompanyCategory category) {
        return switch (category) {
            case HALL -> combo.getHallCmno();
            case STUDIO -> combo.getStudioCmno();
            case DRESS -> combo.getDressCmno();
            case MAKEUP -> combo.getMakeupCmno();
        };
    }

    private void applyUpgrade(AiPlanPackageCandidateDTO combo, CompanyCategory category, Company upgraded) {
        String reason = "예산에 맞춰 조금 더 여유 있는 곳으로 조정했어요";
        switch (category) {
            case HALL -> {
                HallItem hallItem = candidateBuilder.resolveHallItem(upgraded);
                combo.setHallCmno(upgraded.getCmno());
                combo.setHallName(upgraded.getName());
                combo.setHallRoomName(hallItem != null ? hallItem.getItemName() : null);
                combo.setHallImageUrl(hallItem != null ? hallItem.getImageUrl() : AiPlanCandidateBuilder.firstImage(upgraded));
                combo.setHallPrice(hallItem != null ? hallItem.getPrice() : upgraded.getPriceAvg());
                combo.setHallReason(reason);
            }
            case STUDIO -> {
                combo.setStudioCmno(upgraded.getCmno());
                combo.setStudioName(upgraded.getName());
                combo.setStudioImageUrl(AiPlanCandidateBuilder.firstImage(upgraded));
                combo.setStudioPrice(upgraded.getPriceAvg());
                combo.setStudioReason(reason);
            }
            case DRESS -> {
                DressItem item = candidateBuilder.resolveDressItem(upgraded, List.of());
                combo.setDressCmno(upgraded.getCmno());
                combo.setDressName(upgraded.getName());
                combo.setDressItemId(item != null ? item.getDressItemId() : null);
                combo.setDressOptionName(item != null ? item.getItemName() : null);
                combo.setDressImageUrl(item != null ? item.getImageUrl() : null);
                combo.setDressPrice(item != null ? item.getPrice() : upgraded.getPriceAvg());
                combo.setDressReason(reason);
            }
            case MAKEUP -> {
                combo.setMakeupCmno(upgraded.getCmno());
                combo.setMakeupName(upgraded.getName());
                combo.setMakeupImageUrl(AiPlanCandidateBuilder.firstImage(upgraded));
                combo.setMakeupPrice(upgraded.getPriceAvg());
                // 예산 업그레이드로 갈아탄 업체라 원래 요청한 패키지 타입을 갖고 있는지 보장 안 됨 - 초기화.
                combo.setMakeupPackageType(null);
                combo.setMakeupReason(reason);
            }
        }
    }

    private AiPlanQuickResultDTO fallback(String region, Long budget, AiPlanDetailRequestDTO requestDTO) {
        AiPlanQuickResultDTO result = candidateBuilder.recommend(
                region, budget, requestDTO.getGuestCount(), AiPlanCategoryPreferences.fromDetailRequest(requestDTO));
        return attachSession(result, budget, region, requestDTO.getWeddingDate(), "AI_FALLBACK");
    }

    // 조합이 정확히 하나로 나왔을 때만 세션을 만든다 (AiPlanDetailServiceImpl과 동일한 규칙).
    // weddingDate는 세션 생성 여부와 무관하게 결과에 항상 실어보낸다 - 폼에 입력한 값을 결과 화면에도 보여주기 위함.
    private AiPlanQuickResultDTO attachSession(AiPlanQuickResultDTO result, Long budget, String region,
                                               java.time.LocalDate weddingDate, String mode) {
        if (result.getCandidates().size() == 1) {
            var session = sessionSupport.createSession(budget, region, weddingDate, mode, result.getCandidates().get(0));
            result.setSessionId(session.getSessionId());
        }
        result.setWeddingDate(weddingDate);
        return result;
    }

    // ── 후보 풀 구성 (SQL 단계) ────────────────────────────────────────

    private Map<CompanyCategory, List<Company>> buildPools(String region, Long budget, Integer guestCount,
                                                            String freeText) {

        Map<CompanyCategory, List<Company>> pools = new EnumMap<>(CompanyCategory.class);

        pools.put(CompanyCategory.HALL, filterExcluded(
                fetchHallPool(region, allocate(budget, AiPlanCandidateBuilder.HALL_RATIO), guestCount), freeText));
        pools.put(CompanyCategory.DRESS, filterExcluded(
                fetchPool(CompanyCategory.DRESS, region, allocate(budget, AiPlanCandidateBuilder.DRESS_RATIO)), freeText));
        pools.put(CompanyCategory.STUDIO, filterExcluded(
                fetchPool(CompanyCategory.STUDIO, region, allocate(budget, AiPlanCandidateBuilder.STUDIO_RATIO)), freeText));
        pools.put(CompanyCategory.MAKEUP, filterExcluded(
                fetchPool(CompanyCategory.MAKEUP, region, allocate(budget, AiPlanCandidateBuilder.MAKEUP_RATIO)), freeText));

        return pools;
    }

    // "OO는 빼고 찾아줘" 같은 자유 요청사항에서 언급된 업체를 후보 풀에서 미리 제거한다.
    private List<Company> filterExcluded(List<Company> pool, String freeText) {

        if (freeText == null || freeText.isBlank() || pool.isEmpty()) {
            return pool;
        }

        List<String> excludedNames = extractExcludedNames(pool, freeText);

        if (excludedNames.isEmpty()) {
            return pool;
        }

        return pool.stream()
                .filter(c -> excludedNames.stream().noneMatch(name -> name.equals(c.getName())))
                .toList();
    }

    private List<String> extractExcludedNames(List<Company> pool, String freeText) {

        String[] segments = freeText.split("[.,!?\\n]");
        List<String> excluded = new ArrayList<>();

        for (String segment : segments) {
            boolean hasCue = EXCLUDE_CUES.stream().anyMatch(segment::contains);
            if (!hasCue) {
                continue;
            }
            for (Company c : pool) {
                if (c.getName() != null && segment.contains(c.getName())) {
                    excluded.add(c.getName());
                }
            }
        }

        return excluded;
    }

    // 홀 후보 풀 - 하객수(guestCount) 필터가 걸려야 해서 CompanyRepository 대신 HallDetailRepository로 조회.
    private List<Company> fetchHallPool(String region, Long allocatedBudget, Integer guestCount) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;
        Sort sort = Sort.by("company.priceAvg").descending();

        List<HallDetail> pool = hallDetailRepository.searchByCapacity(region, maxPrice, guestCount, sort);

        if (pool.isEmpty() && region != null) {
            pool = hallDetailRepository.searchByCapacity(null, maxPrice, guestCount, sort);
        }

        return pool.stream().map(HallDetail::getCompany).limit(POOL_SIZE).toList();
    }

    private List<Company> fetchPool(CompanyCategory category, String region, Long allocatedBudget) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;
        Sort sort = Sort.by("priceAvg").descending();

        List<Company> pool = companyRepository
                .searchList(category, region, null, maxPrice, PageRequest.of(0, POOL_SIZE, sort))
                .getContent();

        if (pool.isEmpty() && region != null) {
            pool = companyRepository
                    .searchList(category, null, null, maxPrice, PageRequest.of(0, POOL_SIZE, sort))
                    .getContent();
        }

        return pool;
    }

    private Long allocate(Long budget, double ratio) {
        return budget != null && budget > 0 ? Math.round(budget * ratio) : null;
    }

    // ── 프롬프트 구성 ──────────────────────────────────────────────────

    private String buildPrompt(Map<CompanyCategory, List<Company>> pools, AiPlanDetailRequestDTO dto) {

        StringBuilder sb = new StringBuilder();

        sb.append("[예산] ").append(dto.getBudget() != null ? dto.getBudget() + "원" : "미입력").append('\n');
        sb.append("[지역] ").append(dto.getRegion() != null ? dto.getRegion() : "무관").append('\n');
        if (dto.getGuestCount() != null) {
            sb.append("[하객수] ").append(dto.getGuestCount()).append("명\n");
        }

        appendIfPresent(sb, "홀 분위기 선호", dto.getHallType());
        appendIfPresent(sb, "스튜디오 분위기 선호", dto.getStudioMood());
        appendIfPresent(sb, "드레스 스타일 선호", dto.getDressStyle());
        appendIfPresent(sb, "메이크업 스타일 선호", dto.getMakeupStyle());
        appendIfPresent(sb, "자유 요청사항", dto.getFreeText());

        for (CompanyCategory category : CompanyCategory.values()) {
            sb.append("\n[").append(category).append(" 후보]\n");
            for (Company c : pools.get(category)) {
                sb.append(String.format("- cmno=%d | %s | %s원 | %s | %s%n",
                        c.getCmno(),
                        c.getName(),
                        c.getPriceAvg() != null ? c.getPriceAvg().toBigInteger().toString() : "정보없음",
                        c.getAddress(),
                        c.getDescription() != null ? c.getDescription() : ""));
            }
        }

        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append('[').append(label).append("] ").append(value).append('\n');
        }
    }

    // ── AI 응답 파싱 + 그라운딩 재검증 ─────────────────────────────────

    private AiPlanPackageCandidateDTO parseAndValidate(String raw, Map<CompanyCategory, List<Company>> pools,
                                                       String dressStyleRaw, String makeupStyleRaw) {

        // 프론트가 칩 중복선택 결과를 콤마로 이어붙여 보내므로(예: "머메이드,벨라인") 여기서 쪼갠다.
        List<String> dressStyleKeywords = dressStyleRaw == null || dressStyleRaw.isBlank()
                ? List.of()
                : java.util.stream.Stream.of(dressStyleRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        // AI가 고른 메이크업 업체가 이 패키지를 실제로 갖고 있는지는 여기서 그라운딩하지 않으므로
        // (풀 자체가 패키지 타입으로 안 걸러짐), 프론트에서 옵션가를 다시 찾다가 없으면 평균가로
        // 안전하게 폴백한다는 전제로 요청값을 그대로 담아 보낸다.
        String makeupPackageType = null;
        if (makeupStyleRaw != null && !makeupStyleRaw.isBlank()) {
            try {
                makeupPackageType = MakeupPackageType.fromString(makeupStyleRaw.trim().toUpperCase()).name();
            } catch (IllegalArgumentException ignored) {
                // 잘못된 값이면 그냥 무시 - null 유지
            }
        }

        try {
            JsonNode root = objectMapper.readTree(raw);

            CategoryResult hall = parseCategory(root.get("hall"), pools.get(CompanyCategory.HALL));
            CategoryResult studio = parseCategory(root.get("studio"), pools.get(CompanyCategory.STUDIO));
            CategoryResult dress = parseCategory(root.get("dress"), pools.get(CompanyCategory.DRESS));
            CategoryResult makeup = parseCategory(root.get("makeup"), pools.get(CompanyCategory.MAKEUP));

            if (hall == null || studio == null || dress == null || makeup == null) {
                return null;
            }

            // 전부 빼달라고 하면 조합 자체가 성립하지 않음 - 그라운딩 실패와 동일하게 취급해 폴백시킨다
            if (hall.excluded() && studio.excluded() && dress.excluded() && makeup.excluded()) {
                log.warn("AiPlan AI: every category excluded by free text, falling back");
                return null;
            }

            String excludedNote = root.hasNonNull("excludedNote") ? root.get("excludedNote").asText() : null;

            DressItem dressItem = dress.excluded() ? null
                    : candidateBuilder.resolveDressItem(dress.company(), dressStyleKeywords);
            HallItem hallItem = hall.excluded() ? null : candidateBuilder.resolveHallItem(hall.company());

            BigDecimal totalPrice = java.util.stream.Stream.of(
                            hall.excluded() ? null : (hallItem != null ? hallItem.getPrice() : hall.company().getPriceAvg()),
                            studio.excluded() ? null : studio.company().getPriceAvg(),
                            dress.excluded() ? null : (dressItem != null ? dressItem.getPrice() : dress.company().getPriceAvg()),
                            makeup.excluded() ? null : makeup.company().getPriceAvg())
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return AiPlanPackageCandidateDTO.builder()
                    .pno(null)
                    .name("AI 추천 조합")
                    .description(excludedNote)
                    .packagePrice(totalPrice)
                    .distanceKm(null)
                    .hallCmno(hall.excluded() ? null : hall.company().getCmno())
                    .hallName(hall.excluded() ? null : hall.company().getName())
                    .hallRoomName(hallItem != null ? hallItem.getItemName() : null)
                    .hallReason(hall.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요" : hall.reason())
                    .hallImageUrl(hall.excluded() ? null
                            : (hallItem != null ? hallItem.getImageUrl() : AiPlanCandidateBuilder.firstImage(hall.company())))
                    .hallPrice(hall.excluded() ? null
                            : (hallItem != null ? hallItem.getPrice() : hall.company().getPriceAvg()))
                    .dressCmno(dress.excluded() ? null : dress.company().getCmno())
                    .dressName(dress.excluded() ? null : dress.company().getName())
                    .dressReason(dress.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요" : dress.reason())
                    .dressItemId(dressItem != null ? dressItem.getDressItemId() : null)
                    .dressOptionName(dressItem != null ? dressItem.getItemName() : null)
                    .dressImageUrl(dressItem != null ? dressItem.getImageUrl() : null)
                    .dressPrice(dress.excluded() ? null
                            : (dressItem != null ? dressItem.getPrice() : dress.company().getPriceAvg()))
                    .studioCmno(studio.excluded() ? null : studio.company().getCmno())
                    .studioName(studio.excluded() ? null : studio.company().getName())
                    .studioReason(studio.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요" : studio.reason())
                    .studioImageUrl(studio.excluded() ? null : AiPlanCandidateBuilder.firstImage(studio.company()))
                    .studioPrice(studio.excluded() ? null : studio.company().getPriceAvg())
                    .makeupCmno(makeup.excluded() ? null : makeup.company().getCmno())
                    .makeupName(makeup.excluded() ? null : makeup.company().getName())
                    .makeupReason(makeup.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요" : makeup.reason())
                    .makeupImageUrl(makeup.excluded() ? null : AiPlanCandidateBuilder.firstImage(makeup.company()))
                    .makeupPrice(makeup.excluded() ? null : makeup.company().getPriceAvg())
                    .makeupPackageType(makeup.excluded() ? null : makeupPackageType)
                    .sourceType("AI_COMBO")
                    .build();

        } catch (Exception e) {
            log.error("AiPlan AI: failed to parse response as JSON", e);
            return null;
        }
    }

    // exclude:true면 카테고리 자체를 안 고르는 게 정상이니 cmno 없이 통과시키고, 그 외엔 기존과
    // 동일하게 cmno가 실제 후보 풀 안에 있는지 재검증한다 (그라운딩 실패 시 전체 응답을 버림).
    private CategoryResult parseCategory(JsonNode node, List<Company> pool) {

        if (node == null) {
            return null;
        }

        boolean exclude = node.hasNonNull("exclude") && node.get("exclude").asBoolean();
        if (exclude) {
            return new CategoryResult(true, null, null);
        }

        if (!node.hasNonNull("cmno")) {
            return null;
        }

        long cmno = node.get("cmno").asLong();

        Company matched = pool.stream()
                .filter(c -> c.getCmno() == cmno)
                .findFirst()
                .orElse(null);

        if (matched == null) {
            log.warn("AiPlan AI: cmno={} not in candidate pool, discarding entire response", cmno);
            return null;
        }

        String reason = node.hasNonNull("reason") ? node.get("reason").asText() : null;

        return new CategoryResult(false, matched, reason);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private record CategoryResult(boolean excluded, Company company, String reason) {
    }
}
