import axios from "axios";
import jwtAxios from "../util/jwtUtil";

// 서버 주소 - 다른 api 파일들도 이 값을 가져다 씀, 이름/위치 바꾸지 말 것
export const API_SERVER_HOST = "http://localhost:8080";

const prefix = `${API_SERVER_HOST}/api/reservations`;

export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (reservation) => {
  const res = await jwtAxios.post(`${prefix}/`, reservation);

  return res.data;
};

export const putOne = async (reservation) => {
  const res = await jwtAxios.put(
    `${prefix}/${reservation.reservationId}`,
    reservation,
  );

  return res.data;
};

export const deleteOne = async (reservationId) => {
  const res = await jwtAxios.delete(`${prefix}/${reservationId}`);

  return res.data;
};

// ↓↓↓ 재원 추가 - 업체 예약 결제 (날짜/옵션 선택 → 토스 결제)

// 결제창 열기 전 - 주문번호 발급
export const preparePayment = async (reservationId) => {
  const res = await jwtAxios.post(`${prefix}/${reservationId}/payment/prepare`);

  return res.data;
};

// 결제 승인 (paymentKey, orderNumber, amount)
export const confirmPayment = async (reservationId, payload) => {
  const res = await jwtAxios.post(
    `${prefix}/${reservationId}/payment/confirm`,
    payload,
  );

  return res.data;
};

// 결제 취소/실패 처리
export const cancelPayment = async (reservationId) => {
  const res = await jwtAxios.post(`${prefix}/${reservationId}/payment/cancel`);

  return res.data;
};

// 업체 상세페이지 "결제 횟수" 표시용 - 비로그인 사용자도 볼 수 있어야 하므로 axios(비인증) 사용
export const getPaymentCount = async (cmno) => {
  const res = await axios.get(`${prefix}/company/${cmno}/payment-count`);

  return res.data.paymentCount;
};

// 예약 날짜 중복 확인 - 같은 업체+같은 옵션+같은 날짜 예약이 이미 있는지 (비로그인도 확인 가능)
export const checkAvailability = async (cmno, optionName, weddingDate) => {
  const res = await axios.get(`${prefix}/company/${cmno}/availability`, {
    params: { optionName, weddingDate },
  });

  return res.data.taken;
};
// ↑↑↑ 재원 추가

// 승진 코드 추가
// 묶음 결제 - 주문번호 발급 (reservationIds: number[])
export const prepareBulkPayment = async (reservationIds) => {
  const res = await jwtAxios.post(
    `${prefix}/payment/bulk-prepare`,
    reservationIds,
  );

  return res.data;
};

// 묶음 결제 승인 ({ paymentKey, orderNumber, amount, reservationIds })
export const confirmBulkPayment = async (payload) => {
  const res = await jwtAxios.post(`${prefix}/payment/bulk-confirm`, payload);

  return res.data;
};

// 묶음 결제 취소/실패 처리 (reservationIds: number[])
export const cancelBulkPayment = async (reservationIds) => {
  const res = await jwtAxios.post(
    `${prefix}/payment/bulk-cancel`,
    reservationIds,
  );

  return res.data;
};
// 승진 코드 추가 끝
