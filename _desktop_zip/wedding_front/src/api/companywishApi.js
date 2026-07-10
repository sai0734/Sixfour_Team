import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/companywishes`;

export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (companyWish) => {
  const res = await jwtAxios.post(`${prefix}/`, companyWish);

  return res.data;
};

export const deleteOne = async (wishId) => {
  const res = await jwtAxios.delete(`${prefix}/${wishId}`);

  return res.data;
};

// wishId를 모를 때 memberEmail + cmno로 찜 취소 (업체 상세페이지 하트 토글용)
export const deleteByMemberAndCompany = async (memberEmail, cmno) => {
  const res = await jwtAxios.delete(
    `${prefix}/member/${memberEmail}/company/${cmno}`,
  );

  return res.data;
};
