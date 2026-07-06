import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/ddayevents`;

export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (event) => {
  const res = await jwtAxios.post(`${prefix}/`, event);

  return res.data;
};

export const putOne = async (event) => {
  const res = await jwtAxios.put(`${prefix}/${event.ddayId}`, event);

  return res.data;
};

export const deleteOne = async (ddayId) => {
  const res = await jwtAxios.delete(`${prefix}/${ddayId}`);

  return res.data;
};
