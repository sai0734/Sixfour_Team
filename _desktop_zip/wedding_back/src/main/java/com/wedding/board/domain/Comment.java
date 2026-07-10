package com.wedding.board.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_comment")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @Column(name = "board_id")
    private Long boardId;

    private String memberEmail;

    // 작성 시점의 닉네임 스냅샷
    private String nickname;

    // null = 댓글(최상위), 값 있음 = 대댓글 (부모 댓글의 commentId)
    @Column(name = "parent_id")
    private Long parentId;

    @Lob
    private String content;

    // AI 자동답변 여부 - 지금은 항상 false, AI 기능 붙일 때 사용
    @Builder.Default
    private boolean ai = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeDeleted(boolean deleted) {
        this.deleted = deleted;
    }

}
