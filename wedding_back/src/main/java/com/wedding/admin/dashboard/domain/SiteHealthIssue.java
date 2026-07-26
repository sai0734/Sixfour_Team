package com.wedding.admin.dashboard.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_site_health_issue")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiteHealthIssue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pageUrl;

    // IMAGE_BROKEN / CONSOLE_ERROR / PAGE_ERROR
    @Column(nullable = false)
    private String issueType;

    // IMAGE_BROKEN일 때 대상 상품 pno - 있으면 detail을 서버가 고정된 문구로 조합해서
    // AI가 쓴 문장을 신뢰하지 않고, 한글 인코딩도 DB에서 바로 읽어와 깨지지 않게 한다
    private Long productPno;

    @Column(length = 1000)
    private String detail;

    @Column(columnDefinition = "boolean default false")
    private boolean resolved;

    public void resolve() {
        this.resolved = true;
    }
}
