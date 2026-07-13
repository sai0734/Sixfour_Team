package com.wedding.reservation.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.reservation.domain.Reservation;
import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.dto.ReservationPaymentConfirmRequestDTO;
import com.wedding.reservation.repository.ReservationRepository;
import com.wedding.global.util.TossPaymentClient;

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

  @Override
  public Long register(ReservationDTO reservationDTO) {

    log.info("reservation register.........");

    Reservation reservation = modelMapper.map(reservationDTO, Reservation.class);

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

    reservation.changeWeddingDate(reservationDTO.getWeddingDate());
    reservation.changeStatus(reservationDTO.getStatus());
    reservation.changeMemo(reservationDTO.getMemo());

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

}
