package com.wedding.reservation.service;

// 승진 코드 추가
import java.time.LocalDateTime;
import java.util.ArrayList;
// 승진 코드 추가 끝
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
import com.wedding.company.dto.CompanyDTO;
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

  @Override
  public Long register(ReservationDTO reservationDTO) {

    log.info("reservation register.........");

    Reservation reservation = modelMapper.map(reservationDTO, Reservation.class);
    // 승진 코드 추가 - 등록 시 항상 예약대기(대기) 상태로 시작
    reservation.changeStatus("대기");
    // 승진 코드 추가 끝

    Reservation saved = reservationRepository.save(reservation);

    return saved.getReservationId();
  }

  @Override
  public ReservationDTO get(Long reservationId) {

    Optional<Reservation> result = reservationRepository.findById(reservationId);

    Reservation reservation = result.orElseThrow();

    return modelMapper.map(reservation, ReservationDTO.class);
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
            .map(r -> modelMapper.map(r, ReservationDTO.class))
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

    // 승진 코드 추가 - 결제 취소/실패 후 재결제 시 payStatus 복구
    reservation.resetPayStatusForRetry();
    // 승진 코드 추가 끝

    // 주문번호는 결제 시도할 때마다 새로 발급 (실패 후 재시도 대비)
    String orderNumber = "RSV-" + reservationId + "-" + System.currentTimeMillis();
    reservation.assignOrder(orderNumber);
    reservationRepository.save(reservation);

    log.info("reservation preparePayment - orderNumber: " + orderNumber);

    return modelMapper.map(reservation, ReservationDTO.class);
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

    // 토스페이먼츠 서버-서버 승인 (checkout 패키지와 동일한 공용 유틸 사용)
    tossPaymentClient.confirmPayment(
            requestDTO.getPaymentKey(), requestDTO.getOrderNumber(), requestDTO.getAmount());

    reservation.completePayment(requestDTO.getPaymentKey(), java.time.LocalDateTime.now());
    reservationRepository.save(reservation);

    log.info("reservation confirmPayment 완료 - reservationId: " + reservationId);

    return modelMapper.map(reservation, ReservationDTO.class);
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

    log.info("confirmByManager - reservationId: {}, status: {}",
        reservationId, reservation.getStatus());

    return modelMapper.map(reservation, ReservationDTO.class);
  }
  // 승진 코드 추가 끝

}
