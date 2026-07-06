package com.wedding.checkout.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConfirmRequestDTO {

    private String paymentKey;

    private String orderNumber;

    private int amount;

    private List<Long> cinos;   // 결제 성공 후 장바구니에서 지울 아이템 번호들

}