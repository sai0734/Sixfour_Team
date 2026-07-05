package com.wedding.product.domain;

import com.wedding.global.domain.BaseTimeEntity;
import com.wedding.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_qna")
@Getter
@Builder
@ToString(exclude = {"product", "member", "qna"})
@AllArgsConstructor
@NoArgsConstructor
public class Qna extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long qno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_pno", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_qno", nullable = true)
    private Qna qna;

    @Column(nullable = false)
    private String content;

    public void changeContent(String content) {
        this.content = content;
    }

}