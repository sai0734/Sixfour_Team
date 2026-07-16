package com.wedding.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class WeddingPackageDTO {
    private Long weddingPackageId;
    private String name;
    private String description;
    private BigDecimal totalPrice;
    private BigDecimal salePrice;
    private String thumbnail;
    private LocalDateTime regDate;
    @Builder.Default
    private List<WeddingPackageItemDTO> items = new ArrayList<>();

}
