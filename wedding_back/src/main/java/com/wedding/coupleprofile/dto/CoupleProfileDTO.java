package com.wedding.coupleprofile.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoupleProfileDTO {

    private Long profileId;

    private String memberEmail;

    private Integer budgetMin;

    private Integer budgetMax;

    private String region;

    private String weddingStyle;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate weddingDate;

    private String bio;
}
