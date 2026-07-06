package com.wedding.cart.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class CartItemListDTO {

    private Long cino;

    private int qty;

    private Long pno;

    private String pname;

    private int price;

    private String imageFile;

    private Long pono;

    private String optionName;

    private String optionValue;

    private int extraPrice;

    public CartItemListDTO(Long cino, int qty, Long pno, String pname, int price, String imageFile,
                           Long pono, String optionName, String optionValue, Integer extraPrice) {
        this.cino = cino;
        this.qty = qty;
        this.pno = pno;
        this.pname = pname;
        this.price = price;
        this.imageFile = imageFile;
        this.pono = pono;
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.extraPrice = extraPrice == null ? 0 : extraPrice;
    }
}