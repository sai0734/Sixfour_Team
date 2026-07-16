package com.wedding.inquiry.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wedding.inquiry.domain.InquiryMessage;
import com.wedding.inquiry.domain.InquiryRoom;

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

    // 이 메시지를 받는 쪽(보낸 사람이 아닌 상대방)이 이미 읽었는지 여부
    private boolean readByRecipient;

    // Entity → DTO 변환 (room의 읽음 시각을 기준으로 readByRecipient를 계산한다)
    public static InquiryMessageDTO from(InquiryMessage message, InquiryRoom room) {
        boolean senderIsMember = message.getSenderEmail().equals(room.getMemberEmail());
        LocalDateTime recipientReadAt = senderIsMember
                ? room.getManagerLastReadAt()
                : room.getMemberLastReadAt();
        boolean readByRecipient = recipientReadAt != null
                && !recipientReadAt.isBefore(message.getRegDate());

        return InquiryMessageDTO.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderEmail(message.getSenderEmail())
                .content(message.getContent())
                .regDate(message.getRegDate())
                .readByRecipient(readByRecipient)
                .build();
    }
}