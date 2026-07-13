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
public class OrderDTO {

    private Long ono;

    private String orderNumber;

    private int totalPrice;

    private int shippingFee;

    private String receiverName;

    private String receiverPhone;

    private String zipcode;

    private String address;

    private String addressDetail;

    private String orderStatus;

    private LocalDateTime regDate;

    private String trackingNo;      // 재원 추가 - 배송조회

    private String exchangeReturnType;

    private String exchangeReturnReason;

    private String exchangeReturnDetail;

    private LocalDateTime exchangeReturnRequestedAt;

    private List<OrderItemDTO> items;

}