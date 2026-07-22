package com.wedding.aiplan.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // 메이크업 패키지 타입(예: FULL)을 요청했을 때, 그걸 실제로 파는 업체를 찾기 위해 일단 넓게
    // 훑어보는 개수. POOL_SIZE(6)만 미리 가격순으로 뽑으면 그 안에 지원 업체가 하나도 없을 수 있다.
    private static final int WIDE_POOL_SIZE = 30;

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

    // 규칙 기반 엔진이 골랐거나(예산 초과로 대체된 경우), 예산 여유로 업체가 바뀐 카테고리는
    // AI가 그 업체를 직접 고른 게 아니라서 이유가 없다 - "이미 정해진 4곳"을 그대로 보여주고
    // 왜 사용자 조건에 잘 맞는지 설명만 새로 받는다(선택은 안 바꾸고 설명만 채우는 보조 호출).
    private static final String EXPLAIN_SYSTEM_PROMPT =
            "당신은 웨딩 준비 컨설턴트입니다. 아래는 신랑신부를 위해 이미 확정된 홀/스튜디오/드레스/메이크업 "
                    + "업체입니다(변경 불가, 그대로 두세요). 사용자가 입력한 예산/하객수/분위기/스타일/자유 요청과 "
                    + "각 업체의 정보(설명, 가격)를 보고, 왜 이 업체가 사용자 조건에 잘 맞는지 카테고리별로 한국어 "
                    + "한 문장씩 구체적으로 설명하세요(업체 이름이나 특징을 직접 언급하며 설득력 있게, "
                    + "\"예산 내\"/\"지역에서 골랐어요\" 같은 뻔한 말은 피하세요). 후보로 안 들어온(null인) "
                    + "카테고리는 응답에서 생략하세요.\n"
                    + "반드시 아래 JSON 형식으로만 응답하세요. 다른 설명, 마크다운, 코드블록 없이 순수 JSON만 "
                    + "출력합니다: {\"hall\":\"...\",\"studio\":\"...\",\"dress\":\"...\",\"makeup\":\"...\"}";

    // 이미 확정된 4곳(cmno)을 그대로 두고 이유만 AI한테 새로 받아서 combo에 채운다. 실패해도
    // 조용히 무시한다 - 이미 채워져 있는 계산된 이유(pickReason)가 폴백으로 남아있다.
    private void explainWithAi(AiPlanPackageCandidateDTO combo, AiPlanDetailRequestDTO requestDTO) {
        try {
            OpenAiResponseDTO response = openAiClient.getJsonChatCompletion(List.of(
                    OpenAiMessageDTO.of("system", EXPLAIN_SYSTEM_PROMPT),
                    OpenAiMessageDTO.of("user", buildExplainPrompt(combo, requestDTO))));

            String raw = response.getChoices().get(0).getMessage().getContent();
            JsonNode root = objectMapper.readTree(raw);

            if (combo.getHallCmno() != null && root.hasNonNull("hall")) {
                combo.setHallReason(root.get("hall").asText());
            }
            if (combo.getStudioCmno() != null && root.hasNonNull("studio")) {
                combo.setStudioReason(root.get("studio").asText());
            }
            if (combo.getDressCmno() != null && root.hasNonNull("dress")) {
                combo.setDressReason(root.get("dress").asText());
            }
            if (combo.getMakeupCmno() != null && root.hasNonNull("makeup")) {
                combo.setMakeupReason(root.get("makeup").asText());
            }
        } catch (Exception e) {
            log.warn("AiPlan explain-reason 호출 실패 - 계산된 이유로 대체", e);
        }
    }

    private String buildExplainPrompt(AiPlanPackageCandidateDTO combo, AiPlanDetailRequestDTO requestDTO) {
        StringBuilder sb = new StringBuilder();
        sb.append("[확정된 업체]\n");
        appendExplainCompanyLine(sb, "홀", combo.getHallCmno(), combo.getHallName());
        appendExplainCompanyLine(sb, "스튜디오", combo.getStudioCmno(), combo.getStudioName());
        appendExplainCompanyLine(sb, "드레스", combo.getDressCmno(), combo.getDressName());
        appendExplainCompanyLine(sb, "메이크업", combo.getMakeupCmno(), combo.getMakeupName());

        sb.append("\n[사용자 조건]\n");
        appendIfPresent(sb, "예산", requestDTO.getBudget() != null ? String.format("%,d원", requestDTO.getBudget()) : null);
        appendIfPresent(sb, "지역", requestDTO.getRegion());
        appendIfPresent(sb, "하객수", requestDTO.getGuestCount() != null ? requestDTO.getGuestCount() + "명" : null);
        appendIfPresent(sb, "홀 분위기", requestDTO.getHallType());
        appendIfPresent(sb, "스튜디오 분위기", requestDTO.getStudioMood());
        appendIfPresent(sb, "드레스 스타일", requestDTO.getDressStyle());
        appendIfPresent(sb, "메이크업 패키지", requestDTO.getMakeupStyle());
        appendIfPresent(sb, "자유 요청", requestDTO.getFreeText());
        return sb.toString();
    }

    private void appendExplainCompanyLine(StringBuilder sb, String label, Long cmno, String name) {
        if (cmno == null) {
            return;
        }
        Company company = companyRepository.findById(cmno).orElse(null);
        sb.append("- ").append(label).append(": ").append(name != null ? name : "");
        if (company != null) {
            if (company.getPriceAvg() != null) {
                sb.append(" | ").append(String.format("%,d원", company.getPriceAvg().longValue()));
            }
            if (company.getDescription() != null && !company.getDescription().isBlank()) {
                sb.append(" | ").append(company.getDescription());
            }
        }
        sb.append('\n');
    }

    @Override
    public AiPlanQuickResultDTO getAiRecommendations(AiPlanDetailRequestDTO requestDTO) {

        String region = blankToNull(requestDTO.getRegion());
        Long budget = requestDTO.getBudget();

        // 요청한 메이크업 패키지 타입(예: FULL)을 실제로 파는 업체 cmno 목록을 미리 구해서, 후보 풀
        // 구성(buildPools)과 AI 응답 최종 검증(parseAndValidate) 둘 다에서 같은 기준으로 쓴다.
        // "블랑쉬 웨딩뷰티"처럼 헤어+메이크업만 파는 곳이 FULL 요청에 매칭되던 문제가 이 부분이었음.
        MakeupPackageType requestedMakeupType = parseMakeupPackageType(requestDTO.getMakeupStyle());
        Set<Long> makeupTypeCmnos = candidateBuilder.makeupCmnosSupporting(requestedMakeupType);

        Map<CompanyCategory, List<Company>> pools =
                buildPools(region, budget, requestDTO.getGuestCount(), requestDTO.getFreeText(), makeupTypeCmnos);

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

        AiPlanPackageCandidateDTO combo = parseAndValidate(
                raw, pools, requestDTO.getDressStyle(), requestedMakeupType, makeupTypeCmnos, budget, region);

        if (combo == null) {
            log.warn("AiPlan AI: response failed parsing/grounding validation, falling back. raw={}", raw);
            return fallback(region, budget, requestDTO);
        }

        if (budget != null && budget > 0 && fillComboTowardBudget(combo, region, budget)) {
            // 예산 여유로 한 카테고리 이상 업체가 바뀌면, 바뀐 자리는 AI가 원래 준 설명이 더 이상
            // 안 맞는다 - 최종 확정된 4곳을 그대로 다시 AI한테 보여주고 이유만 새로 받는다.
            explainWithAi(combo, requestDTO);
        }

        // 하객수/취향 조건 때문에 AI가 고른 조합이 예산을 톨러런스 넘게 초과했으면, 그 조합 대신 예산
        // 안에서(조건은 다 내려놓고) 고른 대안을 규칙 기반 엔진으로 다시 찾아 먼저 보여준다 - AI를 한 번
        // 더 호출하는 비용을 안 쓰기 위해 AiPlanCandidateBuilder(규칙 기반)로 대체한다. 규칙 기반 쪽은
        // 자기 자신을 재귀 호출하는 구조라 "이미 완화된 상태" 가드가 필요했지만, 여긴 완전히 별개
        // 호출이라 그 가드가 필요 없다. 메이크업 패키지 타입만은 (가격 영향이 작아서) 계속 유지한다 -
        // 안 그러면 "FULL 요청했는데 전혀 상관없는 패키지가 나온다"는 문제가 생긴다.
        AiPlanCandidateBuilder.BudgetSuggestion suggestion =
                AiPlanCandidateBuilder.budgetSuggestion(combo.getPackagePrice(), budget);
        if (suggestion != null) {
            AiPlanCategoryPreferences makeupOnly = AiPlanCategoryPreferences.of(
                    List.of(), List.of(), List.of(), requestedMakeupType);
            AiPlanQuickResultDTO budgetFit = candidateBuilder.recommend(
                    region, budget, null, makeupOnly);
            if (!budgetFit.getCandidates().isEmpty()) {
                // 안내가 진짜로 도움이 되는지 직접 검증한다 - "이만큼 늘리면 더 맞는 곳을 찾아준다"고
                // 말해놓고, 실제로 그 예산으로 다시 찾아봐도 카탈로그 한계 때문에 똑같은(더 안 비싼)
                // 조합이 나오면 안내 자체를 보여주지 않는다. 그래야 사용자가 안내를 보고 예산을
                // 늘려 다시 눌렀을 때 반드시 더 나은(더 비싼) 조합을 받게 된다는 게 보장된다.
                AiPlanQuickResultDTO bumpedFit = candidateBuilder.recommend(
                        region, suggestion.suggestedBudget(), null, makeupOnly);
                BigDecimal currentPrice = budgetFit.getCandidates().get(0).getPackagePrice();
                BigDecimal bumpedPrice = !bumpedFit.getCandidates().isEmpty()
                        ? bumpedFit.getCandidates().get(0).getPackagePrice() : null;
                boolean wouldActuallyImprove = bumpedPrice != null && currentPrice != null
                        && bumpedPrice.longValue() > currentPrice.longValue();
                if (wouldActuallyImprove) {
                    budgetFit.setSuggestedBudget(suggestion.suggestedBudget());
                    budgetFit.setMessage(suggestion.message());
                }
                // 이 조합은 AI가 고른 게 아니라 예산 초과로 규칙 기반 엔진이 대신 찾은 대안이라
                // 이유 설명이 없다 - 확정된 4곳을 그대로 AI한테 보여주고 설명만 받아온다.
                explainWithAi(budgetFit.getCandidates().get(0), requestDTO);
                return attachSession(budgetFit, budget, region, requestDTO, "AI_FALLBACK");
            }
        }

        // 예산 안내가 있으면 그 문구 하나만 보여준다 - "AI가 골라줬다" 같은 부가 설명은 결과
        // 화면 배너에 안 보여주기로 정리했다(예산 늘리기 안내 전용으로만 씀).
        return attachSession(AiPlanQuickResultDTO.builder()
                .candidates(List.of(combo))
                .regionRelaxed(false)
                .message(suggestion != null ? suggestion.message() : null)
                .suggestedBudget(suggestion != null ? suggestion.suggestedBudget() : null)
                .build(), budget, region, requestDTO, "AI");
    }

    // 규칙 기반 경로(AiPlanCandidateBuilder)에 넣은 것과 같은 방식 - AI가 고른 조합도 합계가 예산보다
    // 500만원 넘게 모자라면, 카테고리 순서대로 남은 여유 예산 안에서 더 비싼 대안이 있으면 갈아탄다.
    // 사용자가 통째로 빼달라고 한 카테고리(cmno null)는 건드리지 않는다.
    private boolean fillComboTowardBudget(AiPlanPackageCandidateDTO combo, String region, Long budget) {

        long total = combo.getPackagePrice() != null ? combo.getPackagePrice().longValue() : 0L;
        long leftover = budget - total;
        boolean anyUpgraded = false;

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
            applyUpgrade(combo, category, upgraded, budget, region);
            leftover -= gained;
            total += gained;
            anyUpgraded = true;
        }

        combo.setPackagePrice(BigDecimal.valueOf(total));
        return anyUpgraded;
    }

    private Long cmnoOf(AiPlanPackageCandidateDTO combo, CompanyCategory category) {
        return switch (category) {
            case HALL -> combo.getHallCmno();
            case STUDIO -> combo.getStudioCmno();
            case DRESS -> combo.getDressCmno();
            case MAKEUP -> combo.getMakeupCmno();
        };
    }

    private void applyUpgrade(AiPlanPackageCandidateDTO combo, CompanyCategory category, Company upgraded,
                              Long budget, String region) {
        // AI가 원래 고른 업체를 통째로 다른 곳(upgraded)으로 바꿔치기하는 거라, AI가 그 업체에 대해
        // 준 원래 설명은 더 이상 안 맞는다 - 새 업체 기준으로 다시 설명을 만든다(예산/지역 매칭 등,
        // pickReason 재사용). AI를 한 번 더 불러서 새로 설명받는 것보단 덜 자세하지만, 최소한
        // "예산에 맞춰 조정했어요" 같은 의미 없는 문구보단 낫다.
        String reason = candidateBuilder.pickReason(category, upgraded, budget, region);
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
                // 예산 업그레이드로 갈아탄 업체라 원래 요청한 패키지 타입을 갖고 있는지 보장 안 됨 -
                // 그 타입을 파는지 다시 확인하고, 못 파는 업체면 그 업체가 실제로 파는 것 중 가장
                // 풍성한 패키지로 대신 채운다(가격도 그 패키지 실제가로 다시 계산).
                MakeupPackageType requested = combo.getMakeupPackageType() != null
                        ? MakeupPackageType.valueOf(combo.getMakeupPackageType())
                        : null;
                MakeupPackageType groundedType = (requested != null && candidateBuilder.makeupSupports(upgraded, requested))
                        ? requested
                        : candidateBuilder.bestMakeupType(upgraded);
                combo.setMakeupCmno(upgraded.getCmno());
                combo.setMakeupName(upgraded.getName());
                combo.setMakeupImageUrl(AiPlanCandidateBuilder.firstImage(upgraded));
                combo.setMakeupPrice(candidateBuilder.resolveMakeupPrice(upgraded, groundedType));
                combo.setMakeupPackageType(groundedType != null ? groundedType.name() : null);
                combo.setMakeupReason(reason);
            }
        }
    }

    private AiPlanQuickResultDTO fallback(String region, Long budget, AiPlanDetailRequestDTO requestDTO) {
        AiPlanQuickResultDTO result = candidateBuilder.recommend(
                region, budget, requestDTO.getGuestCount(), AiPlanCategoryPreferences.fromDetailRequest(requestDTO));
        // 이 경로는 AI 호출 자체를 아예 안 거치거나 실패해서 규칙 기반으로 대신 찾은 조합이라
        // 이유 설명이 없다 - 확정된 4곳을 그대로 AI한테 보여주고 설명만 받아온다(실패해도 조용히
        // 무시하고 기존 계산된 이유로 넘어감).
        if (!result.getCandidates().isEmpty()) {
            explainWithAi(result.getCandidates().get(0), requestDTO);
        }
        return attachSession(result, budget, region, requestDTO, "AI_FALLBACK");
    }

    // 조합이 정확히 하나로 나왔을 때만 세션을 만든다 (AiPlanDetailServiceImpl과 동일한 규칙).
    // weddingDate는 세션 생성 여부와 무관하게 결과에 항상 실어보낸다 - 폼에 입력한 값을 결과 화면에도 보여주기 위함.
    private AiPlanQuickResultDTO attachSession(AiPlanQuickResultDTO result, Long budget, String region,
                                               AiPlanDetailRequestDTO requestDTO, String mode) {
        if (result.getCandidates().size() == 1) {
            MakeupPackageType makeupType = parseMakeupPackageType(requestDTO.getMakeupStyle());
            var session = sessionSupport.createSession(budget, region, requestDTO.getWeddingDate(),
                    requestDTO.getGroomName(), requestDTO.getBrideName(),
                    makeupType != null ? makeupType.name() : null,
                    mode, result.getCandidates().get(0));
            result.setSessionId(session.getSessionId());
        }
        result.setWeddingDate(requestDTO.getWeddingDate());
        return result;
    }

    // ── 후보 풀 구성 (SQL 단계) ────────────────────────────────────────

    private Map<CompanyCategory, List<Company>> buildPools(String region, Long budget, Integer guestCount,
                                                            String freeText, Set<Long> makeupTypeCmnos) {

        Map<CompanyCategory, List<Company>> pools = new EnumMap<>(CompanyCategory.class);

        pools.put(CompanyCategory.HALL, filterExcluded(
                fetchHallPool(region, allocate(budget, AiPlanCandidateBuilder.HALL_RATIO), guestCount), freeText));
        pools.put(CompanyCategory.DRESS, filterExcluded(
                fetchPool(CompanyCategory.DRESS, region, allocate(budget, AiPlanCandidateBuilder.DRESS_RATIO)), freeText));
        pools.put(CompanyCategory.STUDIO, filterExcluded(
                fetchPool(CompanyCategory.STUDIO, region, allocate(budget, AiPlanCandidateBuilder.STUDIO_RATIO)), freeText));
        pools.put(CompanyCategory.MAKEUP, filterExcluded(
                fetchMakeupPool(region, allocate(budget, AiPlanCandidateBuilder.MAKEUP_RATIO), makeupTypeCmnos), freeText));

        return pools;
    }

    // 요청한 메이크업 패키지 타입을 실제로 파는 업체만 AI에게 후보로 보여준다 - 없으면(그 지역/예산
    // 안엔 지원 업체가 없는 경우) 지역 무관으로 한 번 더 찾아보고, 그래도 없으면 일반 후보 풀로
    // 폴백한다(이 경우 parseAndValidate에서 makeupPackageType을 null로 비워 정직하게 처리함).
    private List<Company> fetchMakeupPool(String region, Long allocatedBudget, Set<Long> makeupTypeCmnos) {

        if (!makeupTypeCmnos.isEmpty()) {
            List<Company> matched = fetchPool(CompanyCategory.MAKEUP, region, allocatedBudget, makeupTypeCmnos);
            if (!matched.isEmpty()) {
                return matched;
            }
            if (region != null) {
                List<Company> matchedAnyRegion = fetchPool(CompanyCategory.MAKEUP, null, allocatedBudget, makeupTypeCmnos);
                if (!matchedAnyRegion.isEmpty()) {
                    return matchedAnyRegion;
                }
            }
        }

        return fetchPool(CompanyCategory.MAKEUP, region, allocatedBudget);
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

    // restrictToCmnos 안에 있는 업체만 남긴 후보 풀 (메이크업 패키지 타입 필터용). POOL_SIZE만
    // 미리 뽑으면 그 안에 지원 업체가 하나도 없을 수 있어서, WIDE_POOL_SIZE로 넉넉히 뽑은 뒤 거른다.
    private List<Company> fetchPool(CompanyCategory category, String region, Long allocatedBudget,
                                     Set<Long> restrictToCmnos) {

        BigDecimal maxPrice = allocatedBudget != null ? BigDecimal.valueOf(allocatedBudget) : null;
        Sort sort = Sort.by("priceAvg").descending();

        List<Company> wide = companyRepository
                .searchList(category, region, null, maxPrice, PageRequest.of(0, WIDE_POOL_SIZE, sort))
                .getContent();

        return wide.stream().filter(c -> restrictToCmnos.contains(c.getCmno())).limit(POOL_SIZE).toList();
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

    // AI가 그 카테고리를 왜 골랐는지 직접 설명해준 한국어 문장(디테일함, 예: "내추럴한 무드라
    // 요청하신 감성과 잘 맞아요")을 우선 쓴다. AI가 이유를 안 채워준 예외적인 경우에만 예산/지역
    // 매칭 여부로 계산한 문구로 대체한다. AiPlanSessionSupport.createSession이 이 값을 세션에
    // 그대로 저장해서, 이후 확정/새로고침/조합 히스토리 배지로 다시 봐도 이 설명이 유지된다.
    private String reasonOrFallback(String aiReason, CompanyCategory category, Company company,
                                    Long budget, String region) {
        if (aiReason != null && !aiReason.isBlank()) {
            return aiReason;
        }
        return candidateBuilder.pickReason(category, company, budget, region);
    }

    // ── AI 응답 파싱 + 그라운딩 재검증 ─────────────────────────────────

    private AiPlanPackageCandidateDTO parseAndValidate(String raw, Map<CompanyCategory, List<Company>> pools,
                                                       String dressStyleRaw, MakeupPackageType requestedMakeupType,
                                                       Set<Long> makeupTypeCmnos, Long budget, String region) {

        // 프론트가 칩 중복선택 결과를 콤마로 이어붙여 보내므로(예: "머메이드,벨라인") 여기서 쪼갠다.
        List<String> dressStyleKeywords = dressStyleRaw == null || dressStyleRaw.isBlank()
                ? List.of()
                : java.util.stream.Stream.of(dressStyleRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

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

            // 최종 그라운딩 - AI가 고른 메이크업 업체가 실제로 요청한 패키지 타입을 파는 업체
            // 목록(makeupTypeCmnos)에 있을 때만 packageType을 채운다. 후보 풀 자체를 못 좁혀서
            // 일반 풀로 폴백했던 경우까지 여기서 한 번 더 걸러내는 마지막 방어선 - 이게 없으면
            // "블랑쉬 웨딩뷰티"처럼 그 패키지를 안 파는 업체가 그 패키지를 파는 것처럼 나간다.
            MakeupPackageType groundedMakeupType = (!makeup.excluded() && requestedMakeupType != null
                    && makeupTypeCmnos.contains(makeup.company().getCmno()))
                    ? requestedMakeupType
                    : (makeup.excluded() ? null : candidateBuilder.bestMakeupType(makeup.company()));
            String makeupPackageType = groundedMakeupType != null ? groundedMakeupType.name() : null;

            DressItem dressItem = dress.excluded() ? null
                    : candidateBuilder.resolveDressItem(dress.company(), dressStyleKeywords);
            HallItem hallItem = hall.excluded() ? null : candidateBuilder.resolveHallItem(hall.company());
            // 패키지 취향이 그라운딩됐으면 그 패키지 실제가(할인 반영)를 쓴다 - 안 그러면 카드엔 풀
            // 패키지 가격을 보여주고 합계엔 업체 평균가만 더해지는 불일치가 생긴다.
            BigDecimal makeupAmount = makeup.excluded() ? null
                    : candidateBuilder.resolveMakeupPrice(makeup.company(), groundedMakeupType);

            BigDecimal totalPrice = java.util.stream.Stream.of(
                            hall.excluded() ? null : (hallItem != null ? hallItem.getPrice() : hall.company().getPriceAvg()),
                            studio.excluded() ? null : studio.company().getPriceAvg(),
                            dress.excluded() ? null : (dressItem != null ? dressItem.getPrice() : dress.company().getPriceAvg()),
                            makeupAmount)
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
                    .hallReason(hall.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요"
                            : reasonOrFallback(hall.reason(), CompanyCategory.HALL, hall.company(), budget, region))
                    .hallImageUrl(hall.excluded() ? null
                            : (hallItem != null ? hallItem.getImageUrl() : AiPlanCandidateBuilder.firstImage(hall.company())))
                    .hallPrice(hall.excluded() ? null
                            : (hallItem != null ? hallItem.getPrice() : hall.company().getPriceAvg()))
                    .dressCmno(dress.excluded() ? null : dress.company().getCmno())
                    .dressName(dress.excluded() ? null : dress.company().getName())
                    .dressReason(dress.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요"
                            : reasonOrFallback(dress.reason(), CompanyCategory.DRESS, dress.company(), budget, region))
                    .dressItemId(dressItem != null ? dressItem.getDressItemId() : null)
                    .dressOptionName(dressItem != null ? dressItem.getItemName() : null)
                    .dressImageUrl(dressItem != null ? dressItem.getImageUrl() : null)
                    .dressPrice(dress.excluded() ? null
                            : (dressItem != null ? dressItem.getPrice() : dress.company().getPriceAvg()))
                    .studioCmno(studio.excluded() ? null : studio.company().getCmno())
                    .studioName(studio.excluded() ? null : studio.company().getName())
                    .studioReason(studio.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요"
                            : reasonOrFallback(studio.reason(), CompanyCategory.STUDIO, studio.company(), budget, region))
                    .studioImageUrl(studio.excluded() ? null : AiPlanCandidateBuilder.firstImage(studio.company()))
                    .studioPrice(studio.excluded() ? null : studio.company().getPriceAvg())
                    .makeupCmno(makeup.excluded() ? null : makeup.company().getCmno())
                    .makeupName(makeup.excluded() ? null : makeup.company().getName())
                    .makeupReason(makeup.excluded() ? "요청하신 대로 이번엔 빼고 찾았어요"
                            : reasonOrFallback(makeup.reason(), CompanyCategory.MAKEUP, makeup.company(), budget, region))
                    .makeupImageUrl(makeup.excluded() ? null : AiPlanCandidateBuilder.firstImage(makeup.company()))
                    .makeupPrice(makeupAmount)
                    .makeupPackageType(makeupPackageType)
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

    private MakeupPackageType parseMakeupPackageType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MakeupPackageType.fromString(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record CategoryResult(boolean excluded, Company company, String reason) {
    }
}
