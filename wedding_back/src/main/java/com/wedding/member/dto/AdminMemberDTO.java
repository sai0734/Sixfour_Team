package com.wedding.member.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 관리자 - 회원 목록/상세용 DTO
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminMemberDTO {

    private String email;

    private String nickname;

    // 가입일
    private LocalDateTime regDate;

    // 최근 로그인 시각 (없으면 미접속)
    private LocalDateTime lastLoginAt;

    // ACTIVE / BLACKLIST / DORMANT
    private String status;

    // status가 BLACKLIST일 때만 값 존재
    private String suspendReason;

    // 정지 만료 시각. null이면 영구 정지 (status가 BLACKLIST일 때만 의미 있음)
    private LocalDateTime suspendUntil;

    private boolean emailVerified;

}