package com.wedding.member.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDetailDTO {

    private String email;

    private String name;

    private String phone;

    private LocalDate birthDate;

    private String zipCode;

    private String address;

    private String addressDetail;

}