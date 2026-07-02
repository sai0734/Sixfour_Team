package com.wedding.companywish.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_company_wish", uniqueConstraints = {
        // 같은 회원이 같은 업체를 중복으로 찜하는 것 방지
        @UniqueConstraint(columnNames = {"member_email", "cmno"})
})
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyWish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wishId;

    @Column(name = "member_email")
    private String memberEmail;

    // Company(D파트) 또는 HallDetail(E파트) 미확정 - 값만 저장, JPA 관계 없음
    private Long cmno;

    @Column(name = "reg_date", updatable = false)
    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

}
