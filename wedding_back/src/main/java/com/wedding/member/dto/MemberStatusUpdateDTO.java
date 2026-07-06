package com.wedding.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberStatusUpdateDTO {


    @NotBlank
    private String status;


    private String reason;


    private Integer suspendDays;

}