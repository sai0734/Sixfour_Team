package com.wedding.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// CONFIRM_LINK 상태에서, 사용자가 "예, 연동할게요"를 선택했을 때 보내는 요청
@Data
public class KakaoLinkConfirmDTO {

    @NotBlank
    private String confirmToken;

}