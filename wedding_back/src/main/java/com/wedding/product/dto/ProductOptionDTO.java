package com.wedding.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductOptionDTO {

    private Long pono;

    private Long pno;

    private String optionName;

    private String optionValue;

    private int extraPrice;

}
