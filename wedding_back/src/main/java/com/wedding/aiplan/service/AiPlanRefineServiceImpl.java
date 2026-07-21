package com.wedding.aiplan.service;

import java.math.BigDecimal;
import java.util.Arrays;
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
import com.wedding.aiplan.dto.AiPlanSlotActionRequestDTO;
import com.wedding.aiplan.dto.AiPlanTurnDTO;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;
import com.wedding.weddingplan.domain.WeddingPlan;
import com.wedding.weddingplan.repository.WeddingPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 6단계 - 결과 화면에서 바로 자유 발화로 다듬기. "스튜디오 빼줘", "드레스 다른 걸로" 같은 한 턴을
// 받아서 카테고리별로 CONFIRM/EXCLUDE/RECONSIDER/UNCHANGED로 분류하고(AI, 5단계와 같은 JSON 강제
// 응답 + 그라운딩 패턴), RECONSIDER인 카테고리만 남은 예산을 재분배해서 다시 검색한다.
//
// 5단계는 AI가 실패하면 규칙 기반으로 "조용히" 폴백하지만, 여기서는 그렇게 안 한다 - 잘못
// 해석해서 사용자가 확정한 슬롯까지 건드리면 더 나쁘기 때문에, AI 호출/파싱이 실패하면 세션을
// 아예 안 건드리고 "이해 못 했다"는 메시지만 돌려준다.
//
// 사이드패널 확정 버튼(applySlotAction)으로 CONFIRMED된 슬롯은 AI가 뭐라고 분류하든
// applyOne/reconsiderAndReallocate 양쪽에서 이중으로 보호된다 - "해제" 버튼으로만 다시 열림.
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
    private final WeddingPlanRepository weddingPlanRepository;

    private static final String SYSTEM_PROMPT =
            "당신은 웨딩플랜 리파인 어시스턴트입니다. 사용자가 현재 고른 홀/스튜디오/드레스/메이크업 "
                    + "조합에 대해 자유롭게 말하면, 카테고리 4개 각각을 CONFIRM(마음에 들어서 확정)/"
                    + "EXCLUDE(빼줘)/RECONSIDER(다른 곳으로 다시 찾아줘)/UNCHANGED(이번 발화에서 언급 안 함) "
                    + "중 하나로 분류하세요. RECONSIDER면 어떤 스타일/조건을 원하는지 note에 한 문장으로 "
                    + "적고, 그 외엔 note를 빈 문자열로 두세요. 이미 확정(CONFIRMED)되어 있다고 표시된 "
                    + "카테고리는 사용자가 뭐라고 말하든 반드시 UNCHANGED로 두세요(확정 해제는 버튼으로만 가능). "
                    + "반드시 아래 JSON 형식으로만 응답하고 다른 설명은 절대 붙이지 마세요:\n"
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
                    .weddingDate(session.getWeddingDate())
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
                .weddingDate(session.getWeddingDate())
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
                    .weddingDate(session.getWeddingDate())
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
                .weddingDate(session.getWeddingDate())
                .candidates(List.of(combo))
                .message("이전 상태로 되돌렸어요.")
                .build();
    }

    // 사이드패널 확정/해제/다시찾기 버튼 - AI 안 거치고 즉시 반영.
    // CONFIRM이면 그 즉시 다듬기 보호 대상이 되고, RECONSIDER는 제외됐던 카테고리를 실제로 다시 검색한다.
    @Override
    public AiPlanQuickResultDTO applySlotAction(AiPlanSlotActionRequestDTO requestDTO) {

        AiPlanSession session = sessionSupport.findSession(requestDTO.getSessionId())
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없어요"));

        CompanyCategory category = CompanyCategory.valueOf(requestDTO.getCategory());
        SlotState slot = slotFor(session, category);
        String action = requestDTO.getAction();

        String message;
        switch (action) {
            case "CONFIRM" -> {
                slot.changeStatus(SlotStatus.CONFIRMED);
                message = "확정했어요. 이제부터 이 카테고리는 다듬기에서 안 건드려요.";
            }
            case "RECONSIDER" -> {
                if (slot.getStatus() == SlotStatus.CONFIRMED) {
                    message = "확정된 카테고리는 다시 찾을 수 없어요. 먼저 해제해주세요.";
                } else {
                    reconsiderOne(session, category);
                    message = "다시 찾아봤어요.";
                }
            }
            case "EXCLUDE" -> {
                if (slot.getStatus() == SlotStatus.CONFIRMED) {
                    message = "확정된 카테고리는 제외할 수 없어요. 먼저 해제해주세요.";
                } else {
                    slot.changeStatus(SlotStatus.EXCLUDED);
                    slot.changeSelectedCmno(null);
                    message = "제외했어요. 다른 곳에서 예약하신 걸로 볼게요.";
                }
            }
            default -> {
                slot.changeStatus(SlotStatus.PENDING);
                message = "확정을 해제했어요.";
            }
        }

        int nextTurn = sessionSupport.historyOf(session.getSessionId()).size();
        sessionSupport.saveHistory(session, nextTurn, historyNote(action, category));

        AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);

        return AiPlanQuickResultDTO.builder()
                .sessionId(session.getSessionId())
                .weddingDate(session.getWeddingDate())
                .candidates(List.of(combo))
                .message(message)
                .build();
    }

    private String historyNote(String action, CompanyCategory category) {
        return switch (action) {
            case "CONFIRM" -> "(확정: " + category + ")";
            case "RECONSIDER" -> "(다시 찾기: " + category + ")";
            case "EXCLUDE" -> "(제외: " + category + ")";
            default -> "(확정 해제: " + category + ")";
        };
    }

    // "다시 찾기" 버튼 - 확정된 카테고리들에 쓴 예산을 빼고 남은 예산을, 아직 확정 안 된
    // 카테고리들끼리 원래 비율(HALL/DRESS/STUDIO/MAKEUP_RATIO)대로 나눠서 이 카테고리 몫만큼만
    // 배정한다 (reconsiderAndReallocate와 같은 사상이지만, 자유발화 없이 버튼 하나로 즉시 실행).
    private void reconsiderOne(AiPlanSession session, CompanyCategory category) {

        BigDecimal confirmedSpend = confirmedSpend(session);
        Long budget = session.getBudget();
        Long remaining = budget != null ? Math.max(budget - confirmedSpend.longValue(), 0) : null;

        double ratioSum = Arrays.stream(CompanyCategory.values())
                .filter(c -> slotFor(session, c).getStatus() != SlotStatus.CONFIRMED)
                .mapToDouble(this::ratioFor)
                .sum();

        Long categoryBudget = remaining != null
                ? Math.round(remaining * (ratioFor(category) / ratioSum))
                : null;

        // 지금 이 슬롯에 들어있는 업체를 후보에서 빼야 "다시 찾기"가 실제로 다른 곳을 돌려준다.
        Long currentCmno = slotFor(session, category).getSelectedCmno();
        AiPlanCandidateBuilder.PickResult picked = candidateBuilder.pickOne(
                category, session.getRegion(), categoryBudget, preferencesForReconsider(category, session), currentCmno);

        SlotState target = slotFor(session, category);
        target.changeStatus(SlotStatus.PENDING);
        target.changeSelectedCmno(picked != null && picked.company() != null ? picked.company().getCmno() : null);
        target.changeNote(null);
    }

    private double ratioFor(CompanyCategory category) {
        return switch (category) {
            case HALL -> AiPlanCandidateBuilder.HALL_RATIO;
            case DRESS -> AiPlanCandidateBuilder.DRESS_RATIO;
            case STUDIO -> AiPlanCandidateBuilder.STUDIO_RATIO;
            case MAKEUP -> AiPlanCandidateBuilder.MAKEUP_RATIO;
        };
    }

    // 새로고침 복원 - 세션이 이미 갖고 있는 상태를 그대로 다시 조립해서 돌려줌 (AI 호출 없음)
    @Override
    public AiPlanQuickResultDTO getSession(Long sessionId) {

        AiPlanSession session = sessionSupport.findSession(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없어요"));

        AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);

        return AiPlanQuickResultDTO.builder()
                .sessionId(sessionId)
                .weddingDate(session.getWeddingDate())
                .candidates(List.of(combo))
                .message(null)
                .build();
    }

    // 다듬기 대화 기록 - 턴별 사용자 발화만 뽑아서 돌려줌 (슬롯 스냅샷 JSON은 안 내려줌)
    @Override
    public List<AiPlanTurnDTO> getRefineHistory(Long sessionId) {

        return sessionSupport.historyOf(sessionId).stream()
                .map(h -> AiPlanTurnDTO.builder()
                        .turnNo(h.getTurnNo())
                        .message(h.getUserMessage() != null ? h.getUserMessage() : "초기 추천 결과")
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    // ── "이 결과 마이페이지에 담기" ──────────────────────────────────
    //
    // 추천 도중 자동으로 반영하면 아직 확정 안 한 조합까지 준비관리에 섞여버리므로, 결과 화면에서
    // 사용자가 명시적으로 버튼을 눌렀을 때만 실행한다. 여기서는 웨딩플랜(예식일/총예산/신랑·신부
    // 이름)만 반영한다 - 예산관리(카테고리별 계획예산)·체크리스트(업체 계약)는 아직 확정 안 된
    // AI 추천이 아니라, 실제로 매니저가 예약을 확인해서 결제대기로 넘어온 시점에만 반영되도록
    // ReservationServiceImpl.confirmByManager() 쪽으로 옮겼다. 찜/예약은 이미 별도로 연동돼
    // 있어서 여기서 건드리지 않는다.
    @Override
    public Map<String, String> applyToPlan(Long sessionId) {

        String email = sessionSupport.currentMemberEmailOrNull();
        if (email == null) {
            throw new IllegalStateException("로그인 후 이용해주세요.");
        }

        AiPlanSession session = sessionSupport.findSession(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없어요"));

        // 비로그인으로 시작한 세션을 지금 로그인한 사람이 담으려는 경우 - 그 사람 것으로 소유권을 넘김
        if (session.getMemberEmail() == null) {
            session.changeMemberEmail(email);
        } else if (!session.getMemberEmail().equals(email)) {
            throw new IllegalStateException("본인이 만든 AI 웨딩플랜만 담을 수 있어요.");
        }

        applyWeddingPlan(email, session);

        return Map.of(
                "RESULT", "SUCCESS",
                "message", "예식일·총예산을 마이페이지 플랜에 반영했어요. 예산관리·체크리스트는 "
                        + "업체 예약을 확정(결제대기)하면 자동으로 채워져요.");
    }

    // 1:1이라 없으면 새로 만들고, 있으면 예식일/총예산만 덮어씀
    // (신랑·신부 이름·메모는 AI 플랜이 모르는 값이라 안 건드림 - 이름은 그래도 세션에 있으면 반영)
    private void applyWeddingPlan(String email, AiPlanSession session) {

        WeddingPlan plan = weddingPlanRepository.findByMemberEmail(email).orElse(null);

        if (plan == null) {
            weddingPlanRepository.save(WeddingPlan.builder()
                    .memberEmail(email)
                    .groomName(session.getGroomName())
                    .brideName(session.getBrideName())
                    .weddingDate(session.getWeddingDate())
                    .totalBudget(session.getBudget())
                    .build());
            return;
        }

        if (session.getGroomName() != null) {
            plan.changeGroomName(session.getGroomName());
        }
        if (session.getBrideName() != null) {
            plan.changeBrideName(session.getBrideName());
        }
        if (session.getWeddingDate() != null) {
            plan.changeWeddingDate(session.getWeddingDate());
        }
        if (session.getBudget() != null) {
            plan.changeTotalBudget(session.getBudget());
        }
        weddingPlanRepository.save(plan);
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
                c -> {
                    sb.append(c.getName()).append(" (").append(c.getPriceAvg()).append("원)");
                    if (slot.getStatus() == SlotStatus.CONFIRMED) {
                        sb.append(" [사용자가 확정함 - 절대 변경 금지]");
                    }
                    sb.append('\n');
                },
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
        if (slot.getStatus() == SlotStatus.CONFIRMED) {
            return; // 버튼으로 확정한 슬롯은 자유발화로 절대 안 바뀜 - "해제"는 버튼으로만 가능
        }
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
                .filter(e -> slotFor(session, e.getKey()).getStatus() != SlotStatus.CONFIRMED)
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

            AiPlanCategoryPreferences prefs = preferencesFor(category, action.note(), session);
            // 지금 이 슬롯에 들어있는 업체를 후보에서 빼야 "다른 걸로 바꿔줘"가 실제로 다른 곳을 돌려준다.
            Long currentCmno = slotFor(session, category).getSelectedCmno();
            AiPlanCandidateBuilder.PickResult picked =
                    candidateBuilder.pickOne(category, session.getRegion(), categoryBudget, prefs, currentCmno);

            SlotState target = slotFor(session, category);
            target.changeStatus(SlotStatus.PENDING);
            target.changeSelectedCmno(picked != null && picked.company() != null ? picked.company().getCmno() : null);
            target.changeNote(action.note());
        }
    }

    // 홀은 이제 구조화 값(HallType enum)으로 매칭하는데, 여기 note는 AI가 다듬기 대화에서 만들어낸
    // 자유 텍스트라 enum 토큰으로 안전하게 못 바꾼다 - 그래서 예산/지역만으로 다시 찾는다("다른
    // 느낌으로" 같은 뉘앙스는 아직 반영 못 함, 추후 개선 여지). 메이크업은 세션에 저장해둔 원래
    // 취향(makeupPackageType)을 계속 지킨다 - 다듬기 자유 텍스트로 바뀌는 게 아니라 처음 요청한
    // 패키지 타입을 유지하는 게 맞다. 스튜디오/드레스는 태그 문자열 LIKE 매칭이라 note 하나를
    // 그대로 키워드로 써도 된다(단일 항목 리스트).
    private AiPlanCategoryPreferences preferencesFor(CompanyCategory category, String note, AiPlanSession session) {
        List<String> noteList = note != null ? List.of(note) : List.of();
        return switch (category) {
            case STUDIO -> AiPlanCategoryPreferences.of(List.of(), noteList, List.of(), null);
            case DRESS -> AiPlanCategoryPreferences.of(List.of(), List.of(), noteList, null);
            case MAKEUP -> preferencesForReconsider(category, session);
            case HALL -> AiPlanCategoryPreferences.empty();
        };
    }

    // 사이드패널 "다시 찾기" 버튼(reconsiderOne)과 다듬기 대화의 메이크업 재검색이 공유하는 로직 -
    // 세션에 저장해둔 원래 메이크업 취향을 그대로 다시 써서, 재검색해도 처음 요청한 패키지 타입을
    // 계속 지킨다. MAKEUP이 아닌 카테고리는 (스튜디오/드레스 태그, 홀 타입 모두 세션에 저장해두지
    // 않으므로) 기존과 동일하게 예산/지역만으로 다시 찾는다.
    private AiPlanCategoryPreferences preferencesForReconsider(CompanyCategory category, AiPlanSession session) {
        if (category != CompanyCategory.MAKEUP || session.getMakeupPackageType() == null) {
            return AiPlanCategoryPreferences.empty();
        }
        try {
            MakeupPackageType type = MakeupPackageType.valueOf(session.getMakeupPackageType());
            return AiPlanCategoryPreferences.of(List.of(), List.of(), List.of(), type);
        } catch (IllegalArgumentException e) {
            return AiPlanCategoryPreferences.empty();
        }
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
