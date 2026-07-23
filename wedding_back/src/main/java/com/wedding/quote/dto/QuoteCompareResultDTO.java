package com.wedding.quote.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 견적서 비교 결과 - "어느 쪽이 더 좋다"는 절대 담지 않는다. 가격차/항목차/조건차/공통점/
// 확인할 질문까지만 - 전부 사실 나열이지 우열 판단이 아니다(프롬프트에서도 명시적으로 금지).
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteCompareResultDTO {

    private QuoteDTO quoteA;
    private QuoteDTO quoteB;

    private String priceDifference;          // nullable - 둘 다 totalPrice 없으면 null
    private List<String> onlyInA;
    private List<String> onlyInB;
    private List<String> conditionDifferences;
    private List<String> commonNotes;        // 두 견적서 모두 해당하는 특징/누락 정보
    private List<String> suggestedQuestions; // 업체에 확인해야 할 질문
}
