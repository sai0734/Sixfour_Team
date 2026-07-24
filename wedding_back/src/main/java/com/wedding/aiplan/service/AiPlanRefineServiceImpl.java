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
                    + "다만 사용자 발화가 그 확정된 카테고리도 같이 바꾸고 싶어하는 것처럼 보이면(예: \"나머지 "
                    + "새로운 걸로 다시 추천해줘\", \"전부 다시 찾아줘\" 등) 그 카테고리의 blocked를 true로 "
                    + "표시하세요 - action은 그래도 UNCHANGED로 두고 blocked만 true로 바꾸는 겁니다. "
                    + "그 외에는 blocked를 항상 false로 두세요.\n"
                    + "반드시 아래 JSON 형식으로만 응답하고 다른 설명은 절대 붙이지 마세요:\n"
                    + "{\"hall\":{\"action\":\"...\",\"note\":\"...\",\"blocked\":false},"
                    + "\"studio\":{\"action\":\"...\",\"note\":\"...\",\"blocked\":false},"
                    + "\"dress\":{\"action\":\"...\",\"note\":\"...\",\"blocked\":false},"
                    + "\"makeup\":{\"action\":\"...\",\"note\":\"...\",\"blocked\":false}}";

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

        // 결과 화면 배너는 원래 예산 늘리기 안내 전용으로만 쓰기로 정리했는데, "확정해둔 카테고리를
        // 깜빡하고 같이 바꿔달라고 했다가 조용히 무시당하는" 것도 사용자가 꼭 알아야 하는 정보라
        // 예외로 같이 띄운다. 예산 안내가 있으면 그게 우선이고, 없을 때만 확정 잠금 안내를 보여준다.
        AiPlanCandidateBuilder.BudgetSuggestion suggestion =
                AiPlanCandidateBuilder.budgetSuggestion(combo.getPackagePrice(), session.getBudget());

        List<String> blockedLabels = actions.entrySet().stream()
                .filter(e -> e.getValue().blocked())
                .map(e -> labelFor(e.getKey()))
                .toList();

        String message;
        Long suggestedBudget;
        if (suggestion != null) {
            message = suggestion.message();
            suggestedBudget = suggestion.suggestedBudget();
        } else if (!blockedLabels.isEmpty()) {
            message = String.join(" · ", blockedLabels)
                    + "는 이미 확정되어 있어서 수정하지 못했어요. 먼저 확정을 해제해주세요.";
            suggestedBudget = null;
        } else {
            message = "말씀하신 대로 반영했어요.";
            suggestedBudget = null;
        }

        return AiPlanQuickResultDTO.builder()
                .sessionId(session.getSessionId())
                .weddingDate(session.getWeddingDate())
                .candidates(List.of(combo))
                .message(message)
                .suggestedBudget(suggestedBudget)
                .build();
    }

    private String labelFor(CompanyCategory category) {
        return switch (category) {
            case HALL -> "웨딩홀";
            case STUDIO -> "스튜디오";
            case DRESS -> "드레스";
            case MAKEUP -> "메이크업";
        };
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

        switch (action) {
            case "CONFIRM" -> slot.changeStatus(SlotStatus.CONFIRMED);
            case "RECONSIDER" -> {
                if (slot.getStatus() != SlotStatus.CONFIRMED) {
                    reconsiderOne(session, category);
                }
                // 확정된 카테고리는 다시 찾기가 안 먹는다 - 프론트에서 버튼 자체가 비활성으로
                // 보이니 여기서는 조용히 무시한다(배너로 알릴 만큼 놀라운 상황이 아님).
            }
            case "EXCLUDE" -> {
                // 확정 상태여도 X는 바로 제외되게 한다 - 확정된 카테고리는 원래 이 액션을
                // 막았었는데, 그 안내 문구가 상단 배너로만 떠서 카드 쪽으로 스크롤해 내려온
                // 상태에선 안 보여 "눌러도 반응 없음"처럼 느껴졌다. X는 "이미 다른 데서
                // 예약해서 이 조합에서 뺀다"는 뜻이라 확정 여부와 상관없이 항상 되는 게 맞다.
                //
                // 재원 수정 - selectedCmno/pickReason은 일부러 안 지운다. EXCLUDED 상태 자체가
                // 화면(resolve())과 프롬프트(appendSlot())에서 이미 "제외됨"으로 취급해 감추는
                // 역할을 하므로, 굳이 지웠다가 "다시 찾기"에서 새로 검색할 필요가 없다 - 지워두지
                // 않으면 다시 찾기를 눌렀을 때 원래 있던 업체와 이유가 그대로 복원된다
                // (reconsiderOne 참고. 예전엔 여기서 지워버려서 다시 찾기가 항상 새 업체+AI로
                // 새로 지어낸 이유를 보여줬는데, "그냥 제외를 취소하고 싶은 건데 왜 다른 업체가
                // 나오냐"는 피드백으로 순수 복원 방식으로 바꿨다).
                slot.changeStatus(SlotStatus.EXCLUDED);
            }
            default -> slot.changeStatus(SlotStatus.PENDING);
        }

        // 사이드패널 버튼은 상단 조합 히스토리 배지에 새 항목으로 안 남는다 - 사용자가 자유발화로
        // 말한 "턴"만 배지가 되어야 하는데, 확정/해제/다시찾기/제외 버튼까지 배지로 쌓이면
        // 오히려 뭘 눌렀는지 헷갈린다는 피드백 반영. 대신 마지막 배지의 스냅샷은 갱신해서,
        // 나중에 그 배지를 다시 봐도 방금 한 조정이 사라지지 않게 한다.
        sessionSupport.refreshLatestHistorySnapshot(session);

        AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);

        // 결과 화면 배너는 예산 늘리기 안내 전용으로만 쓰기로 정리했다 - "확정했어요" 같은
        // 액션별 안내는 버튼 자체의 상태 변화(색/문구)로 이미 보이니 배너로 또 안 띄운다.
        // 다만 이 조정 이후에도 합계가 예산을 넘으면 그건 계속 보여줄 가치가 있는 정보라
        // 그때만 배너를 띄운다.
        AiPlanCandidateBuilder.BudgetSuggestion suggestion =
                AiPlanCandidateBuilder.budgetSuggestion(combo.getPackagePrice(), session.getBudget());

        return AiPlanQuickResultDTO.builder()
                .sessionId(session.getSessionId())
                .weddingDate(session.getWeddingDate())
                .candidates(List.of(combo))
                .message(suggestion != null ? suggestion.message() : null)
                .suggestedBudget(suggestion != null ? suggestion.suggestedBudget() : null)
                .build();
    }

    // "다시 찾기" 버튼 - 프론트에서 이 버튼은 EXCLUDED(제외됨) 상태 자리에서만 뜨고, X는 더 이상
    // selectedCmno/pickReason을 지우지 않으므로(applySlotAction의 EXCLUDE 참고), 대부분은 그냥
    // 상태만 PENDING으로 되돌리는 순수 "복원"이다 - 새로 검색하거나 AI를 새로 부르지 않는다.
    // "제외를 취소했을 뿐인데 왜 다른 업체/이유가 나오냐"는 피드백을 반영해 예전의 재검색 방식을
    // 걷어냈다. 다만 이 변경 이전에 이미 제외되어 selectedCmno가 지워진 옛날 세션까지 대비해,
    // 복원할 정보가 없을 때(selectedCmno==null)만 예외적으로 예전처럼 새로 검색한다.
    private void reconsiderOne(AiPlanSession session, CompanyCategory category) {

        SlotState target = slotFor(session, category);

        if (target.getSelectedCmno() != null) {
            target.changeStatus(SlotStatus.PENDING);
            return;
        }

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

        AiPlanCandidateBuilder.PickResult picked = candidateBuilder.pickOne(
                category, session.getRegion(), categoryBudget, preferencesForReconsider(category, session), null);

        target.changeStatus(SlotStatus.PENDING);
        target.changeSelectedCmno(picked != null && picked.company() != null ? picked.company().getCmno() : null);
        target.changeNote(null);

        String reason = null;
        if (picked != null && picked.company() != null) {
            reason = explainReasonWithAi(category, picked.company(), null, session);
            if (reason == null) {
                reason = candidateBuilder.pickReason(category, picked.company(), categoryBudget, session.getRegion());
            }
        }
        target.changePickReason(reason);
    }

    private double ratioFor(CompanyCategory category) {
        return switch (category) {
            case HALL -> AiPlanCandidateBuilder.HALL_RATIO;
            case DRESS -> AiPlanCandidateBuilder.DRESS_RATIO;
            case STUDIO -> AiPlanCandidateBuilder.STUDIO_RATIO;
            case MAKEUP -> AiPlanCandidateBuilder.MAKEUP_RATIO;
        };
    }

    // 상단 조합 히스토리 배지 클릭 - 그 턴의 스냅샷을 세션에 그대로 적용해서 보여준다.
    // rollback()과 달리 새 히스토리 턴을 추가하지 않는다 - 그냥 예전 시점을 "보는" 것뿐이라
    // 배지를 눌러볼 때마다 배지 목록 자체가 늘어나면 안 되기 때문. 이 상태에서 확정/다시찾기/
    // 다듬기 등 실제 변경을 하면 그건 기존 로직대로 새 턴이 되어 배지 목록 맨 뒤에 붙는다.
    @Override
    public AiPlanQuickResultDTO viewTurn(Long sessionId, int turnNo) {

        AiPlanSession session = sessionSupport.findSession(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없어요"));

        AiPlanSessionHistory target = sessionSupport.historyOf(sessionId).stream()
                .filter(h -> h.getTurnNo() == turnNo)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 기록을 찾을 수 없어요"));

        sessionSupport.applySnapshot(session, target);

        AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);

        // 결과 화면 배너는 예산 늘리기 안내 전용으로만 쓰기로 정리했다 - "첫 추천 조합이에요"
        // 같은 부가 설명은 더 이상 안 보여준다.
        AiPlanCandidateBuilder.BudgetSuggestion suggestion =
                AiPlanCandidateBuilder.budgetSuggestion(combo.getPackagePrice(), session.getBudget());

        return AiPlanQuickResultDTO.builder()
                .sessionId(sessionId)
                .weddingDate(session.getWeddingDate())
                .candidates(List.of(combo))
                .message(suggestion != null ? suggestion.message() : null)
                .suggestedBudget(suggestion != null ? suggestion.suggestedBudget() : null)
                .build();
    }

    // 새로고침 복원 - 세션이 이미 갖고 있는 상태를 그대로 다시 조립해서 돌려줌 (AI 호출 없음).
    // "예산을 더 쓰면 더 맞는 곳을 찾아준다"는 안내는 원래 최초 추천 시점에만 계산되고 세션엔
    // 저장이 안 돼서, 다른 페이지 갔다가 돌아오면(=새로고침 복원) 이 안내가 사라지는 문제가
    // 있었다 - 지금 조합 합계가 여전히 예산을 넘으면 매번 다시 계산해서 붙여준다.
    @Override
    public AiPlanQuickResultDTO getSession(Long sessionId) {

        AiPlanSession session = sessionSupport.findSession(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없어요"));

        AiPlanPackageCandidateDTO combo = sessionSupport.toCombo(session, "SESSION_COMBO", null);
        AiPlanCandidateBuilder.BudgetSuggestion suggestion =
                AiPlanCandidateBuilder.budgetSuggestion(combo.getPackagePrice(), session.getBudget());

        return AiPlanQuickResultDTO.builder()
                .sessionId(sessionId)
                .weddingDate(session.getWeddingDate())
                .candidates(List.of(combo))
                .message(suggestion != null ? suggestion.message() : null)
                .suggestedBudget(suggestion != null ? suggestion.suggestedBudget() : null)
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
        // 로그인 여부와 무관하게 "담기"를 눌렀다는 사실 자체를 표시 - DB 정리 배치가 이 값만
        // 보고 삭제 대상에서 뺀다(로그인 상태로 세션을 만들었다고 자동으로 담아진 걸로 치지 않음).
        session.changeAppliedToPlan(true);

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
        boolean blocked = node.hasNonNull("blocked") && node.get("blocked").asBoolean();
        return new Action(intent, (note == null || note.isBlank()) ? null : note, blocked);
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
        // 재원 수정 - 사이드패널 EXCLUDE(applySlotAction)와 동일하게, 자유발화로 "빼줘"라고
        // 해도 selectedCmno/pickReason은 지우지 않는다 - "다시 찾기"로 원래 업체를 그대로
        // 복원할 수 있어야 하기 때문 (reconsiderOne 참고).
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
            // 플랜 수정하기(다듬기 채팅)로 새 업체를 받으면 왜 그 업체를 골랐는지 AI한테 다시
            // 설명을 받는다 - 예산/지역 매칭 정도로만 계산한 문구보다 훨씬 구체적이다. 실패하면
            // (네트워크 등) 조용히 계산된 이유로 대체한다.
            String reason = null;
            if (picked != null && picked.company() != null) {
                reason = explainReasonWithAi(category, picked.company(), action.note(), session);
                if (reason == null) {
                    reason = candidateBuilder.pickReason(category, picked.company(), categoryBudget, session.getRegion());
                }
            }
            target.changePickReason(reason);
        }
    }

    // 다듬기 채팅으로 새로 고른 업체 하나에 대해 "왜 잘 맞는지" AI 설명을 받는다. 사용자가 남긴
    // 요청(note, 예: "더 화려한 걸로")까지 같이 넘겨서 그 요청에 맞춘 설명이 나오게 한다.
    // 실패하면 null을 돌려주고, 호출부에서 계산된 이유로 대체한다.
    private String explainReasonWithAi(CompanyCategory category, Company company, String note, AiPlanSession session) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("[업체]\n").append(categoryLabel(category)).append(": ").append(company.getName());
            if (company.getPriceAvg() != null) {
                prompt.append(" | ").append(String.format("%,d원", company.getPriceAvg().longValue()));
            }
            if (company.getDescription() != null && !company.getDescription().isBlank()) {
                prompt.append(" | ").append(company.getDescription());
            }
            prompt.append("\n[사용자 요청]\n").append(note != null ? note : "(특별한 요청 없음)");
            if (session.getRegion() != null) {
                prompt.append("\n[지역] ").append(session.getRegion());
            }

            OpenAiResponseDTO response = openAiClient.getJsonChatCompletion(List.of(
                    OpenAiMessageDTO.of("system", EXPLAIN_PICK_SYSTEM_PROMPT),
                    OpenAiMessageDTO.of("user", prompt.toString())));

            String raw = response.getChoices().get(0).getMessage().getContent();
            JsonNode root = objectMapper.readTree(raw);
            return root.hasNonNull("reason") ? root.get("reason").asText() : null;
        } catch (Exception e) {
            log.warn("AiPlan 다듬기 이유 설명 호출 실패 - 계산된 이유로 대체", e);
            return null;
        }
    }

    private static final String EXPLAIN_PICK_SYSTEM_PROMPT =
            "당신은 웨딩 준비 컨설턴트입니다. 아래 업체 정보와 사용자 요청을 보고, 이 업체가 왜 "
                    + "사용자 요청에 잘 맞는지 한국어 한 문장으로 구체적으로 설명하세요(업체 이름이나 "
                    + "특징을 직접 언급하며 설득력 있게, \"예산 내\"/\"지역에서 골랐어요\" 같은 뻔한 말은 "
                    + "피하세요). 반드시 아래 JSON 형식으로만 응답하세요: {\"reason\":\"...\"}";

    private String categoryLabel(CompanyCategory category) {
        return switch (category) {
            case HALL -> "웨딩홀";
            case STUDIO -> "스튜디오";
            case DRESS -> "드레스";
            case MAKEUP -> "메이크업";
        };
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

    private record Action(SlotStatus status, String note, boolean blocked) {
    }
}
