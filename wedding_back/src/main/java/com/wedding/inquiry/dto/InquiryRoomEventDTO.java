package com.wedding.inquiry.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// /topic/inquiries/room/{roomId}로 나가는 실시간 이벤트 봉투.
// 새 메시지 도착(MESSAGE)과 상대방의 읽음 처리(READ)를 같은 토픽으로 구분해서 보낸다.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryRoomEventDTO {

    private String type; // "MESSAGE" | "READ"

    private InquiryMessageDTO message; // type=MESSAGE일 때만 채워짐

    private String readerEmail; // type=READ일 때만 채워짐

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime readAt; // type=READ일 때만 채워짐

    public static InquiryRoomEventDTO message(InquiryMessageDTO message) {
        return InquiryRoomEventDTO.builder().type("MESSAGE").message(message).build();
    }

    public static InquiryRoomEventDTO read(String readerEmail, LocalDateTime readAt) {
        return InquiryRoomEventDTO.builder()
                .type("READ")
                .readerEmail(readerEmail)
                .readAt(readAt)
                .build();
    }
}
