package com.wedding.inquiry.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.inquiry.domain.InquiryMessage;

public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {

    // 특정 채팅방의 메시지 목록 조회 (시간 오름차순)
    @Query("SELECT m FROM InquiryMessage m WHERE m.roomId = :roomId ORDER BY m.regDate ASC")
    List<InquiryMessage> findByRoomIdOrderByRegDateAsc(@Param("roomId") Long roomId);

    // 채팅창 열 때 — 최신 메시지부터 최대 pageable 만큼만 (대화가 길어져도 매번 전체를 안 불러오기 위함)
    @Query("SELECT m FROM InquiryMessage m WHERE m.roomId = :roomId ORDER BY m.regDate DESC")
    List<InquiryMessage> findByRoomIdOrderByRegDateDesc(
            @Param("roomId") Long roomId, Pageable pageable);

}