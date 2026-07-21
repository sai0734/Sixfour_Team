package com.wedding.service;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.budget.domain.Budget;
import com.wedding.budget.repository.BudgetRepository;
import com.wedding.checklist.domain.Checklist;
import com.wedding.checklist.repository.ChecklistRepository;
import com.wedding.company.domain.Company;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.repository.ReservationRepository;
import com.wedding.reservation.service.ReservationService;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 매니저가 예약을 확인해서 "결제대기"로 넘어오는 시점(ReservationServiceImpl.confirmByManager)에
// 예산관리(카테고리별 계획예산)·체크리스트(2단계 업체 계약)가 자동으로 채워지는지 확인한다.
// AI 웨딩플랜 "담기" 버튼은 더 이상 이 둘을 안 건드리므로(AiPlanApplyToPlanTests 참고),
// 예약 확인 흐름만 별도로 검증한다. 롤백 기본값이라 테스트 끝나면 DB에 안 남음.
@SpringBootTest
@Log4j2
public class ReservationConfirmPrepSyncTests {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ChecklistRepository checklistRepository;

    // 강남 GARDEN 홀 더미데이터 - AiPlanDetailServiceTests 등에서도 같은 cmno를 기준으로 삼음
    private static final Long HALL_CMNO = 11L;

    @Transactional
    @Test
    public void testConfirmByManagerSyncsBudgetAndChecklist() {

        Company hall = companyRepository.findById(HALL_CMNO).orElseThrow();
        LocalDate weddingDate = LocalDate.now().plusMonths(6);

        Long reservationId = reservationService.register(ReservationDTO.builder()
                .memberEmail("ccc@ccc.com")
                .cmno(HALL_CMNO)
                .weddingDate(weddingDate)
                .amount(5_000_000)
                .build());

        // 확인 전(대기)에는 아직 준비관리에 아무것도 안 생겨야 함
        List<Budget> budgetsBefore = budgetRepository.findByMemberEmailOrderBySortOrderAsc("ccc@ccc.com");
        assertTrue(budgetsBefore.stream().noneMatch(b -> "홀".equals(b.getCategory())));

        // jjj@jjj.com은 시드 데이터상 관리자(ADMIN) 계정이라 confirmByManager 권한 검사를 통과함
        ReservationDTO confirmed = reservationService.confirmByManager(reservationId, "jjj@jjj.com");
        assertEquals("결제대기", confirmed.getStatus());

        Budget hallBudget = budgetRepository.findByMemberEmailOrderBySortOrderAsc("ccc@ccc.com").stream()
                .filter(b -> "홀".equals(b.getCategory()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("결제대기 전환 후 홀 예산 항목이 생겨야 함"));
        assertEquals(5_000_000L, hallBudget.getBudgetAmount());

        String expectedTitle = hall.getName() + " 결제";
        Checklist checklist = checklistRepository.findByMemberEmailOrderByStageAscSortOrderAsc("ccc@ccc.com").stream()
                .filter(c -> reservationId.equals(c.getReservationId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("결제대기 전환 후 체크리스트 항목이 생겨야 함"));
        assertEquals(expectedTitle, checklist.getTitle());
        assertEquals(2, checklist.getStage());
        assertEquals(weddingDate.minusDays(14), checklist.getDueDate());
    }
}
