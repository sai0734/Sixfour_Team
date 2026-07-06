package com.wedding.member.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminMemberDTO {

    private String email;

    private String nickname;


    private LocalDateTime regDate;


    private LocalDateTime lastLoginAt;


    private String status;


    private String suspendReason;


    private LocalDateTime suspendUntil;

    private boolean emailVerified;


    private boolean admin;

}