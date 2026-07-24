package com.wedding.reservation.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 승진 코드 추가
import com.wedding.reservation.dto.ReservationBulkPaymentConfirmRequestDTO;
import com.wedding.reservation.dto.ReservationBulkPaymentPrepareDTO;
// 승진 코드 추가 끝
import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.dto.ReservationPaymentConfirmRequestDTO;
import com.wedding.reservation.service.ReservationService;

import com.wedding.member.dto.MemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/reservations")
public class ReservationController {

  private final ReservationService service;

  // 회원의 예약 전체 조회 (마이페이지 "예약 현황" 탭용)
  @GetMapping("/member/{memberEmail}")
  public List<ReservationDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

    return service.listByMember(memberEmail);
  }

  // 승진 코드 추가 - 업체 예약관리 (경로 충돌 방지를 위해 /{reservationId}보다 위에 배치)
  @GetMapping("/company/{cmno}")
  public List<ReservationDTO> listByCompany(
          @AuthenticationPrincipal MemberDTO memberDTO,
          @PathVariable(name = "cmno") Long cmno) {

    log.info("ReservationController_listByCompany 실행~~~~~~~~ cmno={}", cmno);

    if (memberDTO == null) {
      throw new IllegalStateException("로그인이 필요합니다.");
    }

    return service.listByCompany(cmno, memberDTO.getEmail());
  }

  @GetMapping("/manager/mine")
  public List<ReservationDTO> listMyManagedCompany(
          @AuthenticationPrincipal MemberDTO memberDTO) {

    log.info("ReservationController_listMyManagedCompany 실행~~~~~~~~");

    if (memberDTO == null) {
      throw new IllegalStateException("로그인이 필요합니다.");
    }

    return service.listMyManagedCompany(memberDTO.getEmail());
  }

  @PutMapping("/{reservationId}/manager-confirm")
  public ReservationDTO confirmByManager(
          @AuthenticationPrincipal MemberDTO memberDTO,
          @PathVariable(name = "reservationId") Long reservationId) {

    log.info("ReservationController_confirmByManager 실행~~~~~~~~ id={}", reservationId);

    if (memberDTO == null) {
      throw new IllegalStateException("로그인이 필요합니다.");
    }

    return service.confirmByManager(reservationId, memberDTO.getEmail());
  }
  // 승진 코드 추가 끝

  // D파트 업체탐색 "예약하기" 버튼에서도 이 엔드포인트를 그대로 호출
  @PostMapping("/")
  public Map<String, Long> register(@RequestBody ReservationDTO reservationDTO) {

    log.info("ReservationDTO: " + reservationDTO);

    Long reservationId = service.register(reservationDTO);

    return Map.of("reservationId", reservationId);
  }

  @PutMapping("/{reservationId}")
  public Map<String, String> modify(
          @PathVariable(name = "reservationId") Long reservationId,
          @RequestBody ReservationDTO reservationDTO) {

    reservationDTO.setReservationId(reservationId);

    log.info("Modify: " + reservationDTO);

    service.modify(reservationDTO);

    return Map.of("RESULT", "SUCCESS");
  }

  @DeleteMapping("/{reservationId}")
  public Map<String, String> remove(@PathVariable(name = "reservationId") Long reservationId) {

    log.info("Remove: " + reservationId);

    service.remove(reservationId);

    return Map.of("RESULT", "SUCCESS");
  }

  // ↓↓↓ 재원 추가 - 업체 상세페이지 "예약" 버튼 → 날짜/옵션 선택 → 결제
  // (황용현님 checkout 패키지는 건드리지 않고, TossPaymentClient만 재사용)

  // 결제창 열기 전 - 주문번호 발급
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/{reservationId}/payment/prepare")
  public ReservationDTO preparePayment(Principal principal,
                                       @PathVariable(name = "reservationId") Long reservationId) {

    log.info("ReservationController_preparePayment 실행~~~~~~~~");

    return service.preparePayment(reservationId, principal.getName());
  }

  // 결제 승인
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/{reservationId}/payment/confirm")
  public ReservationDTO confirmPayment(Principal principal,
                                       @PathVariable(name = "reservationId") Long reservationId,
                                       @RequestBody ReservationPaymentConfirmRequestDTO requestDTO) {

    log.info("ReservationController_confirmPayment 실행~~~~~~~~");

    return service.confirmPayment(reservationId, principal.getName(), requestDTO);
  }

  // 결제 취소/실패 처리
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/{reservationId}/payment/cancel")
  public Map<String, String> cancelPayment(Principal principal,
                                           @PathVariable(name = "reservationId") Long reservationId) {

    log.info("ReservationController_cancelPayment 실행~~~~~~~~");

    service.cancelPayment(reservationId, principal.getName());

    return Map.of("RESULT", "SUCCESS");
  }

  // 업체 상세페이지 "결제 횟수" 표시용 - 로그인 불필요 (업체 상세는 비로그인도 조회 가능)
  @GetMapping("/company/{cmno}/payment-count")
  public Map<String, Long> getPaymentCount(@PathVariable(name = "cmno") Long cmno) {

    return Map.of("paymentCount", service.getPaymentCount(cmno));
  }

  // 예약 날짜 선택 시 미리 확인 - 로그인 불필요 (예약 화면 진입 전에도 확인 가능해야 함)
  @GetMapping("/company/{cmno}/availability")
  public Map<String, Boolean> checkAvailability(
          @PathVariable(name = "cmno") Long cmno,
          @RequestParam(name = "optionName") String optionName,
          @RequestParam(name = "weddingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate weddingDate) {

    return Map.of("taken", service.isDateTaken(cmno, optionName, weddingDate));
  }
  // ↑↑↑ 재원 추가

  // 승진 코드 추가
  // 묶음 결제 - 주문번호 발급
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/payment/bulk-prepare")
  public ReservationBulkPaymentPrepareDTO prepareBulkPayment(Principal principal,
                                                             @RequestBody List<Long> reservationIds) {

    log.info("ReservationController_prepareBulkPayment 실행~~~~~~~~ ids={}", reservationIds);

    return service.prepareBulkPayment(reservationIds, principal.getName());
  }

  // 묶음 결제 - 토스 승인
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/payment/bulk-confirm")
  public List<ReservationDTO> confirmBulkPayment(Principal principal,
                                                 @RequestBody ReservationBulkPaymentConfirmRequestDTO requestDTO) {

    log.info("ReservationController_confirmBulkPayment 실행~~~~~~~~ orderNumber={}", requestDTO.getOrderNumber());

    return service.confirmBulkPayment(principal.getName(), requestDTO);
  }

  // 묶음 결제 취소/실패 처리
  @PreAuthorize("hasAnyRole('USER')")
  @PostMapping("/payment/bulk-cancel")
  public Map<String, String> cancelBulkPayment(Principal principal,
                                               @RequestBody List<Long> reservationIds) {

    log.info("ReservationController_cancelBulkPayment 실행~~~~~~~~ ids={}", reservationIds);

    service.cancelBulkPayment(reservationIds, principal.getName());

    return Map.of("RESULT", "SUCCESS");
  }
  // 승진 코드 추가 끝

}
