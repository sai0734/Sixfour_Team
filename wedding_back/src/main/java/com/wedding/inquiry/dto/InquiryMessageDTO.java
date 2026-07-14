package com.wedding.inquiry.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wedding.inquiry.domain.InquiryMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryMessageDTO {

    private Long messageId;

    private Long roomId;

    private String senderEmail;

    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime regDate;

    // Entity → DTO 변환 (Service에서 사용)
    public static InquiryMessageDTO from(InquiryMessage message) {
        return InquiryMessageDTO.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderEmail(message.getSenderEmail())
                .content(message.getContent())
                .regDate(message.getRegDate())
                .build();
    }
}