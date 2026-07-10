package com.wedding.checklist.dto;

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
public class ChecklistDTO {

    private Long checklistId;

    private String memberEmail;

    private String title;

    private boolean isDone;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private int sortOrder;

    // 준비 단계 (1/2/3 - 이름표는 프론트 고정 매핑)
    private int stage;
}
