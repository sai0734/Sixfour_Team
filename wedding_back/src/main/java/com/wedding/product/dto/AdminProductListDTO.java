package com.wedding.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminProductListDTO {

    private Long pno;

    private String pname;

    private String thumbnail;

    private String category;

    private int price;

    private int stockQty;

    private boolean delFlag;

    private String saleStatus; // ON_SALE(판매중) / SOLD_OUT(품절) / HIDDEN(숨김)

    private boolean lowStock;  // 재고 임계치 이하 여부

}