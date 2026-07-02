package com.wedding.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialCompleteDTO {

  @NotBlank
  private String email;

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