package com.wedding.reservation.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationDTO {

  private Long reservationId;

  private String memberEmail;

  private Long cmno;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate weddingDate;

  private String status;

  private String memo;

  // ↓↓↓ 재원 추가 - 결제 관련 필드
  private String optionName;

  private int amount;

  private String orderNumber;

  private String paymentKey;

  private String payStatus;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private java.time.LocalDateTime paidAt;
  // ↑↑↑ 재원 추가
}
