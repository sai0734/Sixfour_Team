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
@ToString (exclude = "memberRoleList")
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
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private List<MemberRole> memberRoleList = new ArrayList<>();

  // 관리자 회원관리 - 활동 정지(블랙리스트) 사유. status가 BLACKLIST가 아니면 의미 없음
  private String suspendReason;

  // 관리자 회원관리 - 정지 만료 시각. null이면 영구 정지
  private LocalDateTime suspendUntil;

  // 관리자 회원관리 - 최근 로그인 시각. 휴면 전환 대상 판별에 사용
  private LocalDateTime lastLoginAt;

  public void addRole(MemberRole memberRole){

    memberRoleList.add(memberRole);
  }

  public void clearRole(){
    memberRoleList.clear();
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }

  public void changePw(String pw){
    this.pw = pw;
  }

  public void changeSocial(boolean social) {
    this.social = social;
  }

  public void changeStatus(String status) {
    this.status = status;
  }

  public void verifyEmail() {
    this.emailVerified = true;
  }

  // 활동 정지(블랙리스트) 처리. until이 null이면 영구 정지
  public void suspend(String reason, LocalDateTime until) {
    this.status = "BLACKLIST";
    this.suspendReason = reason;
    this.suspendUntil = until;
  }

  // 정지/휴면 해제 -> 정상(ACTIVE) 전환
  public void reactivate() {
    this.status = "ACTIVE";
    this.suspendReason = null;
    this.suspendUntil = null;
  }

  // 장기 미접속 회원 휴면 전환
  public void markDormant() {
    this.status = "DORMANT";
    this.suspendReason = null;
    this.suspendUntil = null;
  }

  public void touchLogin() {
    this.lastLoginAt = LocalDateTime.now();
  }

  // 회원탈퇴 - 로그인 불가능한 값으로 덮어씀 (진짜 DELETE는 주문/리뷰 등 참조 무결성 때문에 위험해서 안 함)
  public void withdraw() {
    this.status = "WITHDRAWN";
    this.pw = "WITHDRAWN_" + java.util.UUID.randomUUID(); // 어차피 로그인 시도 자체를 status로 막지만, 이중 방어로 무의미한 값으로 덮어씀
    this.nickname = "탈퇴회원" + java.util.UUID.randomUUID().toString().substring(0, 8); // nickname unique 제약 때문에 고유값 필요
    this.suspendReason = null;
    this.suspendUntil = null;
  }

}