// 승진 코드 추가
package com.wedding.reservation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationBulkPaymentPrepareDTO {

    private String orderNumber;
    private int totalAmount;
    private List<Long> reservationIds;
}
// 승진 코드 추가 끝
