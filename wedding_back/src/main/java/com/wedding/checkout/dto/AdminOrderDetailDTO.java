package com.wedding.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminOrderDetailDTO {

    private Long ono;

    private String orderNumber;

    private String memberEmail;

    private String receiverName;

    private String receiverPhone;

    private String zipcode;

    private String address;

    private String addressDetail;

    private String request;

    private String trackingNo;

    private String adminMemo;

    private int totalPrice;

    private int shippingFee;

    private String orderStatus;

    private LocalDateTime regDate;

    // 결제 정보
    private String payMethod;

    private String pgProvider;

    private String pgTid;

    private String payStatus;

    private LocalDateTime approvedAt;

    private List<OrderItemDTO> items;

    private String exchangeReturnType;

    private String exchangeReturnReason;

    private String exchangeReturnDetail;

    private LocalDateTime exchangeReturnRequestedAt;

}