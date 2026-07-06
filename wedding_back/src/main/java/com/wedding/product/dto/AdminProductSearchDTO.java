package com.wedding.product.dto;

import com.wedding.global.dto.PageRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminProductSearchDTO extends PageRequestDTO {

    private String keyword;
    private String category;
    private String saleStatus;  // ON_SALE / SOLD_OUT / HIDDEN / null(전체)
    private String sortType;    // latest / stockAsc / salesDesc

}