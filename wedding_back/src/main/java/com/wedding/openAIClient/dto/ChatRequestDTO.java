package com.wedding.openAIClient.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequestDTO {

    private String message;

    // 버튼으로 미리 고정한 의도 - null이면 기존처럼 AI가 여러 함수 중 자유 판단
    private String intent;

    // "업체 찾기"에서 버튼으로 고정한 카테고리(HALL/DRESS/STUDIO/MAKEUP) - null이면 미고정
    private String companyCategory;
}
