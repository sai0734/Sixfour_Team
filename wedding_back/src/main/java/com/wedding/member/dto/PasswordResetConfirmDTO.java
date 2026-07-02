package com.wedding.member.dto;

import lombok.Data;

@Data
public class PasswordResetConfirmDTO {

  private String token;

  private String newPw;

}