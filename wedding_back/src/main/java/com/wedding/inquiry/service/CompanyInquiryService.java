package com.wedding.inquiry.service;

import java.util.List;

import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryThreadSummaryDTO;

public interface CompanyInquiryService {

    // 일반회원이 업체에 메시지 보내기
    void sendMessageFromMember(Long cmno, String memberEmail, String content);

    // 담당자가 특정 회원에게 답장 보내기 (담당 업체는 managerEmail로 자동 결정됨)
    void sendReplyFromManager(String managerEmail, String targetMemberEmail, String content);

    // 일반회원 - 본인이 특정 업체와 나눈 대화 전체
    List<InquiryMessageDTO> getThread(Long cmno, String memberEmail);

    // 담당자 - 자기 업체로 들어온 문의자 목록
    List<InquiryThreadSummaryDTO> getThreadsForManager(String managerEmail);

    // 담당자 - 특정 문의자와의 대화 전체
    List<InquiryMessageDTO> getThreadForManager(String managerEmail, String memberEmail);

}