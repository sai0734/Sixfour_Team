package com.wedding.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberRoleUpdateDTO {

    // "ADMIN" 또는 "USER"
    @NotBlank
    private String role;

}
