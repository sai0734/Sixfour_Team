package com.wedding.product.domain;

import com.wedding.checkout.domain.OrderItem;
import com.wedding.global.domain.BaseTimeEntity;
import com.wedding.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_review")
@Getter
@Builder
@ToString(exclude = {"product", "member", "orderItem", "review"})
@AllArgsConstructor
@NoArgsConstructor
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_pno", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_oino", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_rno", nullable = true)
    private Review review;

    @Column(nullable = true)
    private Integer rating;

    @Column(nullable = false)
    private String content;

    @ElementCollection
    @CollectionTable(name = "tbl_review_image", joinColumns = @JoinColumn(name = "review_rno"))
    @Builder.Default
    private List<ReviewImage> imageList = new ArrayList<>();

}
