package com.wedding.cart.domain;

import jakarta.persistence.*;
import lombok.*;

import com.wedding.member.domain.Member;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "member")
@Table(
  name = "tbl_cart_order",
  indexes = { @Index(name="idx_cart_email", columnList = "member_email") }
)
public class CartOrder {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cno;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="member_email", unique = true)
  private Member member;

}