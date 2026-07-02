package com.wedding.member.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "member")
@Table(name = "tbl_login_fail")
public class LoginFail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "email", unique = true, nullable = false)
  private Member member;

  @Builder.Default
  @Column(nullable = false)
  private int failCount = 0;

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0", nullable = false)
  private boolean locked = false;

  private LocalDateTime lockedAt;

  public void increaseFail() {
    this.failCount++;
  }

  public void lock() {
    this.locked = true;
    this.lockedAt = LocalDateTime.now();
  }

  public void reset() {
    this.failCount = 0;
    this.locked = false;
    this.lockedAt = null;
  }

}