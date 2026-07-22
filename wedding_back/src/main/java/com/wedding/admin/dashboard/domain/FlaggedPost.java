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

    // 명백한 금칙어가 아니라, OpenClaw가 뉘앙스상 의심된다고 판단한 사유
    @Column(length = 1000, nullable = false)
    private String reason;

    @Column(columnDefinition = "boolean default false")
    private boolean resolved;

    public void resolve() {
        this.resolved = true;
    }
}
