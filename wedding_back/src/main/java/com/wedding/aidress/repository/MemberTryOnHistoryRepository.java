package com.wedding.aidress.repository;

import com.wedding.aidress.domain.MemberTryOnHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTryOnHistoryRepository extends JpaRepository<MemberTryOnHistory, Long> {

  List<MemberTryOnHistory> findByMemberEmailOrderByHistoryIdDesc(String memberEmail);
}
