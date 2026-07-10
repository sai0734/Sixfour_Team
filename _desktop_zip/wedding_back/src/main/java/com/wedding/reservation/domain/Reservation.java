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

  public void changeWeddingDate(LocalDate weddingDate) {
    this.weddingDate = weddingDate;
  }

  public void changeStatus(String status) {
    this.status = status;
  }

  public void changeMemo(String memo) {
    this.memo = memo;
  }

}
