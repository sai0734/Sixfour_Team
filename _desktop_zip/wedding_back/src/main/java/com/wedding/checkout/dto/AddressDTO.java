package com.wedding.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO {

    private String receiverName;

    private String receiverPhone;

    private String zipcode;

    private String address;

    private String addressDetail;

}