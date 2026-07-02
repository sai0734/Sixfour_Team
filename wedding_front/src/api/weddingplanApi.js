import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/weddingplans`;

// 1:1 관계라 목록이 아니라 회원 기준 단건 조회
export const getByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (weddingPlan) => {
  const res = await jwtAxios.post(`${prefix}/`, weddingPlan);

  return res.data;
};

export const putOne = async (weddingPlan) => {
  const res = await jwtAxios.put(
    `${prefix}/${weddingPlan.weddingPlanId}`,
    weddingPlan,
  );

  return res.data;
};

export const deleteOne = async (weddingPlanId) => {
  const res = await jwtAxios.delete(`${prefix}/${weddingPlanId}`);

  return res.data;
};
