package com.wedding.inquiry.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.inquiry.domain.CompanyInquiryMessage;

public interface CompanyInquiryMessageRepository extends JpaRepository<CompanyInquiryMessage, Long> {

    List<CompanyInquiryMessage> findByCompany_CmnoAndMemberEmailOrderByRegDateAsc(Long cmno, String memberEmail);

    // 그 업체에 문의를 건 적 있는 회원 이메일 목록 (최근 메시지가 있는 순서로)
    @Query("""
      select m.memberEmail from CompanyInquiryMessage m
      where m.company.cmno = :cmno
      group by m.memberEmail
      order by max(m.regDate) desc
      """)
    List<String> findDistinctMemberEmailsByCompany(@Param("cmno") Long cmno);

}