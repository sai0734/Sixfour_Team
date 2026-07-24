package com.wedding.reservation.service;

// 승진 코드 추가
import java.time.LocalDateTime;
import java.util.ArrayList;
// 승진 코드 추가 끝
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.reservation.domain.Reservation;
// 승진 코드 추가
import com.wedding.reservation.dto.ReservationBulkPaymentConfirmRequestDTO;
import com.wedding.reservation.dto.ReservationBulkPaymentPrepareDTO;
// 승진 코드 추가 끝
import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.dto.ReservationPaymentConfirmRequestDTO;
import com.wedding.reservation.repository.ReservationRepository;
import com.wedding.global.util.TossPaymentClient;
import com.wedding.budget.domain.Budget;
import com.wedding.budget.repository.BudgetRepository;
import com.wedding.checklist.domain.Checklist;
import com.wedding.checklist.repository.ChecklistRepository;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.service.CompanyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReservationServiceImpl implements ReservationService {

  private final ReservationRepository reservationRepository;

  private final ModelMapper modelMapper;

  // 재원 추가 - 결제 승인은 황용현님 checkout 패키지를 건드리지 않고
  // 공용 유틸(global.util.TossPaymentClient)만 재사용
  private final TossPaymentClient tossPaymentClient;

  // 승진 코드 추가 - 업체 예약관리 권한 검증
  private final ReservationAccessService reservationAccessService;
  private final CompanyService companyService;
  // 승진 코드 추가 끝

  // 재원 추가 - 매니저 확인(결제대기 전환)/실제 결제 완료 시 준비관리(예산관리/체크리스트)에
  // 자동 반영하기 위함
  private final CompanyRepository companyRepository;
  private final BudgetRepository budgetRepository;
  private final ChecklistRepository checklistRepository;
  // 재원 추가 끝

  // 재원 추가 - 결제 최소 기한: 예식일(weddingDate) 기준 며칠 전까지만 결제 가능한지
  // (업계 관행상 잔금 결제가 본식 1~2주 전에 몰리는 편이라 2주로 설정)
  private static final long PAYMENT_DEADLINE_DAYS = 14;

  // 예식일 - 14일 = 결제 가능한 마지막 날짜 (예식일이 없으면 마감일 없음)
  private LocalDate calculatePaymentDeadline(Reservation reservation) {
    if (reservation.getWeddingDate() == null) {
      return null;
    }
    return reservation.getWeddingDate().minusDays(PAYMENT_DEADLINE_DAYS);
  }

  // 마감일이 지났으면 결제 차단
  private void validatePaymentDeadline(Reservation reservation) {
    LocalDate deadline = calculatePaymentDeadline(reservation);
    if (deadline != null && LocalDate.now().isAfter(deadline)) {
      throw new IllegalStateException(
              "결제 가능 기한(예식일 " + PAYMENT_DEADLINE_DAYS + "일 전)이 지나 결제할 수 없습니다.");
    }
  }

  // ReservationDTO 변환 시 paymentDeadline까지 같이 채워서 반환 (프론트에서 마감일 표시용)
  private ReservationDTO toDTO(Reservation reservation) {
    ReservationDTO dto = modelMapper.map(reservation, ReservationDTO.class);
    dto.setPaymentDeadline(calculatePaymentDeadline(reservation));
    return dto;
  }
  // 재원 추가 끝

  @Override
  public Long register(ReservationDTO reservationDTO) {

    log.info("reservation register.........");

    // 재원 추가 - 같은 업체+같은 옵션+같은 예식일 예약이 이미 있으면 차단 (중복 예약 방지)
    // 옵션 없이 문의만 하는 예약(optionName 비어있음)은 배타적일 이유가 없어 대상에서 제외
    if (reservationDTO.getOptionName() != null
            && !reservationDTO.getOptionName().isBlank()
            && isDateTaken(
            reservationDTO.getCmno(),
            reservationDTO.getOptionName(),
            reservationDTO.getWeddingDate())) {
      throw new IllegalStateException("이미 예약된 날짜입니다. 다른 날짜를 선택해주세요.");
    }
    // 재원 추가 끝

    Reservation reservation = modelMapper.map(reservationDTO, Reservation.class);
    // 승진 코드 추가 - 등록 시 항상 예약대기(대기) 상태로 시작
    reservation.changeStatus("대기");
    // 승진 코드 추가 끝

    Reservation saved = reservationRepository.save(reservation);

    return saved.getReservationId();
  }

  @Override
  public void modify(ReservationDTO reservationDTO) {

    Optional<Reservation> result =
            reservationRepository.findById(reservationDTO.getReservationId());

    Reservation reservation = result.orElseThrow();

    // 승진 코드 추가 - 결제대기(status=결제대기) 상태에서는 수정 불가
    if ("결제대기".equals(reservation.getStatus())) {
      throw new IllegalStateException("결제대기 상태에서는 수정할 수 없습니다.");
    }
    // 승진 코드 추가 끝

    reservation.changeWeddingDate(reservationDTO.getWeddingDate());
    reservation.changeMemo(reservationDTO.getMemo());

    // 승진 코드 추가 - 옵션/금액 수정 (상태는 사용자가 변경하지 않음)
    if (reservationDTO.getOptionName() != null) {
      reservation.changeOptionInfo(
              reservationDTO.getOptionName(),
              reservationDTO.getAmount()
      );
    }
    // 승진 코드 추가 끝

    reservationRepository.save(reservation);
  }

  @Override
  public void remove(Long reservationId) {

    reservationRepository.deleteById(reservationId);
  }

  @Override
  public List<ReservationDTO> listByMember(String memberEmail) {

    List<Reservation> result =
            reservationRepository.findByMemberEmailOrderByReservationIdDesc(memberEmail);

    return result.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
  }

  // ↓↓↓ 재원 추가 - 업체 예약 결제

  @Override
  public ReservationDTO preparePayment(Long reservationId, String memberEmail) {

    Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

    if (!reservation.getMemberEmail().equals(memberEmail)) {
      throw new IllegalStateException("본인의 예약만 결제할 수 있습니다.");
    }

    if ("PAID".equals(reservation.getPayStatus())) {
      throw new IllegalStateException("이미 결제가 완료된 예약입니다.");
    }

    // 승진 코드 추가 - 업체 확인(결제대기) 후에만 결제 가능
    if (!"결제대기".equals(reservation.getStatus())) {
      throw new IllegalStateException("업체 확인 후 결제할 수 있습니다.");
    }
    if (reservation.getAmount() <= 0) {
      throw new IllegalStateException("결제 금액이 없는 예약입니다.");
    }
    // 승진 코드 추가 끝

    // 재원 추가 - 결제 최소 기한 체크 (예식일 14일 전 지나면 결제 차단)
    validatePaymentDeadline(reservation);

    // 승진 코드 추가 - 결제 취소/실패 후 재결제 시 payStatus 복구
    reservation.resetPayStatusForRetry();
    // 승진 코드 추가 끝

    // 주문번호는 결제 시도할 때마다 새로 발급 (실패 후 재시도 대비)
    String orderNumber = "RSV-" + reservationId + "-" + System.currentTimeMillis();
    reservation.assignOrder(orderNumber);
    reservationRepository.save(reservation);

    log.info("reservation preparePayment - orderNumber: " + orderNumber);

    return toDTO(reservation);
  }

  @Override
  public ReservationDTO confirmPayment(Long reservationId, String memberEmail,
                                       ReservationPaymentConfirmRequestDTO requestDTO) {

    Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

    if (!reservation.getMemberEmail().equals(memberEmail)) {
      throw new IllegalStateException("본인의 예약만 결제할 수 있습니다.");
    }

    if (reservation.getOrderNumber() == null
            || !reservation.getOrderNumber().equals(requestDTO.getOrderNumber())) {
      throw new IllegalStateException("주문 정보가 일치하지 않습니다.");
    }

    if (reservation.getAmount() != requestDTO.getAmount()) {
      throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
    }

    // 재원 추가 - prepare 이후 시간이 지나 마감일을 넘겨버린 경우까지 이중 방어
    validatePaymentDeadline(reservation);

    // 토스페이먼츠 서버-서버 승인 (checkout 패키지와 동일한 공용 유틸 사용)
    tossPaymentClient.confirmPayment(
            requestDTO.getPaymentKey(), requestDTO.getOrderNumber(), requestDTO.getAmount());

    reservation.completePayment(requestDTO.getPaymentKey(), java.time.LocalDateTime.now());
    reservationRepository.save(reservation);

    // 재원 추가 - 실제 결제가 끝난 시점에 예산관리 실지출/체크리스트 완료 표시에 반영
    syncPaymentArtifacts(reservation);

    log.info("reservation confirmPayment 완료 - reservationId: " + reservationId);

    return toDTO(reservation);
  }

  @Override
  public void cancelPayment(Long reservationId, String memberEmail) {

    Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

    if (!reservation.getMemberEmail().equals(memberEmail)) {
      throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
    }

    reservation.cancelPayment();
    reservationRepository.save(reservation);
  }

  // 업체 상세페이지 "결제 횟수" 표시용 - 결제 완료(PAID) 건만 카운트
  @Override
  public long getPaymentCount(Long cmno) {
    return reservationRepository.countByCmnoAndPayStatus(cmno, "PAID");
  }

  // 예약 날짜 선택 시 미리 확인 - 같은 업체+같은 옵션+같은 날짜 예약이 이미 있는지
  @Override
  public boolean isDateTaken(Long cmno, String optionName, LocalDate weddingDate) {
    if (cmno == null || optionName == null || optionName.isBlank() || weddingDate == null) {
      return false;
    }
    return reservationRepository.existsByCmnoAndOptionNameAndWeddingDate(
            cmno, optionName, weddingDate);
  }
  // ↑↑↑ 재원 추가

  // 승진 코드 추가
  @Override
  @Transactional
  public ReservationBulkPaymentPrepareDTO prepareBulkPayment(List<Long> reservationIds, String memberEmail) {

    if (reservationIds == null || reservationIds.isEmpty()) {
      throw new IllegalArgumentException("결제할 예약을 선택해주세요.");
    }

    String orderNumber = "RSV-BULK-" + System.currentTimeMillis();
    int totalAmount = 0;

    for (Long reservationId : reservationIds) {
      Reservation r = reservationRepository.findById(reservationId)
              .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

      if (!r.getMemberEmail().equals(memberEmail)) {
        throw new IllegalStateException("본인의 예약만 결제할 수 있습니다.");
      }
      if ("PAID".equals(r.getPayStatus())) {
        throw new IllegalStateException("이미 결제된 예약입니다: " + reservationId);
      }
      if (!"결제대기".equals(r.getStatus())) {
        throw new IllegalStateException("업체 확인 후 결제할 수 있습니다: " + reservationId);
      }
      if (r.getAmount() <= 0) {
        throw new IllegalStateException("결제 금액이 없는 예약입니다: " + reservationId);
      }
      // 재원 추가 - 일괄결제도 개별결제와 동일하게 결제 최소 기한 적용
      validatePaymentDeadline(r);
      // 재원 추가 끝

      r.resetPayStatusForRetry();
      r.assignOrder(orderNumber);
      reservationRepository.save(r);
      totalAmount += r.getAmount();
    }

    if (totalAmount <= 0) {
      throw new IllegalStateException("결제할 금액이 없습니다.");
    }

    log.info("prepareBulkPayment - orderNumber: {}, totalAmount: {}", orderNumber, totalAmount);

    return ReservationBulkPaymentPrepareDTO.builder()
            .orderNumber(orderNumber)
            .totalAmount(totalAmount)
            .reservationIds(reservationIds)
            .build();
  }

  @Override
  @Transactional
  public List<ReservationDTO> confirmBulkPayment(String memberEmail, ReservationBulkPaymentConfirmRequestDTO requestDTO) {

    if (requestDTO.getReservationIds() == null || requestDTO.getReservationIds().isEmpty()) {
      throw new IllegalArgumentException("결제할 예약 정보가 없습니다.");
    }

    int expectedTotal = 0;
    for (Long reservationId : requestDTO.getReservationIds()) {
      Reservation r = reservationRepository.findById(reservationId).orElseThrow();

      if (!r.getMemberEmail().equals(memberEmail)) {
        throw new IllegalStateException("본인의 예약만 결제할 수 있습니다.");
      }
      if (r.getOrderNumber() != null
              && !r.getOrderNumber().equals(requestDTO.getOrderNumber())) {
        throw new IllegalStateException("주문 정보가 일치하지 않습니다.");
      }
      expectedTotal += r.getAmount();
    }

    if (expectedTotal != requestDTO.getAmount()) {
      throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
    }

    tossPaymentClient.confirmPayment(
            requestDTO.getPaymentKey(), requestDTO.getOrderNumber(), requestDTO.getAmount());

    LocalDateTime now = LocalDateTime.now();
    List<ReservationDTO> result = new ArrayList<>();

    for (Long reservationId : requestDTO.getReservationIds()) {
      Reservation r = reservationRepository.findById(reservationId).orElseThrow();

      r.completePayment(requestDTO.getPaymentKey(), now);
      reservationRepository.save(r);

      // 재원 추가 - 묶음결제도 개별결제와 동일하게, 결제 완료된 건 하나하나 예산관리/체크리스트에 반영
      syncPaymentArtifacts(r);

      result.add(modelMapper.map(r, ReservationDTO.class));
    }

    log.info("confirmBulkPayment 완료 - reservationIds: {}", requestDTO.getReservationIds());

    return result;
  }

  @Override
  @Transactional
  public void cancelBulkPayment(List<Long> reservationIds, String memberEmail) {

    for (Long reservationId : reservationIds) {
      Reservation r = reservationRepository.findById(reservationId).orElseThrow();

      if (!r.getMemberEmail().equals(memberEmail)) {
        throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
      }

      r.cancelPayment();
      reservationRepository.save(r);
    }
  }
  // 승진 코드 추가 끝

  // 승진 코드 추가 - 업체 예약관리
  @Override
  public List<ReservationDTO> listByCompany(Long cmno, String callerEmail) {

    reservationAccessService.requireCanManageCompanyReservations(callerEmail, cmno);

    return reservationRepository.findByCmnoOrderByReservationIdDesc(cmno).stream()
            .map(r -> modelMapper.map(r, ReservationDTO.class))
            .collect(Collectors.toList());
  }

  @Override
  public List<ReservationDTO> listMyManagedCompany(String callerEmail) {

    CompanyDTO company = companyService.getManagedCompany(callerEmail);
    if (company == null || company.getCmno() == null) {
      throw new IllegalStateException("담당 업체가 없습니다.");
    }

    return listByCompany(company.getCmno(), callerEmail);
  }

  @Override
  @Transactional
  public ReservationDTO confirmByManager(Long reservationId, String callerEmail) {

    Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();

    reservationAccessService.requireCanManageCompanyReservations(
            callerEmail, reservation.getCmno());

    if (!"대기".equals(reservation.getStatus())) {
      throw new IllegalStateException("예약대기 상태만 확인할 수 있습니다.");
    }

    if (reservation.getAmount() > 0) {
      reservation.confirmForPayment();
    } else {
      reservation.changeStatus("확정");
    }

    reservationRepository.save(reservation);

    // 재원 추가 - 결제대기로 넘어온 시점에만 준비관리(예산관리/체크리스트)에 반영
    if ("결제대기".equals(reservation.getStatus())) {
      syncPrepArtifacts(reservation);
    }
    // 재원 추가 끝

    log.info("confirmByManager - reservationId: {}, status: {}",
            reservationId, reservation.getStatus());

    return modelMapper.map(reservation, ReservationDTO.class);
  }
  // 승진 코드 추가 끝

  // 재원 추가 - 결제대기 전환 시 예산관리(카테고리별 계획예산)·체크리스트(2단계 업체 계약)에
  // 자동 반영. AI 웨딩플랜 "담기" 버튼이 아니라 실제로 매니저가 예약을 확인한 시점에만 반영되는
  // 구조라, 아직 확정 안 된 후보와 실제 계약이 준비관리 화면에서 안 섞인다.
  private void syncPrepArtifacts(Reservation reservation) {

    Company company = companyRepository.findById(reservation.getCmno()).orElse(null);
    if (company == null) {
      return;
    }

    String category = budgetCategoryOf(company.getCategory());
    if (category != null) {
      upsertBudgetCategory(reservation.getMemberEmail(), category, (long) reservation.getAmount());
    }

    if (!checklistRepository.existsByReservationId(reservation.getReservationId())) {
      createContractChecklistItem(reservation, company.getName());
    }
  }

  // 재원 추가 - 실제 결제가 완료된 시점에 예산관리 실지출을 쌓고, 그 예약이 만든 체크리스트
  // 항목을 완료 체크한다. syncPrepArtifacts()는 "결제대기로 넘어온 시점"에 계획예산/체크리스트
  // 항목을 미리 만들어두는 것이고, 이건 그 뒤 실제 결제(confirmPayment/confirmBulkPayment)가
  // 끝난 시점에 실지출·완료 여부를 반영하는 것 - 서로 다른 이벤트라 메서드를 분리했다.
  private void syncPaymentArtifacts(Reservation reservation) {

    Company company = companyRepository.findById(reservation.getCmno()).orElse(null);
    if (company == null) {
      return;
    }

    String category = budgetCategoryOf(company.getCategory());
    if (category != null) {
      addBudgetActualAmount(reservation.getMemberEmail(), category, (long) reservation.getAmount());
    }

    checklistRepository.findByReservationId(reservation.getReservationId())
            .ifPresent(checklist -> {
              checklist.changeIsDone(true);
              checklistRepository.save(checklist);
            });
  }

  private String budgetCategoryOf(CompanyCategory category) {
    if (category == null) {
      return null;
    }
    return switch (category) {
      case HALL -> "홀";
      case STUDIO -> "스튜디오";
      case DRESS -> "드레스";
      case MAKEUP -> "메이크업";
    };
  }

  // 같은 카테고리 항목이 이미 있으면 계획예산만 갱신, 없으면 새로 만든다
  private void upsertBudgetCategory(String memberEmail, String category, Long amount) {

    List<Budget> budgets = budgetRepository.findByMemberEmailOrderBySortOrderAsc(memberEmail);
    Budget existing = budgets.stream()
            .filter(b -> category.equals(b.getCategory()))
            .findFirst()
            .orElse(null);

    if (existing != null) {
      existing.changeBudgetAmount(amount);
      budgetRepository.save(existing);
      return;
    }

    int nextOrder = budgets.stream().mapToInt(Budget::getSortOrder).max().orElse(0) + 1;
    budgetRepository.save(Budget.builder()
            .memberEmail(memberEmail)
            .category(category)
            .budgetAmount(amount)
            .actualAmount(0L)
            .sortOrder(nextOrder)
            .build());
  }

  // 재원 추가 - 같은 카테고리 항목이 있으면 실지출에 결제 금액을 더하고(계획예산은 안 건드림),
  // 없으면(계획예산 단계 없이 바로 결제까지 간 예외적인 경우 대비) 새로 만든다.
  private void addBudgetActualAmount(String memberEmail, String category, Long amount) {

    List<Budget> budgets = budgetRepository.findByMemberEmailOrderBySortOrderAsc(memberEmail);
    Budget existing = budgets.stream()
            .filter(b -> category.equals(b.getCategory()))
            .findFirst()
            .orElse(null);

    if (existing != null) {
      long current = existing.getActualAmount() != null ? existing.getActualAmount() : 0L;
      existing.changeActualAmount(current + amount);
      budgetRepository.save(existing);
      return;
    }

    int nextOrder = budgets.stream().mapToInt(Budget::getSortOrder).max().orElse(0) + 1;
    budgetRepository.save(Budget.builder()
            .memberEmail(memberEmail)
            .category(category)
            .budgetAmount(0L)
            .actualAmount(amount)
            .sortOrder(nextOrder)
            .build());
  }

  private void createContractChecklistItem(Reservation reservation, String companyName) {

    List<Checklist> checklists =
            checklistRepository.findByMemberEmailOrderByStageAscSortOrderAsc(reservation.getMemberEmail());
    int nextOrder = checklists.stream()
            .filter(c -> c.getStage() == 2)
            .mapToInt(Checklist::getSortOrder)
            .max()
            .orElse(0) + 1;

    checklistRepository.save(Checklist.builder()
            .memberEmail(reservation.getMemberEmail())
            .title(companyName + " 결제")
            .isDone(false)
            .dueDate(calculatePaymentDeadline(reservation))
            .stage(2)
            .sortOrder(nextOrder)
            .reservationId(reservation.getReservationId())
            .build());
  }

}
