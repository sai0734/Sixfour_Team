package com.wedding.company.dto;

import com.wedding.company.domain.CompanyCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class WeddingPackageItemDTO {

    private Long itemId;
    private CompanyCategory category;
    private Long cmno;
    private String companyName;
    private String optionLabel;
    private BigDecimal price;
    private Integer sortOrder;
}
