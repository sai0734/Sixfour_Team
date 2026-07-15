import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/reservations`;

// 매니저 - 담당 업체 예약 목록 (서버에서 담당 cmno 자동 조회)
export const getMyManagedCompanyReservations = async () => {
  const res = await jwtAxios.get(`${prefix}/manager/mine`);

  return res.data;
};

// 매니저 - 특정 업체 예약 목록
export const getCompanyReservations = async (cmno) => {
  const res = await jwtAxios.get(`${prefix}/company/${cmno}`);

  return res.data;
};

// 매니저 - 예약대기 → 결제대기(또는 금액 없으면 확정) 확인
export const confirmReservationByManager = async (reservationId) => {
  const res = await jwtAxios.put(`${prefix}/${reservationId}/manager-confirm`);

  return res.data;
};
