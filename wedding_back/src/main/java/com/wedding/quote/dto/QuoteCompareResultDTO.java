package com.wedding.quote.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 견적서 비교 결과 - "어느 쪽이 더 좋다"는 절대 담지 않는다. 가격차/항목차/조건차/공통점/
// 확인할 질문까지만 - 전부 사실 나열이지 우열 판단이 아니다(프롬프트에서도 명시적으로 금지).
// comparisonId/regDate는 이 비교가 기록(QuoteComparison)으로 저장된 값 - 방금 새로 비교한
// 직후 응답에도, 나중에 기록 목록/기록 하나를 다시 조회할 때도 항상 같이 채워서 내려준다.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteCompareResultDTO {

    private Long comparisonId;
    private LocalDateTime regDate;

    private QuoteDTO quoteA;
    private QuoteDTO quoteB;

    private String priceDifference;          // nullable - 둘 다 totalPrice 없으면 null
    private List<String> onlyInA;
    private List<String> onlyInB;
    private List<String> conditionDifferences;
    private List<String> commonNotes;        // 두 견적서 모두 해당하는 특징/누락 정보
    private List<String> suggestedQuestions; // 업체에 확인해야 할 질문
}
