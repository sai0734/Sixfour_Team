package com.wedding.faq.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_faq_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"faq_id", "member_email"})
})
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaqLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long faqLikeId;

    @Column(name = "faq_id")
    private Long faqId;

    @Column(name = "member_email")
    private String memberEmail;

    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

}
