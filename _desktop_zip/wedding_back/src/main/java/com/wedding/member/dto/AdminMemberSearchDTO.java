package com.wedding.member.dto;

import com.wedding.global.dto.PageRequestDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminMemberSearchDTO extends PageRequestDTO {


    private String keyword;


    private String status;

}