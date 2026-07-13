package com.wedding.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class JoinDTO {

  @NotBlank
  @Email
  private String email;

  @NotBlank
  @Pattern(
          regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$",
          message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상이어야 합니다."
  )
  private String pw;

  @NotBlank
  private String nickname;

  @NotBlank
  private String name;

  @NotBlank
  private String phone;

  private LocalDate birthDate;

  private String zipCode;

  @NotBlank
  private String address;

  private String addressDetail;

  private boolean termsAgree;

  private boolean privacyAgree;

  private boolean marketing;

}