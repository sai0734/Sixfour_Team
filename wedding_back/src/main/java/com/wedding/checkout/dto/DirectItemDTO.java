package com.wedding.checkout.dto;

import lombok.Data;

@Data
public class DirectItemDTO {

    private Long pno;

    private Long pono;   // 옵션 없으면 null

    private int qty;

}