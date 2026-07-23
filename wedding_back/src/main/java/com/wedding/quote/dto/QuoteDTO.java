package com.wedding.quote.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.wedding.company.domain.CompanyCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// imageFileName은 일부러 안 담는다 - 이미지 조회는 quoteId로만 받아서(/api/quotes/{id}/image)
// 서비스가 소유권 확인 후 파일명을 알아내는 구조라, 프론트로 실제 저장 파일명이 안 새 나가게 함.
// @Setter가 필요한 이유: QuoteComparison 스냅샷(JSON 문자열)을 다시 QuoteDTO로 역직렬화할 때
// Jackson이 기본적으로 public setter를 통해서만 값을 채울 수 있어서 (QuoteItemDTO와 동일한 이유).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDTO {

    private Long quoteId;
    private CompanyCategory category;
    private String vendorNameGuess;
    private Long totalPrice;
    private Long perGuestPrice;
    private List<QuoteItemDTO> items;
    private List<String> hiddenNotes;
    private LocalDateTime regDate;
}
