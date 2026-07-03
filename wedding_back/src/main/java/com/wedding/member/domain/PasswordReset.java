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
@Table(name = "tbl_password_reset")
public class PasswordReset {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "email", nullable = false)
  private Member member;

  @Column(unique = true, nullable = false)
  private String token;

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0", nullable = false)
  private boolean used = false;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @CreatedDate
  @Column(name = "reg_date", updatable = false)
  private LocalDateTime regDate;

  public void use() {
    this.used = true;
  }

}