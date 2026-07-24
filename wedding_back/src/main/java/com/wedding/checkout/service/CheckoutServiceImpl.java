package com.wedding.checkout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wedding.cart.domain.CartItem;
import com.wedding.cart.repository.CartItemRepository;
import com.wedding.checkout.domain.OrderItem;
import com.wedding.checkout.domain.Orders;
import com.wedding.checkout.domain.Payment;
import com.wedding.checkout.dto.*;
import com.wedding.checkout.repository.OrderRepository;
import com.wedding.checkout.repository.PaymentRepository;
import com.wedding.global.util.TossPaymentClient;
import com.wedding.member.domain.Member;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductImage;

import java.util.*;

import com.wedding.product.domain.ProductOption;
import com.wedding.product.repository.ProductOptionRepository;
import com.wedding.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckoutServiceImpl implements CheckoutService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final TossPaymentClient tossPaymentClient;
    private final OrderNotificationService orderNotificationService;

    private static final int FREE_SHIPPING_THRESHOLD = 30000;
    private static final int SHIPPING_FEE = 3000;

    private static final Set<String> EXCHANGE_RETURN_TYPES = Set.of("EXCHANGE", "RETURN");

    // 배송비 계산 (3만원 이상 무료)
    private int calculateShippingFee(int productSubtotal) {
        return productSubtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
    }

    // 주문 생성 (결제 전, PENDING 상태)
    @Override
    public OrderDTO prepareOrder(String memberEmail, CheckoutRequestDTO requestDTO) {

        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);

        Member member = Member.builder().email(memberEmail).build();

        // 주소를 합치지 않고 분리된 필드 그대로 저장
        Orders orders = Orders.builder()
                .orderNumber(orderNumber)
                .member(member)
                .totalPrice(0)
                .shippingFee(0)
                .receiverName(requestDTO.getReceiverName())
                .receiverPhone(requestDTO.getReceiverPhone())
                .zipcode(requestDTO.getZipcode())
                .address(requestDTO.getAddress())
                .addressDetail(requestDTO.getAddressDetail())
                .orderStatus("PENDING")
                .request(requestDTO.getRequest())
                .build();

        int productSubtotal;

        if (requestDTO.getDirectItem() != null) {

            DirectItemDTO directItem = requestDTO.getDirectItem();

            Product product = productRepository.findById(directItem.getPno())
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 상품입니다. pno=" + directItem.getPno()));

            int extra = 0;
            ProductOption optionRef = null;

            if (directItem.getPono() != null) {
                ProductOption option = productOptionRepository.findById(directItem.getPono())
                        .orElseThrow(() -> new NoSuchElementException("존재하지 않는 옵션입니다. pono=" + directItem.getPono()));
                extra = option.getExtraPrice();
                optionRef = ProductOption.builder().pono(option.getPono()).build();
            }

            productSubtotal = (product.getPrice() + extra) * directItem.getQty();

            OrderItem orderItem = OrderItem.builder()
                    .product(Product.builder().pno(product.getPno()).build())
                    .productOption(optionRef)
                    .pnameSnapshot(product.getPname())
                    .priceSnapshot(product.getPrice() + extra)
                    .qty(directItem.getQty())
                    .build();

            orders.addOrderItem(orderItem);

        } else {
            List<CartItem> cartItems = requestDTO.getCinos().stream()
                    .map(cino -> cartItemRepository.findById(cino)
                            .orElseThrow(() -> new NoSuchElementException("장바구니 아이템이 없습니다. cino=" + cino)))
                    .collect(Collectors.toList());

            productSubtotal = cartItems.stream()
                    .mapToInt(ci -> {
                        int extra = ci.getProductOption() != null ? ci.getProductOption().getExtraPrice() : 0;
                        return (ci.getProduct().getPrice() + extra) * ci.getQty();
                    })
                    .sum();

            for (CartItem ci : cartItems) {
                int extra = ci.getProductOption() != null ? ci.getProductOption().getExtraPrice() : 0;

                OrderItem orderItem = OrderItem.builder()
                        .product(Product.builder().pno(ci.getProduct().getPno()).build())
                        .productOption(ci.getProductOption() != null
                                ? ProductOption.builder().pono(ci.getProductOption().getPono()).build()
                                : null)
                        .pnameSnapshot(ci.getProduct().getPname())
                        .priceSnapshot(ci.getProduct().getPrice() + extra)
                        .qty(ci.getQty())
                        .build();

                orders.addOrderItem(orderItem);
            }
        }

        int shippingFee = calculateShippingFee(productSubtotal);
        int totalPrice = productSubtotal + shippingFee;

        orders.changeShippingFee(shippingFee);
        orders.changeTotalPrice(totalPrice);

        Orders saved = orderRepository.save(orders);

        return toDTO(saved);
    }

    // 결제 승인 처리 (토스 승인 API 호출 + 주문상태 변경 + 장바구니 정리 + 알림 발송)
    @Override
    public OrderDTO confirmPayment(String memberEmail, ConfirmRequestDTO confirmRequestDTO) {

        Orders orders = orderRepository.findByOrderNumberAndMember(confirmRequestDTO.getOrderNumber(), memberEmail)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. orderNumber=" + confirmRequestDTO.getOrderNumber()));

        if (orders.getTotalPrice() != confirmRequestDTO.getAmount()) {
            throw new IllegalStateException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        JsonNode result = tossPaymentClient.confirmPayment(
                confirmRequestDTO.getPaymentKey(),
                confirmRequestDTO.getOrderNumber(),
                confirmRequestDTO.getAmount()
        );

        Payment payment = Payment.builder()
                .orders(orders)
                .pgProvider("TOSS")
                .pgTid(confirmRequestDTO.getPaymentKey())
                .amount(confirmRequestDTO.getAmount())
                .payMethod(result.path("method").asText(null))
                .payStatus("DONE")
                .approvedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        orders.changeStatus("PAID");
        orderRepository.save(orders);

        if (confirmRequestDTO.getCinos() != null && !confirmRequestDTO.getCinos().isEmpty()) {
            cartItemRepository.deleteAllById(confirmRequestDTO.getCinos());
        }

        orderNotificationService.sendOrderConfirmation(memberEmail, orders);

        return toDTO(orders);
    }

    // 결제 취소/실패 처리
    @Override
    public void cancelOrder(String memberEmail, String orderNumber) {

        Orders orders = orderRepository.findByOrderNumberAndMember(orderNumber, memberEmail)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. orderNumber=" + orderNumber));

        orders.changeStatus("CANCELLED");
        orderRepository.save(orders);
    }

    // 가장 최근 결제완료 주문의 배송지 조회 (배송지 불러오기용)
    @Override
    public AddressDTO getLatestAddress(String memberEmail) {

        Optional<Orders> result = orderRepository.findLatestPaidOrderByMember(memberEmail);

        if (result.isEmpty()) {
            return null;
        }

        Orders orders = result.get();

        // 분리된 필드 그대로 반환 (합쳐서 반환하지 않음)
        return AddressDTO.builder()
                .receiverName(orders.getReceiverName())
                .receiverPhone(orders.getReceiverPhone())
                .zipcode(orders.getZipcode())
                .address(orders.getAddress())
                .addressDetail(orders.getAddressDetail())
                .build();
    }

    // Orders 엔티티 → OrderDTO 변환
    private OrderDTO toDTO(Orders orders) {

        List<OrderItemDTO> items = orders.getOrderItems().stream()
                .map(oi -> {
                    String thumbnail = productRepository.findById(oi.getProduct().getPno())
                            .flatMap(p -> p.getImageList().stream()
                                    .min(Comparator.comparingInt(ProductImage::getOrd)))
                            .map(ProductImage::getFileName)
                            .orElse(null);

                    return OrderItemDTO.builder()
                            .pno(oi.getProduct().getPno())
                            .pname(oi.getPnameSnapshot())
                            .price(oi.getPriceSnapshot())
                            .qty(oi.getQty())
                            .thumbnail(thumbnail)
                            .build();
                })
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .ono(orders.getOno())
                .orderNumber(orders.getOrderNumber())
                .totalPrice(orders.getTotalPrice())
                .shippingFee(orders.getShippingFee())
                .receiverName(orders.getReceiverName())
                .receiverPhone(orders.getReceiverPhone())
                .zipcode(orders.getZipcode())
                .address(orders.getAddress())
                .addressDetail(orders.getAddressDetail())
                .orderStatus(orders.getOrderStatus())
                .regDate(orders.getRegDate())
                .trackingNo(orders.getTrackingNo()) // 재원 추가 - 배송 조회
                .exchangeReturnType(orders.getExchangeReturnType())
                .exchangeReturnReason(orders.getExchangeReturnReason())
                .exchangeReturnDetail(orders.getExchangeReturnDetail())
                .exchangeReturnRequestedAt(orders.getExchangeReturnRequestedAt())
                .items(items)
                .build();
    }

    // 회원 본인 주문 목록(마이페이지 결제 내역용)
    @Override
    public List<OrderDTO> listMyOrders(String memberEmail) {

        return orderRepository.listByMember(memberEmail).stream().map(order -> toDTO(order))
                .collect(Collectors.toList());

    }

    // 교환/환불 신청 (사유 저장만 - 배송완료 주문만 신청 가능)
    @Override
    public void requestExchangeReturn(String memberEmail, String orderNumber, ExchangeReturnRequestDTO requestDTO) {

        Orders orders = orderRepository.findByOrderNumberAndMember(orderNumber, memberEmail).orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. orderNumber=" + orderNumber));

        if(!"DELIVERED".equals(orders.getOrderStatus())) {
            throw new IllegalStateException("배송완료된 주문만 교환/환불을 신청할 수 있습니다.");
        }

        if(orders.getExchangeReturnType() != null) {
            throw new IllegalStateException("이미 교환/환불 신청이 접수된 주문입니다.");
        }

        if(requestDTO.getType() == null || !EXCHANGE_RETURN_TYPES.contains(requestDTO.getType())) {
            throw new IllegalStateException("교환/반품 구분 값이 올바르지 않습니다.");
        }

        orders.requestExchangeReturn(requestDTO.getType(), requestDTO.getReason(), requestDTO.getDetail(), LocalDateTime.now());
        orders.changeStatus("EXCHANGE".equals(requestDTO.getType()) ? "EXCHANGE_REQUESTED" : "REFUND_REQUESTED");

        orderRepository.save(orders);

    }

}