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

    // 안읽은 메시지가 있는지 여부
    private boolean unread;

    // Entity → DTO 변환 (Service에서 사용)
    // Entity → DTO 변환 (viewerEmail 기준으로 unread 계산 — 회원 화면/매니저 화면 공용)
    public static InquiryRoomDTO from(InquiryRoom room, String viewerEmail) {
        boolean isMemberView = room.getMemberEmail().equals(viewerEmail);
        LocalDateTime lastReadAt = isMemberView
                ? room.getMemberLastReadAt()
                : room.getManagerLastReadAt();
        boolean unread = room.getLastMessageAt() != null
                && (lastReadAt == null || room.getLastMessageAt().isAfter(lastReadAt));

        return InquiryRoomDTO.builder()
                .roomId(room.getRoomId())
                .memberEmail(room.getMemberEmail())
                .cmno(room.getCmno())
                .status(room.getStatus())
                .lastMessageAt(room.getLastMessageAt())
                .regDate(room.getRegDate())
                .unread(unread)
                .build();
    }
}