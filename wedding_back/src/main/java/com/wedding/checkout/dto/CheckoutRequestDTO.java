package com.wedding.checkout.dto;

import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequestDTO {

    private List<Long> cinos;               // 주문할 장바구니 아이템 번호들 (바로구매면 빈 리스트)

    private DirectItemDTO directItem;        // 바로구매용 단일 상품 정보 (장바구니 아니면 null)

    private String receiverName;

    private String receiverPhone;

    private String zipcode;

    private String address;

    private String addressDetail;

    private String request;

}