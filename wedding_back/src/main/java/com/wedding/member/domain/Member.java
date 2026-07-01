package com.wedding.member.domain;

import jakarta.persistence.*;
import lombok.*;

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

  private String nickname;

  private boolean social;

  @Builder.Default
  @Column(columnDefinition = "varchar(20) default 'ACTIVE'")
  private String status = "ACTIVE";

  @Builder.Default
  @Column(columnDefinition = "tinyint default 0")
  private boolean emailVerified = false;

  @ElementCollection(fetch = FetchType.LAZY)
  @Builder.Default
  private List<MemberRole> memberRoleList = new ArrayList<>();

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

}