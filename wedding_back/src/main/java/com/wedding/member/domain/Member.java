package com.wedding.member.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;
import com.wedding.global.domain.BaseTimeEntity;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "memberRoleList")
public class Member extends BaseTimeEntity {

  @Id
  private String email;

  private String pw;

  @Column(unique = true)
  private String nickname;

  private boolean social;

  @Builder.Default
  @Column(columnDefinition = "varchar(20) default 'ACTIVE'")
  private String status = "ACTIVE";

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0")
  private boolean emailVerified = false;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
          name = "member_member_role_list",
          joinColumns = @JoinColumn(name = "member_email")
  )
  @Column(name = "member_role_list")
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private List<MemberRole> memberRoleList = new ArrayList<>();

  private String suspendReason;
  private LocalDateTime suspendUntil;
  private LocalDateTime lastLoginAt;

  public void addRole(MemberRole memberRole) {
    if (!this.memberRoleList.contains(memberRole)) {
      this.memberRoleList.add(memberRole);
    }
  }

  public void clearRole() {
    this.memberRoleList.clear();
  }

  public void removeRole(MemberRole memberRole) {
    this.memberRoleList.remove(memberRole);
  }

  // 기타 기존 메서드들...
  public void changeNickname(String nickname) { this.nickname = nickname; }
  public void changePw(String pw) { this.pw = pw; }
  public void changeSocial(boolean social) { this.social = social; }
  public void changeStatus(String status) { this.status = status; }
  public void verifyEmail() { this.emailVerified = true; }
  public void suspend(String reason, LocalDateTime until) {
    this.status = "BLACKLIST";
    this.suspendReason = reason;
    this.suspendUntil = until;
  }
  public void reactivate() {
    this.status = "ACTIVE";
    this.suspendReason = null;
    this.suspendUntil = null;
  }
  public void markDormant() {
    this.status = "DORMANT";
    this.suspendReason = null;
    this.suspendUntil = null;
  }
  public void touchLogin() { this.lastLoginAt = LocalDateTime.now(); }
  public void withdraw() {
    this.status = "WITHDRAWN";
    this.pw = "WITHDRAWN_" + java.util.UUID.randomUUID();
    this.nickname = "탈퇴회원" + java.util.UUID.randomUUID().toString().substring(0, 8);
  }
}