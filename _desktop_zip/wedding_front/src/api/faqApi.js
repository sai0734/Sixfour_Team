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

export const getOne = async (faqId) => {
  const res = await jwtAxios.get(`${prefix}/${faqId}`);

  return res.data;
};

export const postAdd = async (faq) => {
  const res = await jwtAxios.post(`${prefix}/`, faq);

  return res.data;
};

export const putOne = async (faq) => {
  const res = await jwtAxios.put(`${prefix}/${faq.faqId}`, faq);

  return res.data;
};

export const deleteOne = async (faqId) => {
  const res = await jwtAxios.delete(`${prefix}/${faqId}`);

  return res.data;
};
