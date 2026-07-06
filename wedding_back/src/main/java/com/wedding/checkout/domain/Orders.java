package com.wedding.checkout.domain;

import com.wedding.global.domain.BaseTimeEntity;
import com.wedding.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_orders")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "member")
public class Orders extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ono;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int totalPrice;

    @Column(columnDefinition = "int default 0")
    private int shippingFee;

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverPhone;

    private String zipcode;

    @Column(nullable = false)
    private String address;

    private String addressDetail;

    private String trackingNo;

    @Column(nullable = false)
    private String orderStatus;

    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    private String request;

    private String adminMemo;

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrders(this);
    }

    public void changeStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void changeTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void changeShippingFee(int shippingFee) {
        this.shippingFee = shippingFee;
    }

    public void changeTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public void changeAdminMemo(String adminMemo) {
        this.adminMemo = adminMemo;
    }

    public void changeShippingInfo(String receiverName, String receiverPhone,
                                   String zipcode, String address, String addressDetail) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipcode = zipcode;
        this.address = address;
        this.addressDetail = addressDetail;
    }

}