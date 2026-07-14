package com.wedding.inquiry.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_inquiry_message")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    // 오느 채팅방 메시지인지
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // 보낸 사람 이일
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    // 메시지 내용
    @Lob
    @Column(nullable = false)
    private String content;

}
