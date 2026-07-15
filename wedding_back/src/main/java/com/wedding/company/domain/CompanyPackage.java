package com.wedding.company.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// 홀+드레스+스튜디오+메이크업(스드메) 4개 업체를 묶은 패키지 상품.
// 관리자 "업체 리스트" 화면의 "패키지" 탭에서 조회/관리됨.
@Entity
@Table(name = "tbl_company_package")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
public class CompanyPackage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pno;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_cmno", nullable = false)
    private Company hallCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dress_cmno", nullable = false)
    private Company dressCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_cmno", nullable = false)
    private Company studioCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_cmno", nullable = false)
    private Company makeupCompany;

    // 홀+드레스+스튜디오+메이크업 개별 평균가 합산액 (할인 전 정가)
    private BigDecimal totalPrice;

    // 할인율 (0.10~0.20 = 10%~20%)
    private Double discountRate;

    // 패키지 판매가 = totalPrice * (1 - discountRate)
    private BigDecimal packagePrice;

    // 4개 업체 간 이동거리 합(km). 값이 작을수록 동선이 편한 패키지
    private Double distanceKm;

    // 누적 구매건수 (인기순 정렬, "N건 구매" 뱃지 등에 사용)
    @Column(columnDefinition = "integer default 0")
    private int purchaseCount;

    @Column(columnDefinition = "boolean default false")
    private boolean delFlag;

    public void changeDelFlag(boolean delFlag) {
        this.delFlag = delFlag;
    }

    public void change(String name, String description, BigDecimal totalPrice, Double discountRate,
                       BigDecimal packagePrice) {
        this.name = name;
        this.description = description;
        this.totalPrice = totalPrice;
        this.discountRate = discountRate;
        this.packagePrice = packagePrice;
    }

    public void increasePurchaseCount() {
        this.purchaseCount++;
    }
}