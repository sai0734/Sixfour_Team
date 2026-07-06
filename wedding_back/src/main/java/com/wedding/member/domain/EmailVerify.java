package com.wedding.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

// 참고: 엑셀 설계상 email은 "FK -> Member.email"이었으나,
// 회원가입을 "인증 완료 후에만 DB에 실제 저장"하는 방식으로 바꾸면서
// 이 시점엔 아직 Member가 존재하지 않아 FK 대신 일반 문자열 컬럼으로 변경함.
// payload 컬럼도 엑셀 스펙엔 없던 것으로, 인증 전까지 가입 정보를
// 임시 보관하기 위해 추가함 (인증 완료 시 이 값을 읽어 Member 등을 생성).
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tbl_email_verify")
public class EmailVerify {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Column(unique = true, nullable = false)
  private String token;

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0", nullable = false)
  private boolean isVerified = false;

  @Column(nullable = false)
  private LocalDateTime expiredAt;

  // 인증 전까지 임시 보관하는 가입 신청 데이터 (JSON)
  @Lob
  @Column(columnDefinition = "LONGTEXT", nullable = false)
  private String payload;

  @CreatedDate
  @Column(name = "reg_date", updatable = false)
  private LocalDateTime regDate;

  public void verify() {
    this.isVerified = true;
  }

}