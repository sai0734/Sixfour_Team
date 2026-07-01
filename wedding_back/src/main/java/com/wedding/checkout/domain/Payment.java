package com.wedding.checkout.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_payment")
@Getter
@Builder
@ToString(exclude = "orders")
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pmno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_ono")
    private Orders orders;

    @Column(nullable = false)
    private String pgProvider;

    @Column(nullable = true)
    private String pgTig;

    @Column(nullable = false)
    private int amount;

    private String payMethod;

    @Column(nullable = false)
    private String payStatus;

    @Column(nullable = true)
    private String failReason;

    @Column(nullable = true)
    private LocalDateTime approvedAt;

}
