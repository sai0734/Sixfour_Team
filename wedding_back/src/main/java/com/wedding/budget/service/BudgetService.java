package com.wedding.budget.service;

import java.util.List;

import com.wedding.budget.dto.BudgetDTO;

public interface BudgetService {

    Long register(BudgetDTO budgetDTO);

    void modify(BudgetDTO budgetDTO, String requesterEmail);

    void remove(Long budgetId, String requesterEmail);

    // 페이징 없이 회원의 예산 항목 전체를 가져오는 용도
    List<BudgetDTO> listByMember(String memberEmail);

}
