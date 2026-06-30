package com.wedding.cart.domain;

import jakarta.persistence.*;
import lombok.*;

import com.wedding.member.domain.Member;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "owner")
@Table(
  name = "tbl_cart", 
  indexes = { @Index(name="idx_cart_email", columnList = "member_owner") }
)
public class CartOrder {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cno;

  @OneToOne
  @JoinColumn(name="member_email")
  private Member member;

}