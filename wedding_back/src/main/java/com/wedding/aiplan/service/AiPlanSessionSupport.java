package com.wedding.aiplan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.aiplan.domain.AiPlanSession;
import com.wedding.aiplan.domain.AiPlanSessionHistory;
import com.wedding.aiplan.domain.SlotState;
import com.wedding.aiplan.domain.SlotStatus;
import com.wedding.aiplan.dto.AiPlanPackageCandidateDTO;
import com.wedding.aiplan.dto.AiPlanProgressDTO;
import com.wedding.aiplan.repository.AiPlanSessionHistoryRepository;
import com.wedding.aiplan.repository.AiPlanSessionRepository;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.member.dto.MemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 6단계(리파인 대화)를 위해 AiPlanSession을 만들고/읽고/화면용 DTO로 되돌리는 공용 로직.
// 후보가 여러 개인 "빠르게 모드"(패키지 목록)는 아직 하나를 확정 안 한 상태라 세션을 안 만들고,
// 조합이 정확히 하나로 나오는 자세히/AI 모드에서만 세션이 생긴다 (AiPlanDetailServiceImpl,
// AiPlanAiServiceImpl에서 호출).
//
// 히스토리 스냅샷은 Lombok으로 생성된 SlotState를 그대로 Jackson에 맡기면 setter가 없어서
// 역직렬화가 깨질 위험이 있어, Map으로 직접 펼쳐서 저장하고 JsonNode로 직접 읽어 SlotState.builder()로
// 복원한다 (AiPlanAiServiceImpl이 AI 응답을 JsonNode로 다루는 것과 같은 이유).
@Component
@RequiredArgsConstructor
@Log4j2
@Transactional
public class AiPlanSessionSupport {

    private final AiPlanSessionRepository sessionRepository;
    private final AiPlanSessionHistoryRepository historyRepository;
    private final CompanyRepository companyRepository;
    private final AiPlanCandidateBuilder candidateBuilder;
    private final ObjectMapper objectMapper;

    public AiPlanSession createSession(Long budget, String region, LocalDate weddingDate,
                                       String groomName, String brideName, String makeupPackageType, String mode,
                                       AiPlanPackageCandidateDTO combo) {

        AiPlanSession session = AiPlanSession.builder()
                .memberEmail(currentMemberEmailOrNull())
                .budget(budget)
                .region(region)
                .weddingDate(weddingDate)
                .groomName(groomName)
                .brideName(brideName)
                .makeupPackageType(makeupPackageType)
                .mode(mode)
                // 처음 추천 시점에 이미 계산해둔 "왜 이 업체를 골랐는지" 설명(AI가 준 이유거나
                // 규칙 기반 조합 사유)을 슬롯에 같이 저장한다 - 안 그러면 세션엔 cmno만 남고
                // 그 이유는 이 응답 한 번 보여주고 사라져버린다.
                .hallSlot(slotOf(combo.getHallCmno(), combo.getHallReason()))
                .studioSlot(slotOf(combo.getStudioCmno(), combo.getStudioReason()))
                .dressSlot(slotOf(combo.getDressCmno(), combo.getDressReason()))
                .makeupSlot(slotOf(combo.getMakeupCmno(), combo.getMakeupReason()))
                .build();

        session = sessionRepository.save(session);
        saveHistory(session, 0, null);
        markPendingStatus(combo); // 새로 만든 세션은 전부 PENDING - 프론트에 상태 필드로 알려줌
        return session;
    }

    // JWTCheckFilter가 aiplan 공개 경로에서도 토큰이 있으면 SecurityContext를 채워두므로,
    // 로그인 상태로 세션을 만들면 여기서 이메일을 꺼내 세션에 남긴다. 비로그인이면 null 그대로.
    // "이 결과 마이페이지에 담기"(AiPlanRefineServiceImpl.applyToPlan)에서 소유권 확인할 때도 재사용한다.
    public String currentMemberEmailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MemberDTO memberDTO)) {
            return null;
        }
        return memberDTO.getEmail();
    }

    private void markPendingStatus(AiPlanPackageCandidateDTO combo) {
        combo.setHallStatus(combo.getHallName() != null ? SlotStatus.PENDING.name() : null);
        combo.setStudioStatus(combo.getStudioName() != null ? SlotStatus.PENDING.name() : null);
        combo.setDressStatus(combo.getDressName() != null ? SlotStatus.PENDING.name() : null);
        combo.setMakeupStatus(combo.getMakeupName() != null ? SlotStatus.PENDING.name() : null);
    }

    // cmno가 없으면(예: AI 모드에서 카테고리를 통째로 제외한 조합) 처음부터 EXCLUDED로 시작한다 -
    // 안 그러면 리파인 프롬프트/사이드패널 상태가 "아직 안 고른 PENDING"으로 잘못 표시된다.
    private SlotState slotOf(Long cmno, String pickReason) {
        return SlotState.builder()
                .status(cmno != null ? SlotStatus.PENDING : SlotStatus.EXCLUDED)
                .selectedCmno(cmno)
                .pickReason(cmno != null ? pickReason : null)
                .build();
    }

    public Optional<AiPlanSession> findSession(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    // 메인 화면 위젯용 - 로그인 회원의 가장 최근 세션 기준으로 홀/드레스/스튜디오 슬롯 진행률을 계산.
    // 세션이 없으면(아직 AI 웨딩플랜을 안 써봤으면) 전부 0%로 내려서 프론트가 "아직 시작 전" 상태를 그린다.
    public AiPlanProgressDTO getMyProgress() {
        String email = currentMemberEmailOrNull();
        List<AiPlanSession> sessions = email == null
                ? List.of()
                : sessionRepository.findByMemberEmailOrderByUpdatedAtDesc(email);

        if (sessions.isEmpty()) {
            return AiPlanProgressDTO.builder().hasSession(false).build();
        }

        AiPlanSession latest = sessions.get(0);
        return AiPlanProgressDTO.builder()
                .hasSession(true)
                .hallPercent(slotPercent(latest.getHallSlot()))
                .dressPercent(slotPercent(latest.getDressSlot()))
                .studioPercent(slotPercent(latest.getStudioSlot()))
                .build();
    }

    // CONFIRMED(확정) = 100%, PENDING인데 후보가 골라져 있음 = 50%, 그 외(EXCLUDED/후보 없음) = 0%
    private int slotPercent(SlotState slot) {
        if (slot == null) {
            return 0;
        }
        return switch (slot.getStatus()) {
            case CONFIRMED -> 100;
            case PENDING -> slot.getSelectedCmno() != null ? 50 : 0;
            case EXCLUDED -> 0;
        };
    }

    public List<AiPlanSessionHistory> historyOf(Long sessionId) {
        return historyRepository.findBySessionIdOrderByTurnNoAsc(sessionId);
    }

    public void saveHistory(AiPlanSession session, int turnNo, String userMessage) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("hall", slotMap(session.getHallSlot()));
            snapshot.put("studio", slotMap(session.getStudioSlot()));
            snapshot.put("dress", slotMap(session.getDressSlot()));
            snapshot.put("makeup", slotMap(session.getMakeupSlot()));

            historyRepository.save(AiPlanSessionHistory.builder()
                    .sessionId(session.getSessionId())
                    .turnNo(turnNo)
                    .userMessage(userMessage)
                    .slotSnapshotJson(objectMapper.writeValueAsString(snapshot))
                    .build());
        } catch (Exception e) {
            log.error("AiPlan session history 저장 실패 (sessionId={})", session.getSessionId(), e);
        }
    }

    // 사이드패널 버튼(확정/해제/다시찾기/제외)은 사용자가 자유발화로 말한 "턴"이 아니라 조용한
    // 상태 조정이라 새 히스토리 배지를 만들지 않는다. 그렇다고 아예 기록을 안 남기면, 나중에
    // 상단 배지로 예전 턴을 봤다가 "현재"로 돌아올 때 방금 한 조정(확정/제외 등)이 사라져버리는
    // 문제가 생겨서 - 마지막 턴의 스냅샷을 지금 상태로 덮어써서 그 문제를 막는다.
    public void refreshLatestHistorySnapshot(AiPlanSession session) {
        List<AiPlanSessionHistory> history = historyOf(session.getSessionId());

        if (history.isEmpty()) {
            saveHistory(session, 0, null);
            return;
        }

        AiPlanSessionHistory latest = history.get(history.size() - 1);
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("hall", slotMap(session.getHallSlot()));
            snapshot.put("studio", slotMap(session.getStudioSlot()));
            snapshot.put("dress", slotMap(session.getDressSlot()));
            snapshot.put("makeup", slotMap(session.getMakeupSlot()));

            latest.changeSlotSnapshotJson(objectMapper.writeValueAsString(snapshot));
            historyRepository.save(latest);
        } catch (Exception e) {
            log.error("AiPlan session history 갱신 실패 (sessionId={})", session.getSessionId(), e);
        }
    }

    private Map<String, Object> slotMap(SlotState slot) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", slot.getStatus().name());
        m.put("selectedCmno", slot.getSelectedCmno());
        m.put("note", slot.getNote());
        m.put("pickReason", slot.getPickReason());
        return m;
    }

    public void applySnapshot(AiPlanSession session, AiPlanSessionHistory snapshot) {
        try {
            JsonNode root = objectMapper.readTree(snapshot.getSlotSnapshotJson());
            session.changeHallSlot(slotFromJson(root.get("hall")));
            session.changeStudioSlot(slotFromJson(root.get("studio")));
            session.changeDressSlot(slotFromJson(root.get("dress")));
            session.changeMakeupSlot(slotFromJson(root.get("makeup")));
        } catch (Exception e) {
            log.error("AiPlan history 복원 실패 (sessionId={})", session.getSessionId(), e);
        }
    }

    private SlotState slotFromJson(JsonNode node) {
        if (node == null) {
            return SlotState.empty();
        }
        return SlotState.builder()
                .status(SlotStatus.valueOf(node.get("status").asText()))
                .selectedCmno(node.hasNonNull("selectedCmno") ? node.get("selectedCmno").asLong() : null)
                .note(node.hasNonNull("note") ? node.get("note").asText() : null)
                .pickReason(node.hasNonNull("pickReason") ? node.get("pickReason").asText() : null)
                .build();
    }

    // 세션의 현재 슬롯 상태를 화면에 보여줄 DTO로 재조립.
    // EXCLUDED되었거나 아직 안 고른 슬롯은 이름/이미지가 전부 null로 나가고, 프론트는 그 슬롯을 안 그림.
    public AiPlanPackageCandidateDTO toCombo(AiPlanSession session, String sourceType, String description) {

        SlotView hall = resolve(session.getHallSlot());
        SlotView studio = resolve(session.getStudioSlot());
        SlotView dress = resolve(session.getDressSlot());
        SlotView makeup = resolve(session.getMakeupSlot());

        // 드레스는 "옵션(아이템)" 단위 표시 대상이라, 그 슬롯의 note(재검토 시 남긴 스타일 키워드)를
        // 같이 넘겨서 항상 같은 업체+키워드면 같은 아이템이 나오게 한다 (5단계).
        String dressNote = session.getDressSlot().getNote();
        DressItem dressItem = dress.company != null
                ? candidateBuilder.resolveDressItem(dress.company, dressNote != null ? List.of(dressNote) : List.of())
                : null;
        HallItem hallItem = hall.company != null ? candidateBuilder.resolveHallItem(hall.company) : null;

        BigDecimal hallAmount = hallItem != null ? hallItem.getPrice()
                : (hall.company != null ? hall.company.getPriceAvg() : null);
        BigDecimal dressAmount = dressItem != null ? dressItem.getPrice()
                : (dress.company != null ? dress.company.getPriceAvg() : null);
        BigDecimal studioAmount = studio.company != null ? studio.company.getPriceAvg() : null;

        // 세션에 저장해둔 원래 메이크업 취향이 지금 슬롯 업체에서도 실제로 유효한지 다시 확인하고
        // (groundedMakeupType), 유효하면 그 패키지 실제가(할인 반영)를 합계/카드 가격 양쪽에 쓴다 -
        // 안 그러면 카드엔 풀 패키지 가격을 보여주고 합계엔 업체 평균가만 더해지는 불일치가 생긴다.
        MakeupPackageType groundedMakeupType = groundedMakeupType(session, makeup.company);
        BigDecimal makeupAmount = candidateBuilder.resolveMakeupPrice(makeup.company, groundedMakeupType);

        BigDecimal totalPrice = Stream.of(hallAmount, dressAmount, studioAmount, makeupAmount)
                .map(v -> v != null ? v : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AiPlanPackageCandidateDTO.builder()
                .pno(null)
                .name("다듬은 조합")
                .description(description)
                .packagePrice(totalPrice)
                .distanceKm(null)
                .hallCmno(hall.cmno())
                .hallName(hall.name())
                .hallRoomName(hallItem != null ? hallItem.getItemName() : null)
                .hallImageUrl(hallItem != null ? hallItem.getImageUrl()
                        : (hall.company != null ? AiPlanCandidateBuilder.firstImage(hall.company) : null))
                .hallPrice(hallAmount)
                .hallReason(hall.reasonOrFallback(CompanyCategory.HALL, candidateBuilder, session.getBudget(), session.getRegion()))
                .hallStatus(hall.status.name())
                .studioCmno(studio.cmno())
                .studioName(studio.name())
                .studioImageUrl(studio.company != null ? AiPlanCandidateBuilder.firstImage(studio.company) : null)
                .studioPrice(studioAmount)
                .studioReason(studio.reasonOrFallback(CompanyCategory.STUDIO, candidateBuilder, session.getBudget(), session.getRegion()))
                .studioStatus(studio.status.name())
                .dressCmno(dress.cmno())
                .dressName(dress.name())
                .dressItemId(dressItem != null ? dressItem.getDressItemId() : null)
                .dressOptionName(dressItem != null ? dressItem.getItemName() : null)
                .dressImageUrl(dressItem != null ? dressItem.getImageUrl() : null)
                .dressPrice(dressAmount)
                .dressReason(dress.reasonOrFallback(CompanyCategory.DRESS, candidateBuilder, session.getBudget(), session.getRegion()))
                .dressStatus(dress.status.name())
                .makeupCmno(makeup.cmno())
                .makeupName(makeup.name())
                .makeupImageUrl(makeup.company != null ? AiPlanCandidateBuilder.firstImage(makeup.company) : null)
                .makeupPrice(makeupAmount)
                .makeupReason(makeup.reasonOrFallback(CompanyCategory.MAKEUP, candidateBuilder, session.getBudget(), session.getRegion()))
                .makeupStatus(makeup.status.name())
                .makeupPackageType(groundedMakeupType != null ? groundedMakeupType.name() : null)
                .sourceType(sourceType)
                .build();
    }

    // 세션에 저장해둔 원래 메이크업 취향(makeupPackageType)이 있어도, 지금 이 슬롯에 들어있는
    // 업체가 실제로 그 패키지를 파는지 매번 다시 확인한다 - "다시 찾기"/다듬기로 업체가 바뀌었는데
    // 취향 매칭에 실패해 아무 업체나 들어간 경우까지 그 패키지를 판다고 잘못 표시하면 안 된다.
    // 취향이 아예 없었거나(랜덤 추천) 못 파는 업체로 바뀐 경우엔, 그 업체가 실제로 파는 것 중
    // 가장 풍성한 패키지를 대신 보여준다.
    private MakeupPackageType groundedMakeupType(AiPlanSession session, Company makeupCompany) {
        if (makeupCompany == null) {
            return null;
        }
        MakeupPackageType preferred = null;
        if (session.getMakeupPackageType() != null) {
            try {
                preferred = MakeupPackageType.valueOf(session.getMakeupPackageType());
            } catch (IllegalArgumentException e) {
                preferred = null;
            }
        }
        if (preferred != null && candidateBuilder.makeupSupports(makeupCompany, preferred)) {
            return preferred;
        }
        return candidateBuilder.bestMakeupType(makeupCompany);
    }

    private SlotView resolve(SlotState slot) {
        if (slot == null || slot.getStatus() == SlotStatus.EXCLUDED || slot.getSelectedCmno() == null) {
            return new SlotView(null, slot != null ? slot.getStatus() : SlotStatus.EXCLUDED, null);
        }
        Company company = companyRepository.findById(slot.getSelectedCmno()).orElse(null);
        return new SlotView(company, slot.getStatus(), slot.getPickReason());
    }

    private record SlotView(Company company, SlotStatus status, String pickReason) {
        Long cmno() {
            return company != null ? company.getCmno() : null;
        }

        String name() {
            return company != null ? company.getName() : null;
        }

        // 슬롯에 저장해둔 "왜 이 업체를 골랐는지"가 있으면 그대로 쓰고(AI가 준 설명 등, 더 구체적),
        // 없으면(옛날 세션이거나 다시찾기에서 못 채운 경우) 예산/지역 매칭으로 계산한 걸로 대체한다.
        String reasonOrFallback(CompanyCategory category, AiPlanCandidateBuilder candidateBuilder,
                                 Long budget, String region) {
            if (status == SlotStatus.EXCLUDED || company == null) {
                return null;
            }
            if (pickReason != null && !pickReason.isBlank()) {
                return pickReason;
            }
            return candidateBuilder.pickReason(category, company, budget, region);
        }
    }
}
