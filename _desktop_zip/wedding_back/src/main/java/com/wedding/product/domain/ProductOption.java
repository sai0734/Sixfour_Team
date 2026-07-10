package com.wedding.product.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_product_option")
@Getter
@Builder
@ToString(exclude = "product")
@AllArgsConstructor
@NoArgsConstructor
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_pno", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String optionName;

    @Column(nullable = false)
    private String optionValue;

    @Column(columnDefinition = "int default 0")
    private int extraPrice;

    public void changeOptionName(String optionName) {
        this.optionName = optionName;
    }

    public void changeOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public void changeOptionExtraPrice(int optionExtraPrice) {
        this.extraPrice = optionExtraPrice;
    }

}
