package com.wedding.coupleprofile.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_couple_profile")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoupleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    // 1인당 1개 (WeddingPlan과 동일하게 1:1)
    @Column(name = "member_email", unique = true, nullable = false)
    private String memberEmail;

    // 예산 최솟값/최댓값 (만원 단위)
    private Integer budgetMin;

    private Integer budgetMax;

    private String region;

    // 소규모/하객형/야외 등
    private String weddingStyle;

    private LocalDate weddingDate;

    @Lob
    private String bio;

    public void changeBudgetMin(Integer budgetMin) {
        this.budgetMin = budgetMin;
    }

    public void changeBudgetMax(Integer budgetMax) {
        this.budgetMax = budgetMax;
    }

    public void changeRegion(String region) {
        this.region = region;
    }

    public void changeWeddingStyle(String weddingStyle) {
        this.weddingStyle = weddingStyle;
    }

    public void changeWeddingDate(LocalDate weddingDate) {
        this.weddingDate = weddingDate;
    }

    public void changeBio(String bio) {
        this.bio = bio;
    }

}
