import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/hallpayments`;

export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (payment) => {
  const res = await jwtAxios.post(`${prefix}/`, payment);

  return res.data;
};

export const putOne = async (payment) => {
  const res = await jwtAxios.put(`${prefix}/${payment.paymentId}`, payment);

  return res.data;
};

export const deleteOne = async (paymentId) => {
  const res = await jwtAxios.delete(`${prefix}/${paymentId}`);

  return res.data;
};
