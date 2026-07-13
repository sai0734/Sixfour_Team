package com.wedding.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordResetConfirmDTO {

  @NotBlank
  private String token;

  @NotBlank
  @Pattern(
          regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$",
          message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상이어야 합니다."
  )
  private String newPw;

}