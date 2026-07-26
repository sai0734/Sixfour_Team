package com.wedding.admin.dashboard.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_flagged_post")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlaggedPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long boardId;

    // SPAM / PERSONAL_ATTACK / PROFANITY / OTHER - AI가 자유 서술 대신 정해진 분류값만 고르게 해서
    // 카드 제목(배지)이 항상 같은 틀로 나오게 한다
    @Column(nullable = false)
    private String flagType;

    // 신고 시점의 게시글 제목 스냅샷 - AI가 쓴 텍스트가 아니라 서버가 board 테이블에서 직접 조회해서
    // 저장하므로 인코딩이 깨지지 않고, 나중에 게시글이 삭제돼도 제목이 남아있다
    private String boardTitle;

    // 명백한 금칙어가 아니라, OpenClaw가 뉘앙스상 의심된다고 판단한 사유
    @Column(length = 1000, nullable = false)
    private String reason;

    @Column(columnDefinition = "boolean default false")
    private boolean resolved;

    public void resolve() {
        this.resolved = true;
    }
}
