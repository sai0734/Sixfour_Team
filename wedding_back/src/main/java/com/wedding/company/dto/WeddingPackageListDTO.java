package com.wedding.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class WeddingPackageListDTO {
    private Long weddingPackageId;
    private String name;
    private String description;
    private BigDecimal totalPrice;
    private BigDecimal salePrice;
    private String thumbnail;
    private int itemCount;
    private LocalDateTime regDate;
}
