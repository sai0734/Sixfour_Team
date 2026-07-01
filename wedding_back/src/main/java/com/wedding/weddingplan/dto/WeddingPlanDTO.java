package com.wedding.weddingplan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeddingPlanDTO {

    private Long weddingPlanId;

    private String memberEmail;

    private String groomName;

    private String brideName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate weddingDate;

    private String weddingLocation;

    private Long totalBudget;

    private String memo;
}
