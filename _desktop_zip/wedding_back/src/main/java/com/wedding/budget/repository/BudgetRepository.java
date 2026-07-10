package com.wedding.budget.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.budget.domain.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // 특정 회원의 예산 항목 전체 조회 (sortOrder 순 - 카테고리 내 순서 유지)
    List<Budget> findByMemberEmailOrderBySortOrderAsc(String memberEmail);

}
