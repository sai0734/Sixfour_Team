package com.wedding.inquiry.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wedding.inquiry.domain.InquiryRoom;
import com.wedding.inquiry.domain.InquiryRoomStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryRoomDTO {

    private Long roomId;

    private String memberEmail;

    private Long cmno;

    private InquiryRoomStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime regDate;

    // Entity → DTO 변환 (Service에서 사용)
    public static InquiryRoomDTO from(InquiryRoom room) {
        return InquiryRoomDTO.builder()
                .roomId(room.getRoomId())
                .memberEmail(room.getMemberEmail())
                .cmno(room.getCmno())
                .status(room.getStatus())
                .lastMessageAt(room.getLastMessageAt())
                .regDate(room.getRegDate())
                .build();
    }
}