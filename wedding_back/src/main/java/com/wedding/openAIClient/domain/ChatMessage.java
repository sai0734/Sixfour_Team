package com.wedding.openAIClient.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_chat_message")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cno;

    @Column(nullable = false)
    private String memberEmail;

    // user 또는 assistant
    @Column(nullable = false)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

}
