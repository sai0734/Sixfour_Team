package com.wedding.cart.domain;

import com.wedding.product.domain.ProductOption;
import jakarta.persistence.*;
import lombok.*;

import com.wedding.product.domain.Product;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString(exclude={"cartOrder", "product", "optionId"})
@Table(name = "tbl_cart_item", indexes = {
    @Index(columnList = "cart_cno", name = "idx_cartitem_cart"),
    @Index(columnList = "product_pno, cart_cno", name="idx_cartitem_pno_cart")
})
public class CartItem {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cino;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_cno", nullable = false)
  private CartOrder cartOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_pno", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "option_id", nullable = true)
  private ProductOption optionId;

  @Column(columnDefinition = "int default 1", nullable = false)
  private int qty;

  public void changeQty(int qty){
    this.qty = qty;
  }

}