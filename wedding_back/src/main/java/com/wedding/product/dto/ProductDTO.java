package com.wedding.product.dto;

import com.wedding.product.domain.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {

    private Long pno;

    private String pname;

    private int price;

    private String pdesc;

    private boolean delFlag;

    private String category;

    private int stockQty;

    private double ratingAvg;

    private int reviewCount;

    @Builder.Default
    private List<ProductImage> imageList = new ArrayList<>();

    @Builder.Default
    private List<String> uploadFileNames = new ArrayList<>();

    private Long viewCount;

    private int salesCount;

}
