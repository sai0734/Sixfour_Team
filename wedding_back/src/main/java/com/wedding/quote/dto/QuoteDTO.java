package com.wedding.quote.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.wedding.company.domain.CompanyCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// imageFileName은 일부러 안 담는다 - 이미지 조회는 quoteId로만 받아서(/api/quotes/{id}/image)
// 서비스가 소유권 확인 후 파일명을 알아내는 구조라, 프론트로 실제 저장 파일명이 안 새 나가게 함.
@Getter
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
