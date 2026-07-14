package com.wedding.inquiry.domain;

import com.wedding.company.domain.Company;
import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원과 업체 담당자 사이에 오가는 문의 메시지 1건.
// memberEmail 기준으로 스레드가 묶임 (그 회원과 그 업체 사이의 대화 = memberEmail + company 조합)
@Entity
@Table(name = "tbl_company_inquiry_message")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyInquiryMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cmno", nullable = false)
    private Company company;

    @Column(name = "member_email", nullable = false)
    private String memberEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    @Column(length = 1000, nullable = false)
    private String content;

}