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
import com.wedding.aiplan.repository.AiPlanSessionHistoryRepository;
import com.wedding.aiplan.repository.AiPlanSessionRepository;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.MakeupPackageRepository;
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
    private final MakeupPackageRepository makeupPackageRepository;
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
                .hallSlot(slotOf(combo.getHallCmno()))
                .studioSlot(slotOf(combo.getStudioCmno()))
                .dressSlot(slotOf(combo.getDressCmno()))
                .makeupSlot(slotOf(combo.getMakeupCmno()))
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
    private SlotState slotOf(Long cmno) {
        return SlotState.builder()
                .status(cmno != null ? SlotStatus.PENDING : SlotStatus.EXCLUDED)
                .selectedCmno(cmno)
                .build();
    }

    public Optional<AiPlanSession> findSession(Long sessionId) {
        return sessionRepository.findById(sessionId);
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

    private Map<String, Object> slotMap(SlotState slot) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", slot.getStatus().name());
        m.put("selectedCmno", slot.getSelectedCmno());
        m.put("note", slot.getNote());
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
                .hallReason(hall.reasonLabel())
                .hallStatus(hall.status.name())
                .studioCmno(studio.cmno())
                .studioName(studio.name())
                .studioImageUrl(studio.company != null ? AiPlanCandidateBuilder.firstImage(studio.company) : null)
                .studioPrice(studioAmount)
                .studioReason(studio.reasonLabel())
                .studioStatus(studio.status.name())
                .dressCmno(dress.cmno())
                .dressName(dress.name())
                .dressItemId(dressItem != null ? dressItem.getDressItemId() : null)
                .dressOptionName(dressItem != null ? dressItem.getItemName() : null)
                .dressImageUrl(dressItem != null ? dressItem.getImageUrl() : null)
                .dressPrice(dressAmount)
                .dressReason(dress.reasonLabel())
                .dressStatus(dress.status.name())
                .makeupCmno(makeup.cmno())
                .makeupName(makeup.name())
                .makeupImageUrl(makeup.company != null ? AiPlanCandidateBuilder.firstImage(makeup.company) : null)
                .makeupPrice(makeupAmount)
                .makeupReason(makeup.reasonLabel())
                .makeupStatus(makeup.status.name())
                .makeupPackageType(groundedMakeupType != null ? groundedMakeupType.name() : null)
                .sourceType(sourceType)
                .build();
    }

    // 세션에 저장해둔 원래 메이크업 취향(makeupPackageType)이 있어도, 지금 이 슬롯에 들어있는
    // 업체가 실제로 그 패키지를 파는지 매번 다시 확인한다 - "다시 찾기"/다듬기로 업체가 바뀌었는데
    // 취향 매칭에 실패해 아무 업체나 들어간 경우까지 그 패키지를 판다고 잘못 표시하면 안 된다.
    private MakeupPackageType groundedMakeupType(AiPlanSession session, Company makeupCompany) {
        if (session.getMakeupPackageType() == null || makeupCompany == null) {
            return null;
        }
        MakeupPackageType type;
        try {
            type = MakeupPackageType.valueOf(session.getMakeupPackageType());
        } catch (IllegalArgumentException e) {
            return null;
        }
        List<Long> supportingCmnos = makeupPackageRepository.findCompanyCmnosByPackageTypeIn(List.of(type));
        return supportingCmnos.contains(makeupCompany.getCmno()) ? type : null;
    }

    private SlotView resolve(SlotState slot) {
        if (slot == null || slot.getStatus() == SlotStatus.EXCLUDED || slot.getSelectedCmno() == null) {
            return new SlotView(null, slot != null ? slot.getStatus() : SlotStatus.EXCLUDED);
        }
        Company company = companyRepository.findById(slot.getSelectedCmno()).orElse(null);
        return new SlotView(company, slot.getStatus());
    }

    private record SlotView(Company company, SlotStatus status) {
        Long cmno() {
            return company != null ? company.getCmno() : null;
        }

        String name() {
            return company != null ? company.getName() : null;
        }

        String reasonLabel() {
            if (company == null) {
                return null;
            }
            return switch (status) {
                case CONFIRMED -> "확정하신 곳이에요";
                case EXCLUDED -> null;
                case PENDING -> "요청하신 대로 다시 골랐어요";
            };
        }
    }
}
