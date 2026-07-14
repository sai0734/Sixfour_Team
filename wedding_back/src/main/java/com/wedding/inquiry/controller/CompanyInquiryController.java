package com.wedding.inquiry.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryThreadSummaryDTO;
import com.wedding.inquiry.service.CompanyInquiryService;
import com.wedding.member.dto.MemberDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 보안 수정: "누구 대신 보내는지/누구 문의함을 보는지"를 예전엔 프론트가 보내주는
// email 파라미터를 그냥 믿었었음(로그인만 되어있으면 아무 이메일이나 넣어서 요청 가능했음).
// 이제 전부 @AuthenticationPrincipal로 "실제 로그인한 사람"의 이메일만 쓰도록 고침 -
// 클라이언트가 이메일을 조작해서 남의 문의함을 보거나 남 대신 답장을 보낼 수 없음
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/inquiry")
public class CompanyInquiryController {

    private final CompanyInquiryService inquiryService;

    // 일반회원 - 업체에 문의 메시지 전송 (memberEmail은 로그인한 본인으로 고정)
    @PostMapping("/{cmno}/message")
    public Map<String, String> sendMessage(
            @PathVariable Long cmno,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal MemberDTO memberDTO) {
        inquiryService.sendMessageFromMember(cmno, memberDTO.getEmail(), body.get("content"));
        return Map.of("RESULT", "SUCCESS");
    }

    // 일반회원 - 본인이 그 업체와 나눈 대화 전체 조회 (본인 것만 조회 가능)
    @GetMapping("/{cmno}/messages")
    public List<InquiryMessageDTO> getMyThread(
            @PathVariable Long cmno, @AuthenticationPrincipal MemberDTO memberDTO) {
        return inquiryService.getThread(cmno, memberDTO.getEmail());
    }

    // 담당자 - 자기 업체로 들어온 문의자 목록 (managerEmail은 로그인한 본인으로 고정)
    @GetMapping("/manager/threads")
    public List<InquiryThreadSummaryDTO> getManagerThreads(@AuthenticationPrincipal MemberDTO memberDTO) {
        return inquiryService.getThreadsForManager(memberDTO.getEmail());
    }

    // 담당자 - 특정 문의자와의 대화 전체 조회
    @GetMapping("/manager/threads/{memberEmail}")
    public List<InquiryMessageDTO> getManagerThread(
            @PathVariable String memberEmail, @AuthenticationPrincipal MemberDTO memberDTO) {
        return inquiryService.getThreadForManager(memberDTO.getEmail(), memberEmail);
    }

    // 담당자 - 답장 보내기 (managerEmail은 로그인한 본인으로 고정 - 이게 핵심 보안 포인트였음)
    @PostMapping("/manager/threads/{memberEmail}/reply")
    public Map<String, String> reply(
            @PathVariable String memberEmail,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal MemberDTO memberDTO) {
        inquiryService.sendReplyFromManager(memberDTO.getEmail(), memberEmail, body.get("content"));
        return Map.of("RESULT", "SUCCESS");
    }

}