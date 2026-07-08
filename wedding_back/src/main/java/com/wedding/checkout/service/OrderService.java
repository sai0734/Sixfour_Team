package com.wedding.checkout.service;

import com.wedding.checkout.dto.AdminOrderDetailDTO;
import com.wedding.checkout.dto.AdminOrderListDTO;
import com.wedding.checkout.dto.AdminOrderSearchDTO;
import com.wedding.global.dto.PageResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface OrderService {

    // 관리자용 주문 리스트 조회
    PageResponseDTO<AdminOrderListDTO> listOrders(AdminOrderSearchDTO searchDTO);

    // 주문 상세 조회 (결제정보 포함)
    AdminOrderDetailDTO getOrderDetail(Long ono);

    // 주문 상태 변경 (+ 회원에게 이메일 알림)
    void changeStatus(Long ono, String newStatus);

    // 여러 주문 상태 일괄 변경
    void bulkChangeStatus(List<Long> onos, String newStatus);

    // 배송지/연락처 수정
    void updateShippingInfo(Long ono, String receiverName, String receiverPhone,
                            String zipcode, String address, String addressDetail);

    // 운송장 번호 등록/수정
    void updateTrackingNo(Long ono, String trackingNo);

    // 관리자 메모(CS 요청사항 등) 등록/수정
    void updateAdminMemo(Long ono, String memo);

    // 환불 처리 (토스 결제취소 API 연동)
    void refund(Long ono, String reason);
}
