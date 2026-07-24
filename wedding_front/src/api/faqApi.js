import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/faqs`;

export const getList = async (category) => {
  const url = category
    ? `${prefix}/?category=${encodeURIComponent(category)}`
    : `${prefix}/`;
  const res = await jwtAxios.get(url);

  return res.data;
};
