package com.wedding.checkout.dto;

import com.wedding.global.dto.PageRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminOrderSearchDTO extends PageRequestDTO {

    private String keyword;     // 주문번호 또는 주문자명

    private String status;      // PAID / SHIPPING_READY / SHIPPING / DELIVERED / REFUNDED / CANCELLED / null(전체)

    private String sortType;    // latest(기본) / oldest

}