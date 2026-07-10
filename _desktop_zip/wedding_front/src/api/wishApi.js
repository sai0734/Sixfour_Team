import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/wishes`;

// 내 찜 목록 조회
export const getListByMember = async () => {
  const res = await jwtAxios.get(`${prefix}/`);

  return res.data;
};

// 찜 등록
export const postAdd = async (pno) => {
  const res = await jwtAxios.post(`${prefix}/product/${pno}`);

  return res.data;
};

// 찜 취소 (하트 토글용)
export const deleteWish = async (pno) => {
  const res = await jwtAxios.delete(`${prefix}/product/${pno}`);

  return res.data;
};

// 찜 여부 확인 (하트 채우기용)
export const isWished = async (pno) => {
  const res = await jwtAxios.get(`${prefix}/product/${pno}`);

  return res.data; // { wished: true/false }
};
