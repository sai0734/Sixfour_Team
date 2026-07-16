import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/admin/dashboard`;

export const getDashboardSummary = async () => {
  const res = await jwtAxios.get(`${prefix}/summary`);
  return res.data;
};

export const getCompanyRanking = async ({ category, month }) => {
  const res = await jwtAxios.get(`${prefix}/company-ranking`, {
    params: { category, month },
  });
  return res.data;
};
