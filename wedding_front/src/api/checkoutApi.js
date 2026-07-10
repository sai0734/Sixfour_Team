import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/checkout`;

// 주문 생성 (결제 전)
export const prepareOrder = async (payload) => {
  const res = await jwtAxios.post(`${host}/prepare`, payload);
  return res.data;
};

// 결제 승인
export const confirmPayment = async (payload) => {
  const res = await jwtAxios.post(`${host}/confirm`, payload);
  return res.data;
};

// 결제 취소/실패 처리
export const cancelOrder = async (orderNumber) => {
  const res = await jwtAxios.post(`${host}/cancel/${orderNumber}`);
  return res.data;
};

// 최근 배송지 불러오기
export const getLastAddress = async () => {
  const res = await jwtAxios.get(`${host}/last-address`);
  return res.data;
};

// 회원 본인 주문 목록 (마이페이지 결제내역 - 답례품 쇼핑몰 탭)
// 주의: 백엔드 컨트롤러 매핑이 "/api/checkout" + "/api/my-order"로 겹쳐서
// 실제 등록된 경로가 /api/checkout/api/my-order 임 (오타로 보이지만 현재 이 경로가 맞음)
export const getMyOrders = async () => {
  const res = await jwtAxios.get(`${host}/api/my-order`);
  return res.data;
};

// 교환/반품 신청 (백엔드에 아직 없음 - 이 경로/스펙대로 만들어달라고 요청해둔 상태)
// payload: { type: "EXCHANGE" | "RETURN", reason, detail }
export const requestExchangeReturn = async (orderNumber, payload) => {
  const res = await jwtAxios.post(
    `${host}/orders/${orderNumber}/exchange-return`,
    payload,
  );
  return res.data;
};

// 교환/반품 신청 현황 조회 (있으면 이미 신청한 건, 없으면 null)
export const getExchangeReturn = async (orderNumber) => {
  const res = await jwtAxios.get(
    `${host}/orders/${orderNumber}/exchange-return`,
  );
  return res.data;
};
