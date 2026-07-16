package com.wedding.inquiry.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryMessageSendDTO;
import com.wedding.inquiry.dto.InquiryRoomDTO;
import com.wedding.inquiry.service.InquiryService;
import com.wedding.member.dto.MemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    // 문의하기 클릭 — 채팅방 생성 또는 기존 방 반환
    @PostMapping("/rooms")
    public InquiryRoomDTO openRoom(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @RequestParam Long cmno) {
        log.info("InquiryController_openRoom_실행~~~~~~~~");
        return inquiryService.getOrCreateRoom(memberDTO.getEmail(), cmno);
    }

    // 매니저 화면 — 업체(cmno)별 문의방 목록
    @GetMapping("/company/{cmno}/rooms")
    public List<InquiryRoomDTO> listRoomsByCompany(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @PathVariable(name = "cmno") Long cmno) {
        log.info("InquiryController_listRoomsByCompany_실행~~~~~~~~");
        return inquiryService.listRoomsByCompany(cmno, memberDTO.getEmail());
    }

    // 채팅창 열 때 / 폴링 — 메시지 목록 조회
    @GetMapping("/rooms/{roomId}/messages")
    public List<InquiryMessageDTO> getMessages(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @PathVariable(name = "roomId") Long roomId) {
        log.info("InquiryController_getMessages_실행~~~~~~~~");
        return inquiryService.getMessages(roomId, memberDTO.getEmail());
    }

    // 메시지 전송
    @PostMapping("/rooms/{roomId}/messages")
    public InquiryMessageDTO sendMessage(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @PathVariable(name = "roomId") Long roomId,
            @RequestBody InquiryMessageSendDTO requestDTO) {
        log.info("InquiryController_sendMessage_실행~~~~~~~~");
        return inquiryService.sendMessage(
                roomId,
                memberDTO.getEmail(),
                requestDTO.getContent());
    }

    // 회원 화면 — 내가 연 모든 문의방 목록 (플로팅 버튼 안읽음 뱃지 폴링용)
    @GetMapping("/my-rooms")
    public List<InquiryRoomDTO> listMyRooms(
            @AuthenticationPrincipal MemberDTO memberDTO) {
        log.info("InquiryController_listMyRooms_실행~~~~~~~~");
        return inquiryService.listRoomsByMember(memberDTO.getEmail());
    }

    // 채팅창을 열어둔 채로 실시간 메시지를 받았을 때 — 목록을 다시 안 불러오고 읽음 시각만 갱신
    @PostMapping("/rooms/{roomId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal MemberDTO memberDTO,
            @PathVariable(name = "roomId") Long roomId) {
        log.info("InquiryController_markRead_실행~~~~~~~~");
        inquiryService.markRead(roomId, memberDTO.getEmail());
    }
}
