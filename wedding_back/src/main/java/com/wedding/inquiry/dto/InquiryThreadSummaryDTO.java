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
public class InquiryThreadSummaryDTO {

    private String memberEmail;

    private String lastMessage;

    private LocalDateTime lastMessageAt;

}