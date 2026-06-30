package com.wedding.checkout.domain;

import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductOption;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "tbl_order_item")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"orders", "product", "productOption"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ono", nullable = false)
    private Orders orders;

    @ManyToOne
    @JoinColumn(name = "pno", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption productOption;

    @Column(nullable = false)
    private String pnameSnapshot;

    @Column(nullable = false)
    private int priceSnapshot;

    @Column(nullable = false)
    private int qty;

    public void setOrders(Orders orders) {
        this.orders = orders;
    }

}
