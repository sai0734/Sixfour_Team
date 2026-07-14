package com.wedding.inquiry.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.company.domain.Company;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.inquiry.domain.CompanyInquiryMessage;
import com.wedding.inquiry.domain.SenderType;
import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryThreadSummaryDTO;
import com.wedding.inquiry.repository.CompanyInquiryMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class CompanyInquiryServiceImpl implements CompanyInquiryService {

    private final CompanyInquiryMessageRepository messageRepository;
    private final CompanyRepository companyRepository;

    @Override
    public void sendMessageFromMember(Long cmno, String memberEmail, String content) {

        Company company = companyRepository.selectOne(cmno).orElseThrow();

        messageRepository.save(CompanyInquiryMessage.builder()
                .company(company)
                .memberEmail(memberEmail)
                .senderType(SenderType.MEMBER)
                .content(content)
                .build());

        log.info("inquiry message from member: " + memberEmail + " -> company " + cmno);
    }

    @Override
    public void sendReplyFromManager(String managerEmail, String targetMemberEmail, String content) {

        Company company = companyRepository.findByManagerEmail(managerEmail)
                .orElseThrow(() -> new IllegalStateException("담당 중인 업체가 없습니다."));

        messageRepository.save(CompanyInquiryMessage.builder()
                .company(company)
                .memberEmail(targetMemberEmail)
                .senderType(SenderType.MANAGER)
                .content(content)
                .build());

        log.info("inquiry reply from manager: " + managerEmail + " -> member " + targetMemberEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InquiryMessageDTO> getThread(Long cmno, String memberEmail) {

        return messageRepository.findByCompany_CmnoAndMemberEmailOrderByRegDateAsc(cmno, memberEmail)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InquiryThreadSummaryDTO> getThreadsForManager(String managerEmail) {

        Company company = companyRepository.findByManagerEmail(managerEmail)
                .orElseThrow(() -> new IllegalStateException("담당 중인 업체가 없습니다."));

        List<String> memberEmails = messageRepository.findDistinctMemberEmailsByCompany(company.getCmno());

        return memberEmails.stream().map(email -> {

            List<CompanyInquiryMessage> thread = messageRepository
                    .findByCompany_CmnoAndMemberEmailOrderByRegDateAsc(company.getCmno(), email);

            CompanyInquiryMessage last = thread.get(thread.size() - 1);

            return InquiryThreadSummaryDTO.builder()
                    .memberEmail(email)
                    .lastMessage(last.getContent())
                    .lastMessageAt(last.getRegDate())
                    .build();

        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InquiryMessageDTO> getThreadForManager(String managerEmail, String memberEmail) {

        Company company = companyRepository.findByManagerEmail(managerEmail)
                .orElseThrow(() -> new IllegalStateException("담당 중인 업체가 없습니다."));

        return getThread(company.getCmno(), memberEmail);
    }

    private InquiryMessageDTO toDTO(CompanyInquiryMessage m) {
        return InquiryMessageDTO.builder()
                .id(m.getId())
                .senderType(m.getSenderType().name())
                .content(m.getContent())
                .regDate(m.getRegDate())
                .build();
    }

}