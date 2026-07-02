package com.wedding.checklist.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.checklist.domain.Checklist;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    // 특정 회원의 체크리스트 전체를 단계 -> sortOrder 순으로 조회 (화면에서 단계별 그룹핑용)
    List<Checklist> findByMemberEmailOrderByStageAscSortOrderAsc(String memberEmail);

}
