package com.wedding.weddingplan.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_wedding_plan", indexes = {
        @Index(columnList = "member_email", name = "idx_weddingplan_member", unique = true)
})
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeddingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long weddingPlanId;

    // Member.email을 FK로 참조, 1:1 관계라 unique 제약
    @Column(name = "member_email", unique = true, nullable = false)
    private String memberEmail;

    private String groomName;

    private String brideName;

    private LocalDate weddingDate;

    private String weddingLocation;

    private Long totalBudget;

    private String memo;

    public void changeGroomName(String groomName) {
        this.groomName = groomName;
    }

    public void changeBrideName(String brideName) {
        this.brideName = brideName;
    }

    public void changeWeddingDate(LocalDate weddingDate) {
        this.weddingDate = weddingDate;
    }

    public void changeWeddingLocation(String weddingLocation) {
        this.weddingLocation = weddingLocation;
    }

    public void changeTotalBudget(Long totalBudget) {
        this.totalBudget = totalBudget;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

}
