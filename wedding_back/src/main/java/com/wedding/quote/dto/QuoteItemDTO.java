package com.wedding.quote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteItemDTO {

    private String name;
    private Long price;              // nullable - 사진에서 못 읽으면 null
    private Boolean includedInTotal; // nullable - 총액 포함 여부를 AI가 확신 못 하면 null
}
