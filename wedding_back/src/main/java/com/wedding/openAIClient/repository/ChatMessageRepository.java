package com.wedding.openAIClient.repository;

import com.wedding.openAIClient.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 회원의 최근 대화를 최신순으로 N개 조회 (문맥 구성 + 보관 개수 판단에 같이 사용)
    @Query("select c from ChatMessage c where c.memberEmail = :memberEmail order by c.cno desc")
    List<ChatMessage> findRecentByMember(@Param("memberEmail") String memberEmail, Pageable pageable);

    // 특정 cno보다 오래된(작은) 메시지 삭제 (최근 N개 초과분 정리용)
    @Modifying(clearAutomatically = true)
    @Query("delete from ChatMessage c where c.memberEmail = :memberEmail and c.cno < :cno")
    void deleteOlderThan(@Param("memberEmail") String memberEmail, @Param("cno") Long cno);

}
