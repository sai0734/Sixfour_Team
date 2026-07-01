package com.wedding.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "member")
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tbl_terms_agree")
public class TermsAgree {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "email", nullable = false)
  private Member member;

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0", nullable = false)
  private boolean termsAgree = false;

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0", nullable = false)
  private boolean privacyAgree = false;

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0", nullable = false)
  private boolean marketing = false;

  @CreatedDate
  @Column(name = "reg_date", updatable = false)
  private LocalDateTime regDate;

}