package com.wedding.inquiry.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryMessageSendDTO;
import com.wedding.inquiry.dto.InquiryRoomDTO;
import com.wedding.inquiry.service.InquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    // 문의하기 클릭 — 채팅방 생성 또는 기존 방 반환
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/rooms")
    public InquiryRoomDTO openRoom(Principal principal, @RequestParam Long cmno) {

        log.info("InquiryController_openRoom_실행~~~~~~~~");

        return inquiryService.getOrCreateRoom(principal.getName(), cmno);
    }

    // 매니저 화면 — 업체(cmno)별 문의방 목록
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/company/{cmno}/rooms")
    public List<InquiryRoomDTO> listRoomsByCompany(@PathVariable(name = "cmno") Long cmno) {

        log.info("InquiryController_listRoomsByCompany_실행~~~~~~~~");

        return inquiryService.listRoomsByCompany(cmno);
    }

    // 채팅창 열 때 / 폴링 — 메시지 목록 조회
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/rooms/{roomId}/messages")
    public List<InquiryMessageDTO> getMessages(@PathVariable(name = "roomId") Long roomId) {

        log.info("InquiryController_getMessages_실행~~~~~~~~");

        return inquiryService.getMessages(roomId);
    }

    // 메시지 전송
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/rooms/{roomId}/messages")
    public InquiryMessageDTO sendMessage(
            Principal principal,
            @PathVariable(name = "roomId") Long roomId,
            @RequestBody InquiryMessageSendDTO requestDTO) {

        log.info("InquiryController_sendMessage_실행~~~~~~~~");

        return inquiryService.sendMessage(
                roomId,
                principal.getName(),
                requestDTO.getContent()
        );
    }

}