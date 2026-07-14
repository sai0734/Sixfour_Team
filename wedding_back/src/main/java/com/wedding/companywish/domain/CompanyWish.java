package com.wedding.companywish.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_company_wish")
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

    // 재원 추가 - 홀/드레스/메이크업처럼 업체 안에 여러 옵션(아이템/패키지)이 있는 경우
    // 어떤 옵션을 찜했는지 저장. 옵션이 없는 업체(예: 스튜디오, 옵션 데이터 없음)는 빈 문자열("")로 저장.
    // NULL을 쓰지 않는 이유: DB 유니크 제약에서 NULL은 서로 달라도 중복으로 안 잡혀서
    // (member_email, cmno, option_name) 조합에 빈 문자열을 "옵션 없음"으로 명시적으로 고정함.
    @Builder.Default
    @Column(name = "option_name")
    private String optionName = "";

    @Column(name = "reg_date", updatable = false)
    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

}
