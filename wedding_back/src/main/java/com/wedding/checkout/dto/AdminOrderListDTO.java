package com.wedding.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminOrderListDTO {

    private Long ono;

    private String orderNumber;

    private String memberEmail;

    private String receiverName;

    private int totalPrice;

    private String orderStatus;

    private LocalDateTime regDate;

}