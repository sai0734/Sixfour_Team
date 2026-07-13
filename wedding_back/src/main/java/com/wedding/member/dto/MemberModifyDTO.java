package com.wedding.member.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MemberModifyDTO {

    private String email;

    // 비밀번호를 안 바꾸는 경우 빈 문자열로 오므로, 빈 값은 통과시키고
    // 값이 있을 때만 규칙(영문+숫자+특수문자 8자 이상) 검증
    @Pattern(
            regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상이어야 합니다."
    )
    private String pw;

    private String nickname;
}