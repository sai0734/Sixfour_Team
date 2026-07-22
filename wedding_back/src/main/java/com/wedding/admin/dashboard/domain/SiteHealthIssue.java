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

    @Column(length = 1000)
    private String detail;

    @Column(columnDefinition = "boolean default false")
    private boolean resolved;

    public void resolve() {
        this.resolved = true;
    }
}
