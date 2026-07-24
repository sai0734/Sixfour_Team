package com.wedding.admin.dashboard.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_ai_briefing")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiBriefing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예: "2026-W30" 형식
    @Column(nullable = false)
    private String weekOf;

    @Column(length = 2000, nullable = false)
    private String summaryText;

    @Column(nullable = false)
    private String pdfFileName;

    @Builder.Default
    private int lowStockCount = 0;

    @Builder.Default
    private int flaggedPostCount = 0;

    @Builder.Default
    private int siteIssueCount = 0;
}
