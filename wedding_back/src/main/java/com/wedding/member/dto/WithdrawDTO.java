package com.wedding.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WithdrawDTO {

    @NotBlank
    private String email;

}