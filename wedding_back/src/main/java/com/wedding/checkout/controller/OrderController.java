package com.wedding.checkout.controller;

import com.wedding.checkout.dto.*;
import com.wedding.checkout.service.OrderService;
import com.wedding.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN')")
public class OrderController {

    private final OrderService adminOrderService;

    // 관리자용 주문 리스트 조회
    @GetMapping
    public PageResponseDTO<AdminOrderListDTO> list(AdminOrderSearchDTO searchDTO) {

        log.info("AdminOrderController_list 실행~~~~~~~~");

        return adminOrderService.listOrders(searchDTO);

    }

    // 주문 상세 조회 (결제정보 포함)
    @GetMapping("/{ono}")
    public AdminOrderDetailDTO detail(@PathVariable Long ono) {

        log.info("AdminOrderController_detail 실행~~~~~~~~");

        return adminOrderService.getOrderDetail(ono);

    }

    // 주문 상태 변경 (+ 회원에게 이메일 알림)
    @PutMapping("/{ono}/status")
    public Map<String, String> changeStatus(@PathVariable Long ono, @RequestParam String status) {

        log.info("AdminOrderController_changeStatus 실행~~~~~~~~");

        adminOrderService.changeStatus(ono, status);

        return Map.of("RESULT", "SUCCESS");

    }

    // 여러 주문 상태 일괄 변경
    @PutMapping("/bulk-status")
    public Map<String, String> bulkChangeStatus(@RequestBody Map<String, Object> body) {

        log.info("AdminOrderController_bulkChangeStatus 실행~~~~~~~~");

        @SuppressWarnings("unchecked")
        List<Integer> rawOnos = (List<Integer>) body.get("onos");
        List<Long> onos = rawOnos.stream().map(Integer::longValue).toList();
        String status = (String) body.get("status");

        adminOrderService.bulkChangeStatus(onos, status);

        return Map.of("RESULT", "SUCCESS");
    }

    // 배송지/연락처 수정
    @PutMapping("/{ono}/shipping")
    public Map<String, String> updateShipping(@PathVariable Long ono, @RequestBody Map<String, String> body) {

        log.info("AdminOrderController_updateShipping 실행~~~~~~~~");

        adminOrderService.updateShippingInfo(
                ono,
                body.get("receiverName"),
                body.get("receiverPhone"),
                body.get("zipcode"),
                body.get("address"),
                body.get("addressDetail")
        );

        return Map.of("RESULT", "SUCCESS");

    }

    // 운송장 번호 등록/수정
    @PutMapping("/{ono}/tracking")
    public Map<String, String> updateTracking(@PathVariable Long ono, @RequestParam String trackingNo) {

        log.info("AdminOrderController_updateTracking 실행~~~~~~~~");

        adminOrderService.updateTrackingNo(ono, trackingNo);

        return Map.of("RESULT", "SUCCESS");

    }

    // 관리자 메모(CS 요청사항 등) 등록/수정
    @PutMapping("/{ono}/memo")
    public Map<String, String> updateMemo(@PathVariable Long ono, @RequestParam String memo) {

        log.info("AdminOrderController_updateMemo 실행~~~~~~~~");

        adminOrderService.updateAdminMemo(ono, memo);

        return Map.of("RESULT", "SUCCESS");

    }

    // 환불 처리 (토스 결제취소 API 연동)
    @PostMapping("/{ono}/refund")
    public Map<String, String> refund(@PathVariable Long ono, @RequestParam String reason) {

        log.info("AdminOrderController_refund 실행~~~~~~~~");

        adminOrderService.refund(ono, reason);

        return Map.of("RESULT", "SUCCESS");

    }

}