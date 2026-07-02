package com.wedding.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishDTO {

    private Long wno;

    private String memberEmail;

    private Long pno;

    private String pname;

    private int price;

    private String thumbnail;

}
