package com.wedding.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BudgetDTO {

    private Long budgetId;

    private String memberEmail;

    private String category;

    private Long budgetAmount;

    private Long actualAmount;

    private String memo;
}
