import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/budgets`;

export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const postAdd = async (budget) => {
  const res = await jwtAxios.post(`${prefix}/`, budget);

  return res.data;
};

export const putOne = async (budget) => {
  const res = await jwtAxios.put(`${prefix}/${budget.budgetId}`, budget);

  return res.data;
};

export const deleteOne = async (budgetId) => {
  const res = await jwtAxios.delete(`${prefix}/${budgetId}`);

  return res.data;
};
