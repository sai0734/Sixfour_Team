package com.wedding.member.dto;

import lombok.Data;

@Data
public class JoinDTO {

  private String email;

  private String pw;

  private String nickname;

  private String name;

  private String phone;

  private boolean termsAgree;

  private boolean privacyAgree;

  private boolean marketing;

}