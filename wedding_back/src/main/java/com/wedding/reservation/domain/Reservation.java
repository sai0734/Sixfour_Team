package com.wedding.reservation.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_reservation")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long reservationId;

  private String memberEmail;

  // Company(D파트) 아직 없음 - 값만 저장, JPA 관계 없음 (CompanyWish와 동일 패턴)
  private Long cmno;

  private LocalDate weddingDate;

  // 대기 / 확정 / 취소
  @Builder.Default
  private String status = "대기";

  private String memo;

  // ↓↓↓ 재원 추가 - 업체 상세페이지 "예약" 버튼 → 날짜/옵션 선택 → 결제 흐름용
  // 선택한 옵션명 (예: "그랜드홀 A / 뷔페", "아이보리 볼가운")
  private String optionName;

  // 결제 금액
  @Builder.Default
  private int amount = 0;

  // 토스페이먼츠 주문번호 (prepare 시 발급, 일괄결제 시 동일 번호 공유 가능)
  private String orderNumber;

  // 토스페이먼츠 결제 승인키 (confirm 성공 시 저장)
  private String paymentKey;

  // NONE(미결제) / PAID(결제완료) / CANCELLED(취소·실패)
  @Builder.Default
  private String payStatus = "NONE";

  private java.time.LocalDateTime paidAt;
  // ↑↑↑ 재원 추가

  public void changeWeddingDate(LocalDate weddingDate) {
    this.weddingDate = weddingDate;
  }

  public void changeStatus(String status) {
    this.status = status;
  }

  public void changeMemo(String memo) {
    this.memo = memo;
  }

  // 승진 코드 추가 - 예약 수정 시 옵션/금액 변경
  public void changeOptionInfo(String optionName, int amount) {
    this.optionName = optionName;
    this.amount = amount;
  }
  // 승진 코드 추가 끝

  // 재원 추가 - 결제 관련 상태 변경 메서드
  public void assignOrder(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public void completePayment(String paymentKey, java.time.LocalDateTime paidAt) {
    this.paymentKey = paymentKey;
    this.payStatus = "PAID";
    this.paidAt = paidAt;
    this.status = "확정";
  }

  public void cancelPayment() {
    this.payStatus = "CANCELLED";
  }

  // 승진 코드 추가 - 결제 취소/실패 후 재결제 시 NONE으로 복구
  public void resetPayStatusForRetry() {
    if ("CANCELLED".equals(this.payStatus)) {
      this.payStatus = "NONE";
    }
  }
  // 승진 코드 추가 끝

}
