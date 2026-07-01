package com.wedding.product.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_review")
@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_pno", nullable = false)
    private Product product;


}
