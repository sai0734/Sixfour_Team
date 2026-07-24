package com.wedding.checkout.service;

import com.wedding.checkout.domain.Orders;
import com.wedding.checkout.domain.Payment;
import com.wedding.checkout.dto.*;
import com.wedding.checkout.repository.OrderRepository;
import com.wedding.checkout.repository.PaymentRepository;
import com.wedding.global.util.TossPaymentClient;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.product.domain.ProductImage;
import com.wedding.product.repository.ProductRepository;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final OrderNotificationService orderNotificationService;
    private final ProductRepository productRepository;

    private static final Set<String> TERMINAL_ORDER_STATUSES = Set.of("REFUNDED", "CANCELLED");

    // 관리자용 주문 리스트 조회
    @Override
    public PageResponseDTO<AdminOrderListDTO> listOrders(AdminOrderSearchDTO searchDTO) {

        Sort sort = "oldest".equals(searchDTO.getSortType())
                ? Sort.by("ono").ascending()
                : Sort.by("ono").descending();

        Pageable pageable = PageRequest.of(searchDTO.getPage() - 1, searchDTO.getSize(), sort);

        Page<Orders> result = orderRepository.adminSearchOrders(
                searchDTO.getKeyword(), searchDTO.getStatus(), pageable);

        List<AdminOrderListDTO> dtoList = result.get().map(o -> AdminOrderListDTO.builder()
                .ono(o.getOno())
                .orderNumber(o.getOrderNumber())
                .memberEmail(o.getMember().getEmail())
                .receiverName(o.getReceiverName())
                .totalPrice(o.getTotalPrice())
                .orderStatus(o.getOrderStatus())
                .regDate(o.getRegDate())
                .build()
        ).collect(Collectors.toList());

        return PageResponseDTO.<AdminOrderListDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(searchDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    // 주문 상세 조회 (결제정보 포함)
    @Override
    public AdminOrderDetailDTO getOrderDetail(Long ono) {

        Orders orders = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. ono=" + ono));

        Optional<Payment> paymentOpt = paymentRepository.findLatestByOrderOno(ono);

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

        AdminOrderDetailDTO.AdminOrderDetailDTOBuilder builder = AdminOrderDetailDTO.builder()
                .ono(orders.getOno())
                .orderNumber(orders.getOrderNumber())
                .memberEmail(orders.getMember().getEmail())
                .receiverName(orders.getReceiverName())
                .receiverPhone(orders.getReceiverPhone())
                .zipcode(orders.getZipcode())
                .address(orders.getAddress())
                .addressDetail(orders.getAddressDetail())
                .request(orders.getRequest())
                .trackingNo(orders.getTrackingNo())
                .adminMemo(orders.getAdminMemo())
                .totalPrice(orders.getTotalPrice())
                .shippingFee(orders.getShippingFee())
                .orderStatus(orders.getOrderStatus())
                .regDate(orders.getRegDate())
                .items(items)
                .exchangeReturnType(orders.getExchangeReturnType())
                .exchangeReturnReason(orders.getExchangeReturnReason())
                .exchangeReturnDetail(orders.getExchangeReturnDetail())
                .exchangeReturnRequestedAt(orders.getExchangeReturnRequestedAt());

        paymentOpt.ifPresent(payment -> builder
                .payMethod(payment.getPayMethod())
                .pgProvider(payment.getPgProvider())
                .pgTid(payment.getPgTid())
                .payStatus(payment.getPayStatus())
                .approvedAt(payment.getApprovedAt())
        );

        return builder.build();
    }

    // 주문 상태 변경 (+ 회원에게 이메일 알림)
    @Override
    public void changeStatus(Long ono, String newStatus) {

        Orders orders = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. ono=" + ono));

        if (TERMINAL_ORDER_STATUSES.contains(orders.getOrderStatus())) {
            throw new IllegalStateException("환불/취소된 주문은 상태를 변경할 수 없습니다.");
        }

        paymentRepository.findLatestByOrderOno(ono).ifPresent(payment -> {
            if ("CANCELED".equals(payment.getPayStatus())) {
                throw new IllegalStateException("결제가 취소된 주문은 배송 상태를 변경할 수 없습니다.");
            }
        });

        orders.changeStatus(newStatus);
        orderRepository.save(orders);

        orderNotificationService.sendStatusChangeNotification(orders.getMember().getEmail(), orders, newStatus);
    }

    // 여러 주문 상태 일괄 변경
    @Override
    public void bulkChangeStatus(List<Long> onos, String newStatus) {
        onos.forEach(ono -> changeStatus(ono, newStatus));
    }

    // 배송지/연락처 수정
    @Override
    public void updateShippingInfo(Long ono, String receiverName, String receiverPhone,
                                   String zipcode, String address, String addressDetail) {

        Orders orders = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. ono=" + ono));

        orders.changeShippingInfo(receiverName, receiverPhone, zipcode, address, addressDetail);
        orderRepository.save(orders);
    }

    // 운송장 번호 등록/수정
    @Override
    public void updateTrackingNo(Long ono, String trackingNo) {

        Orders orders = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. ono=" + ono));

        orders.changeTrackingNo(trackingNo);
        orderRepository.save(orders);
    }

    // 관리자 메모(CS 요청사항 등) 등록/수정
    @Override
    public void updateAdminMemo(Long ono, String memo) {

        Orders orders = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. ono=" + ono));

        orders.changeAdminMemo(memo);
        orderRepository.save(orders);
    }

    // 환불 처리 (토스 결제취소 API 연동)
    @Override
    public void refund(Long ono, String reason) {

        Orders orders = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다. ono=" + ono));

        Payment payment = paymentRepository.findLatestByOrderOno(ono)
                .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다. ono=" + ono));

        if (TERMINAL_ORDER_STATUSES.contains(orders.getOrderStatus())) {
            throw new IllegalStateException("이미 종료된 주문입니다.");
        }

        if ("CANCELED".equals(payment.getPayStatus())) {
            throw new IllegalStateException("이미 취소된 결제입니다.");
        }

        tossPaymentClient.cancelPayment(payment.getPgTid(), reason);

        payment.changePayStatus("CANCELED");
        orders.changeStatus("REFUNDED");

        orderRepository.save(orders);
        paymentRepository.save(payment);

        orderNotificationService.sendStatusChangeNotification(orders.getMember().getEmail(), orders, "REFUNDED");
    }

}