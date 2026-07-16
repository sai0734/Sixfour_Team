package com.wedding.aiplan.service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.aiplan.domain.AiPlanSession;
import com.wedding.aiplan.domain.AiPlanSessionHistory;
import com.wedding.aiplan.domain.SlotState;
import com.wedding.aiplan.domain.SlotStatus;
import com.wedding.aiplan.dto.AiPlanPackageCandidateDTO;
import com.wedding.aiplan.dto.AiPlanQuickResultDTO;
import com.wedding.aiplan.dto.AiPlanRefineRequestDTO;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 6단계 - 결과 화면에서 바로 자유 발화로 다듬기. "스튜디오 빼줘", "드레스 다른 걸로" 같은 한 턴을
// 받아서 카테고리별로 CONFIRM/EXCLUDE/RECONSIDER/UNCHANGED로 분류하고(AI, 5단계와 같은 JSON 강제
// 응답 + 그라운딩 패턴), RECONSIDER인 카테고리만 남은 예산을 재분배해서 다시 검색한다.
//
// 5단계는 AI가 실패하면 규칙 기반으로 "조용히" 폴백하지만, 여기서는 그렇게 안 한다 - 잘못
// 해석해서 사용자가 확정한 슬롯까지 건드리면 더 나쁘기 때문에, AI 호출/파싱이 실패하면 세션을
// 아예 안 건드리고 "이해 못 했다"는 메시지만 돌려준다.
@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class AiPlanRefineServiceImpl implements AiPlanRefineService {

    private final AiPlanSessionSupport sessionSupport;
    private final AiPlanCandidateBuilder candidateBuilder;
    private final CompanyRepository companyRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT =
            "당신은 웨딩플랜 리파인 어시스턴트입니다. 사용자가 현재 고른 홀/스튜디오/드레스/메이크업 "
                    + "조합에 대해 자유롭게 말하면, 카테고리 4개 각각을 CONFIRM(마음에 들어서 확정)/"
                    + "EXCLUDE(빼줘)/RECONSIDER(다른 곳으로 다시 찾아줘)/UNCHANGED(이번 발화에서 언급 안 함) "
                    + "중 하나로 분류하세요. RECONSIDER면 어떤 스타일/조건을 원하는지 note에 한 문장으로 "
                    + "적고, 그 외엔 note를 빈 문자열로 두세요. 반드시 아래 JSON 형식으로만 응답하고 다른 "
                    + "설명은 절대 붙이지 마세요:\n"
                    + "{\"hall\":{\"action\":\"...\",\"note\":\"...\"},\"studio\":{\"action\":\"...\",\"note\":\"...\"},"
                    + "\"dress\":{\"action\":\"...\",\"note\":\"...\"},\"makeup\":{\"action\":\"...\",\"note\":\"...\"}}";

    @Override
    public AiPlanQuickResultDTO refine(AiPlanRefineRequestDTO requestDTO) {

        AiPlanSession session = sessionSupport.findSession(requestDTO.getSessionId())
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없어요"));

        Map<CompanyCategory, Action> actions = classify(session, requestDTO.getMessage());

        if (actions == null) {
            AiPlanPackageCandidateDTO unchanged = sessionSupport.toCombo(session, "SESSION_COMBO", null);
            return AiPlanQuickResultDTO.builder()
                    .sessionId(session.getSessionId())
                    .candidates(List.of(unchanged))
                    .message("지금 요청을 정확히 이해하지 못했어요. 조금 더 구체적으로 다시 말씀해주시겠어요?")
                    .build();
        }

        applyConfirmAndExclude(session, actions);
        reconsiderAndReallocate(session, actions);

        int nextTurn = sessionSupport.historyOf(session.getSessionId()).size();
        sessionSupport.saveHistory(session, nextTurn, requestDTO.getMessage());

        AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);

        return AiPlanQuickResultDTO.builder()
                .sessionId(session.getSessionId())
                .candidates(List.of(combo))
                .message("말씀하신 대로 반영했어요.")
                .build();
    }

    @Override
    public AiPlanQuickResultDTO rollback(Long sessionId) {

        AiPlanSession session = sessionSupport.findSession(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없어요"));

        List<AiPlanSessionHistory> history = sessionSupport.historyOf(sessionId);

        if (history.size() < 2) {
            AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);
            return AiPlanQuickResultDTO.builder()
                    .sessionId(sessionId)
                    .candidates(List.of(combo))
                    .message("더 되돌릴 이전 상태가 없어요.")
                    .build();
        }

        AiPlanSessionHistory previous = history.get(history.size() - 2);
        sessionSupport.applySnapshot(session, previous);
        sessionSupport.saveHistory(session, history.size(), "(되돌리기)");

        AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);

        return AiPlanQuickResultDTO.builder()
                .sessionId(sessionId)
                .candidates(List.of(combo))
                .message("이전 상태로 되돌렸어요.")
                .build();
    }

    // ── 발화 → 카테고리별 액션 분류 (AI) ─────────────────────────────

    private Map<CompanyCategory, Action> classify(AiPlanSession session, String message) {

        String raw;
        try {
            OpenAiResponseDTO response = openAiClient.getJsonChatCompletion(List.of(
                    OpenAiMessageDTO.of("system", SYSTEM_PROMPT),
                    OpenAiMessageDTO.of("user", buildPrompt(session, message))));

            raw = response.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("AiPlan refine AI 호출 실패", e);
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(raw);

            Map<CompanyCategory, Action> actions = new EnumMap<>(CompanyCategory.class);
            actions.put(CompanyCategory.HALL, actionOf(root.get("hall")));
            actions.put(CompanyCategory.STUDIO, actionOf(root.get("studio")));
            actions.put(CompanyCategory.DRESS, actionOf(root.get("dress")));
            actions.put(CompanyCategory.MAKEUP, actionOf(root.get("makeup")));

            if (actions.containsValue(null)) {
                return null;
            }
            return actions;
        } catch (Exception e) {
            log.error("AiPlan refine 응답 파싱 실패. raw={}", raw, e);
            return null;
        }
    }

    private Action actionOf(JsonNode node) {
        if (node == null || !node.hasNonNull("action")) {
            return null;
        }
        SlotStatus intent = switch (node.get("action").asText()) {
            case "CONFIRM" -> SlotStatus.CONFIRMED;
            case "EXCLUDE" -> SlotStatus.EXCLUDED;
            case "RECONSIDER" -> SlotStatus.PENDING;
            default -> null; // UNCHANGED - 상태를 안 바꿈
        };
        String note = node.hasNonNull("note") ? node.get("note").asText() : null;
        return new Action(intent, (note == null || note.isBlank()) ? null : note);
    }

    private String buildPrompt(AiPlanSession session, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("[현재 조합]\n");
        appendSlot(sb, "홀", session.getHallSlot());
        appendSlot(sb, "스튜디오", session.getStudioSlot());
        appendSlot(sb, "드레스", session.getDressSlot());
        appendSlot(sb, "메이크업", session.getMakeupSlot());
        sb.append("\n[사용자 발화]\n").append(message);
        return sb.toString();
    }

    private void appendSlot(StringBuilder sb, String label, SlotState slot) {
        sb.append("- ").append(label).append(": ");
        if (slot.getStatus() == SlotStatus.EXCLUDED || slot.getSelectedCmno() == null) {
            sb.append("(제외됨)\n");
            return;
        }
        companyRepository.findById(slot.getSelectedCmno()).ifPresentOrElse(
                c -> sb.append(c.getName()).append(" (").append(c.getPriceAvg()).append("원)\n"),
                () -> sb.append("(정보없음)\n"));
    }

    // ── 확정/제외 반영 ────────────────────────────────────────────────

    private void applyConfirmAndExclude(AiPlanSession session, Map<CompanyCategory, Action> actions) {

        applyOne(session.getHallSlot(), actions.get(CompanyCategory.HALL));
        applyOne(session.getStudioSlot(), actions.get(CompanyCategory.STUDIO));
        applyOne(session.getDressSlot(), actions.get(CompanyCategory.DRESS));
        applyOne(session.getMakeupSlot(), actions.get(CompanyCategory.MAKEUP));
    }

    private void applyOne(SlotState slot, Action action) {
        if (action.status() == null || action.status() == SlotStatus.PENDING) {
            return; // UNCHANGED 또는 RECONSIDER(재검토는 아래 reconsiderAndReallocate에서 따로 처리)
        }
        slot.changeStatus(action.status());
        if (action.status() == SlotStatus.EXCLUDED) {
            slot.changeSelectedCmno(null);
        }
    }

    // ── 재검토 카테고리 예산 재분배 + 재검색 ────────────────────────────

    private void reconsiderAndReallocate(AiPlanSession session, Map<CompanyCategory, Action> actions) {

        Map<CompanyCategory, Double> ratios = Map.of(
                CompanyCategory.HALL, AiPlanCandidateBuilder.HALL_RATIO,
                CompanyCategory.DRESS, AiPlanCandidateBuilder.DRESS_RATIO,
                CompanyCategory.STUDIO, AiPlanCandidateBuilder.STUDIO_RATIO,
                CompanyCategory.MAKEUP, AiPlanCandidateBuilder.MAKEUP_RATIO);

        List<CompanyCategory> reconsider = actions.entrySet().stream()
                .filter(e -> e.getValue().status() == SlotStatus.PENDING)
                .map(Map.Entry::getKey)
                .toList();

        if (reconsider.isEmpty()) {
            return;
        }

        BigDecimal confirmedSpend = confirmedSpend(session);
        Long budget = session.getBudget();
        Long remaining = budget != null ? Math.max(budget - confirmedSpend.longValue(), 0) : null;

        double ratioSum = reconsider.stream().mapToDouble(ratios::get).sum();

        for (CompanyCategory category : reconsider) {
            Action action = actions.get(category);
            Long categoryBudget = remaining != null
                    ? Math.round(remaining * (ratios.get(category) / ratioSum))
                    : null;

            AiPlanCategoryPreferences prefs = preferencesFor(category, action.note());
            Company picked = candidateBuilder.pickOne(category, session.getRegion(), categoryBudget, prefs);

            SlotState target = slotFor(session, category);
            target.changeStatus(SlotStatus.PENDING);
            target.changeSelectedCmno(picked != null ? picked.getCmno() : null);
            target.changeNote(action.note());
        }
    }

    // 홀은 자유 키워드 검색 인프라(HallType enum만 지원)가 없어서 예산/지역만으로 다시 찾음 -
    // "다른 느낌으로" 같은 뉘앙스는 아직 반영 못 함(추후 개선 여지).
    private AiPlanCategoryPreferences preferencesFor(CompanyCategory category, String note) {
        return switch (category) {
            case STUDIO -> AiPlanCategoryPreferences.of(null, note, null, null);
            case DRESS -> AiPlanCategoryPreferences.of(null, null, note, null);
            case MAKEUP -> AiPlanCategoryPreferences.of(null, null, null, note);
            case HALL -> AiPlanCategoryPreferences.empty();
        };
    }

    private SlotState slotFor(AiPlanSession session, CompanyCategory category) {
        return switch (category) {
            case HALL -> session.getHallSlot();
            case STUDIO -> session.getStudioSlot();
            case DRESS -> session.getDressSlot();
            case MAKEUP -> session.getMakeupSlot();
        };
    }

    private BigDecimal confirmedSpend(AiPlanSession session) {
        return List.of(session.getHallSlot(), session.getStudioSlot(),
                        session.getDressSlot(), session.getMakeupSlot()).stream()
                .filter(s -> s.getStatus() == SlotStatus.CONFIRMED && s.getSelectedCmno() != null)
                .map(s -> companyRepository.findById(s.getSelectedCmno())
                        .map(Company::getPriceAvg).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record Action(SlotStatus status, String note) {
    }
}
