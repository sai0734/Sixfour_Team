package com.wedding.hallpayment.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_hall_payment")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HallPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private String memberEmail;

    // tbl_company(HALL) 기준 확정. HallDetail/Reservation과 동일 cmno 참조, 값만 저장(JPA 관계 없음)
    private Long cmno;

    // 계약금 / 잔금
    private String paymentType;

    private int amount;

    // 대기 / 완료 / 취소
    @Builder.Default
    private String status = "대기";

    // 납부 기한 (추후 FCM 알림 기준)
    private LocalDate dueDate;

    // Iamport 결제 고유번호 - PG 연동 전까지는 비어있음
    private String impUid;

    public void changePaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public void changeAmount(int amount) {
        this.amount = amount;
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    public void changeDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void changeImpUid(String impUid) {
        this.impUid = impUid;
    }

}
