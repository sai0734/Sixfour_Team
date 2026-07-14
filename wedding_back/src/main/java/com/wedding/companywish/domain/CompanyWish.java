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

    // 재원 추가 - 옵션의 실제 가격/이미지를 찜 시점에 같이 저장.
    // (마이페이지에서 "마제스틱 볼룸 2,500만원"처럼 옵션 자체의 가격/이미지를 보여줘야 하는데,
    //  업체 대표가격(priceAvg)/대표이미지만으로는 어떤 옵션을 찜했는지와 안 맞을 수 있어서.
    //  옵션이 없는 업체는 0 / null로 저장하고, 조회 시 업체 대표가격/이미지로 대체 표시함)
    @Builder.Default
    @Column(name = "option_amount")
    private Integer optionAmount = 0;

    @Column(name = "option_image")
    private String optionImage;

    public void changeOptionInfo(Integer optionAmount, String optionImage) {
        this.optionAmount = optionAmount;
        this.optionImage = optionImage;
    }

    @Column(name = "reg_date", updatable = false)
    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

}
