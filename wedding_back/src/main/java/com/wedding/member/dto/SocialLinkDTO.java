package com.wedding.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialLinkDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String kakaoAccessToken;

}