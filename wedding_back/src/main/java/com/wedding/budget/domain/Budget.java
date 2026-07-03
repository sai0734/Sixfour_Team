package com.wedding.budget.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_budget")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long budgetId;

    // Member.email을 FK로 참조 (Checklist와 동일하게 String으로 직접 보관)
    private String memberEmail;

    // 자유 텍스트 (홀/스드메/예복/예물/기타 등, 추후 항목 확장 가능)
    private String category;

    // 계획 예산 금액
    private Long budgetAmount;

    // 실지출 금액 (등록 시 0, 결제 완료 후 수정 API로 갱신)
    @Builder.Default
    private Long actualAmount = 0L;

    private String memo;

    // 단계 내(카테고리 내) 노출 순서
    private int sortOrder;

    public void changeCategory(String category) {
        this.category = category;
    }

    public void changeBudgetAmount(Long budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public void changeActualAmount(Long actualAmount) {
        this.actualAmount = actualAmount;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

}
