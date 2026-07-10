import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/admin/orders`;

// 관리자용 주문 리스트 조회
export const getAdminOrderList = async ({
  page,
  size,
  keyword,
  status,
  sortType,
}) => {
  const res = await jwtAxios.get(host, {
    params: { page, size, keyword, status, sortType },
  });
  return res.data;
};

// 주문 상세 조회
export const getAdminOrderDetail = async (ono) => {
  const res = await jwtAxios.get(`${host}/${ono}`);
  return res.data;
};

// 주문 상태 변경
export const changeOrderStatus = async (ono, status) => {
  const res = await jwtAxios.put(`${host}/${ono}/status`, null, {
    params: { status },
  });
  return res.data;
};

// 여러 주문 상태 일괄 변경
export const bulkChangeOrderStatus = async (onos, status) => {
  const res = await jwtAxios.put(`${host}/bulk-status`, { onos, status });
  return res.data;
};

// 배송지/연락처 수정
export const updateOrderShipping = async (ono, shippingInfo) => {
  const res = await jwtAxios.put(`${host}/${ono}/shipping`, shippingInfo);
  return res.data;
};

// 운송장 번호 등록/수정
export const updateOrderTracking = async (ono, trackingNo) => {
  const res = await jwtAxios.put(`${host}/${ono}/tracking`, null, {
    params: { trackingNo },
  });
  return res.data;
};

// 관리자 메모 등록/수정
export const updateOrderMemo = async (ono, memo) => {
  const res = await jwtAxios.put(`${host}/${ono}/memo`, null, {
    params: { memo },
  });
  return res.data;
};

// 환불 처리
export const refundOrder = async (ono, reason) => {
  const res = await jwtAxios.post(`${host}/${ono}/refund`, null, {
    params: { reason },
  });
  return res.data;
};
