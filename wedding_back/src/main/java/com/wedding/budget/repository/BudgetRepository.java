package com.wedding.budget.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.budget.domain.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // 특정 회원의 예산 항목 전체 조회 (Checklist와 동일하게 페이징 없음 - 카테고리 5개 내외라 목록이 적음)
    List<Budget> findByMemberEmail(String memberEmail);

}
