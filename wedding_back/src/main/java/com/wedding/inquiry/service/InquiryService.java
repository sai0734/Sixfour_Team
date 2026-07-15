package com.wedding.inquiry.service;

import java.util.List;

import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryRoomDTO;

public interface InquiryService {

    // 문의하기 클릭 시 — 기존 방 있으면 반환, 없으면 새로 생성
    InquiryRoomDTO getOrCreateRoom(String memberEmail, Long cmno);

    // 매니저 화면 — 해당 업체(cmno)의 모든 문의방 목록 (최근 대화순)
    List<InquiryRoomDTO> listRoomsByCompany(Long cmno, String callerEmail);

    // 채팅창 열 때 / 폴링할 때 — 특정 방의 메시지 목록 (시간순)
    List<InquiryMessageDTO> getMessages(Long roomId, String callerEmail);

    // 메시지 전송 — 저장 + 방의 lastMessageAt 갱신
    InquiryMessageDTO sendMessage(Long roomId, String senderEmail, String content);

    // 회원 화면 — 내가 연 모든 문의방 목록 (안읽음 뱃지 폴링용)
    List<InquiryRoomDTO> listRoomsByMember(String callerEmail);
}
