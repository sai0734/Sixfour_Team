package com.wedding.board.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_board")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId;

    private String memberEmail;

    // FREE / REVIEW / QNA
    private String boardType;

    // Q&A 카테고리 (예산/업체/일정/기타) - FREE/REVIEW는 null
    private String category;

    private String title;

    @Lob
    private String content;

    // AI 3줄 요약 - AI 기능 붙일 때 채워짐, 지금은 필드만
    @Lob
    private String aiSummary;

    // 후기 별점 1~5 - REVIEW 타입만 사용
    private Integer rating;

    @Builder.Default
    private int viewCount = 0;

    @Builder.Default
    private int likeCount = 0;

    // 댓글+대댓글 합산 수 (CommentService에서 등록/삭제 시 동기화)
    @Builder.Default
    private int commentCount = 0;

    // 소프트 삭제 (Checklist의 isDone과 같은 Jackson 이슈 피하려고 "is" 접두사 없이 명명)
    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeCategory(String category) {
        this.category = category;
    }

    public void changeRating(Integer rating) {
        this.rating = rating;
    }

    public void changeAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public void increaseViewCount() {
        this.viewCount = this.viewCount + 1;
    }

    public void increaseLikeCount() {
        this.likeCount = this.likeCount + 1;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void increaseCommentCount() {
        this.commentCount = this.commentCount + 1;
    }

    public void decreaseCommentCount() {
        this.commentCount = Math.max(0, this.commentCount - 1);
    }

    public void changeDeleted(boolean deleted) {
        this.deleted = deleted;
    }

}
