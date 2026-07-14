package com.wedding.inquiry.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryMessageDTO {

    private Long id;

    private String senderType; // MEMBER / MANAGER

    private String content;

    private LocalDateTime regDate;

}