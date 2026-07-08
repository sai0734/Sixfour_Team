package com.wedding.member.domain;

import java.time.LocalDate;
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
@Table(name = "tbl_member_detail")
public class MemberDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "email", unique = true, nullable = false)
  private Member member;

  private String name;

  private String phone;

  private LocalDate birthDate;

  private String zipCode;

  private String address;

  private String addressDetail;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  public void changeName(String name) {
    this.name = name;
  }

  public void changePhone(String phone) {
    this.phone = phone;
  }

  public void changeBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
  }

  public void changeAddress(String zipCode, String address, String addressDetail) {
    this.zipCode = zipCode;
    this.address = address;
    this.addressDetail = addressDetail;
  }

  // 회원탈퇴 시 개인정보 파기(익명화) - 주문/리뷰 등에서 참조하는 email(Member)은 남기되
  // 실명/연락처/주소 같은 진짜 개인정보만 지움
  public void anonymize() {
    this.name = null;
    this.phone = null;
    this.birthDate = null;
    this.zipCode = null;
    this.address = null;
    this.addressDetail = null;
  }

}