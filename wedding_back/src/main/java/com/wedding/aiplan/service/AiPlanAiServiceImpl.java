package com.wedding.aiplan.service;

import java.math.BigDecimal;
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
import com.wedding.company.repository.CompanyRepository;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 문서 4번 매칭 파이프라인 그대로: (1) SQL로 후보 좁히기 (2) AI 호출 - 그 목록 안에서만 고르고
// JSON으로 응답 강제 (3) 서버 재검증 - AI가 준 cmno가 실제 후보 목록에 있는지 확인, 없으면 버림.
// AI 호출/파싱/그라운딩 중 어느 단계든 실패하면 AiPlanCandidateBuilder(4단계 로직)로 조용히 폴백한다 -
// AI가 죽어도 추천 자체는 항상 되게 하기 위함.
@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class AiPlanAiServiceImpl implements AiPlanAiService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final AiPlanCandidateBuilder candidateBuilder;

    // 카테고리당 AI에게 넘길 후보 개수 (너무 많으면 프롬프트/비용이 커짐)
    private static final int POOL_SIZE = 6;

    private static final String SYSTEM_PROMPT =
            "당신은 웨딩 준비 추천 엔진입니다. 아래 각 카테고리(HALL/STUDIO/DRESS/MAKEUP)마다 제공되는 후보 "
                    + "목록 안에서만 골라야 합니다. 목록에 없는 cmno는 절대 사용하지 마세요.\n"
                    + "사용자의 예산/지역/카테고리별 취향/자유 요청사항을 참고해서 각 카테고리마다 정확히 하나씩 고르고, "
                    + "왜 그 업체를 골랐는지 한국어로 한 문장씩 이유를 붙이세요. 취향과 정확히 맞는 후보가 없으면 "
                    + "그나마 가장 가까운 후보를 고르고 excludedNote에 그 사정을 설명하세요.\n"
                    + "반드시 아래 JSON 형식으로만 응답하세요. 다른 설명, 마크다운, 코드블록 없이 순수 JSON만 출력합니다:\n"
                    + "{\"hall\":{\"cmno\":0,\"reason\":\"...\"},\"studio\":{\"cmno\":0,\"reason\":\"...\"},"
                    + "\"dress\":{\"cmno\":0,\"reason\":\"...\"},\"makeup\":{\"cmno\":0,\"reason\":\"...\"},"
                    + "\"excludedNote\":\"...\"}";

    @Override
    public AiPlanQuickResultDTO getAiRecommendations(AiPlanDetailRequestDTO requestDTO) {

        String region = blankToNull(requestDTO.getRegion());
        Long budget = requestDTO.getBudget();

        Map<CompanyCategory, List<Company>> pools = buildPools(region, budget);

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

        AiPlanPackageCandidateDTO combo = parseAndValidate(raw, pools);

        if (combo == null) {
            log.warn("AiPlan AI: response failed parsing/grounding validation, falling back. raw={}", raw);
            return fallback(region, budget, requestDTO);
        }

        return AiPlanQuickResultDTO.builder()
                .candidates(List.of(combo))
                .regionRelaxed(false)
                .message("AI가 취향과 자유 입력을 반영해서 골라줬어요.")
                .build();
    }

    private AiPlanQuickResultDTO fallback(String region, Long budget, AiPlanDetailRequestDTO requestDTO) {
        return candidateBuilder.recommend(region, budget, AiPlanCategoryPreferences.fromDetailRequest(requestDTO));
    }

    // ── 후보 풀 구성 (SQL 단계) ────────────────────────────────────────

    private Map<CompanyCategory, List<Company>> buildPools(String region, Long budget) {

        Map<CompanyCategory, List<Company>> pools = new EnumMap<>(CompanyCategory.class);

        pools.put(CompanyCategory.HALL,
                fetchPool(CompanyCategory.HALL, region, allocate(budget, AiPlanCandidateBuilder.HALL_RATIO)));
        pools.put(CompanyCategory.DRESS,
                fetchPool(CompanyCategory.DRESS, region, allocate(budget, AiPlanCandidateBuilder.DRESS_RATIO)));
        pools.put(CompanyCategory.STUDIO,
                fetchPool(CompanyCategory.STUDIO, region, allocate(budget, AiPlanCandidateBuilder.STUDIO_RATIO)));
        pools.put(CompanyCategory.MAKEUP,
                fetchPool(CompanyCategory.MAKEUP, region, allocate(budget, AiPlanCandidateBuilder.MAKEUP_RATIO)));

        return pools;
    }

    private List<Company> fetchPool(CompanyCategory category, String region, Long allocatedBudget) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;
        Sort sort = Sort.by("priceAvg").descending();

        List<Company> pool = companyRepository
                .searchList(category, region, null, maxPrice, PageRequest.of(0, POOL_SIZE, sort))
                .getContent();

        // 지역 매칭 후보가 하나도 없으면 지역 조건 없이 한 번 더 (문서 5번 조건 완화와 같은 취지)
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

    private AiPlanPackageCandidateDTO parseAndValidate(String raw, Map<CompanyCategory, List<Company>> pools) {

        try {
            JsonNode root = objectMapper.readTree(raw);

            Picked hall = extractAndValidate(root.get("hall"), pools.get(CompanyCategory.HALL));
            Picked studio = extractAndValidate(root.get("studio"), pools.get(CompanyCategory.STUDIO));
            Picked dress = extractAndValidate(root.get("dress"), pools.get(CompanyCategory.DRESS));
            Picked makeup = extractAndValidate(root.get("makeup"), pools.get(CompanyCategory.MAKEUP));

            if (hall == null || studio == null || dress == null || makeup == null) {
                // 4개 중 하나라도 그라운딩 검증 실패(목록에 없는 cmno 등) - AI 응답 전체를 신뢰하지 않음
                return null;
            }

            String excludedNote = root.hasNonNull("excludedNote") ? root.get("excludedNote").asText() : null;

            BigDecimal totalPrice = List.of(hall, studio, dress, makeup).stream()
                    .map(p -> p.company.getPriceAvg() != null ? p.company.getPriceAvg() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return AiPlanPackageCandidateDTO.builder()
                    .pno(null)
                    .name("AI 추천 조합")
                    .description(excludedNote)
                    .packagePrice(totalPrice)
                    .distanceKm(null)
                    .hallCmno(hall.company.getCmno())
                    .hallName(hall.company.getName())
                    .hallReason(hall.reason)
                    .dressCmno(dress.company.getCmno())
                    .dressName(dress.company.getName())
                    .dressReason(dress.reason)
                    .studioCmno(studio.company.getCmno())
                    .studioName(studio.company.getName())
                    .studioReason(studio.reason)
                    .makeupCmno(makeup.company.getCmno())
                    .makeupName(makeup.company.getName())
                    .makeupReason(makeup.reason)
                    .sourceType("AI_COMBO")
                    .build();

        } catch (Exception e) {
            log.error("AiPlan AI: failed to parse response as JSON", e);
            return null;
        }
    }

    // node의 cmno가 실제로 그 카테고리 후보 목록(pool)에 있는지 확인 - 없으면 null(=신뢰 불가)
    private Picked extractAndValidate(JsonNode node, List<Company> pool) {

        if (node == null || !node.hasNonNull("cmno")) {
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

        return new Picked(matched, reason);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static class Picked {
        private final Company company;
        private final String reason;

        private Picked(Company company, String reason) {
            this.company = company;
            this.reason = reason;
        }
    }
}
