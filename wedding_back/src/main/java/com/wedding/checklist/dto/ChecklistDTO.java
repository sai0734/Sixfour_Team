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

    // 예약이 결제대기로 넘어오면서 자동 생성된 항목이면 그 예약 id (프론트가 이 값이 있으면
    // 마이페이지 예약 현황으로 바로 이동할 수 있게 클릭 가능 처리함). 수동 항목은 null.
    private Long reservationId;
}
