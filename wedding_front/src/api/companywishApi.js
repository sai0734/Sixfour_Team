import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/companywishes`;

/** 현재 로그인 사용자가 해당 업체를 찜했는지 확인 */
export const checkCompanyWish = async (cmno) => {
  const res = await jwtAxios.get(`${prefix}/${cmno}/check`);
  return res.data;
};

/** 업체 찜 등록 */
export const addCompanyWish = async (cmno) => {
  const res = await jwtAxios.post(`${prefix}/${cmno}`);
  return res.data;
};

/** 업체 찜 해제 */
export const removeCompanyWish = async (cmno) => {
  const res = await jwtAxios.delete(`${prefix}/${cmno}`);
  return res.data;
};

/** 마이페이지 - 로그인 사용자의 찜 업체 목록 조회 */
export const getMyCompanyWishes = async () => {
  const res = await jwtAxios.get(prefix);
  return res.data;
};
