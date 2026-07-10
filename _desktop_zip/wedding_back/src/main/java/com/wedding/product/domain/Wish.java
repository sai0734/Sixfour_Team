package com.wedding.product.domain;

import com.wedding.global.domain.BaseTimeEntity;
import com.wedding.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_wish", uniqueConstraints = @UniqueConstraint(columnNames = {"member_email", "product_pno"}))
@Getter
@Builder
@ToString(exclude = {"member", "product"})
@AllArgsConstructor
@NoArgsConstructor
public class Wish extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_pno", nullable = false)
    private Product product;

}
