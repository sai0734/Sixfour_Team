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
