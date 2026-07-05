import { API_SERVER_HOST } from "./reservationApi";
import jwtAxios from "../util/jwtUtil";

const host = `${API_SERVER_HOST}/api/product`;

export const postAdd = async (product) => {
  const header = { headers: { "Content-Type": "multipart/form-data" } };

  // 경로 뒤 '/' 주의
  const res = await jwtAxios.post(`${host}/`, product, header);

  return res.data;
};

export const getList = async (pageParam) => {
  const {
    page,
    size,
    categories,
    keyword,
    minPrice,
    maxPrice,
    minRating,
    sortType,
  } = pageParam;

  const res = await jwtAxios.get(`${host}/list`, {
    params: {
      page,
      size,
      categories,
      keyword,
      minPrice,
      maxPrice,
      minRating,
      sortType,
    },
  });

  return res.data;
};

export const getCategories = async () => {
  const res = await jwtAxios.get(`${host}/categories`);

  return res.data;
};

export const getOne = async (pno) => {
  const res = await jwtAxios.get(`${host}/${pno}`);

  return res.data;
};

export const getOptions = async (pno) => {
  const res = await jwtAxios.get(`${host}/${pno}/options`);

  return res.data;
};

export const putOne = async (pno, product) => {
  const header = { headers: { "Content-Type": "multipart/form-data" } };

  const res = await jwtAxios.put(`${host}/${pno}`, product, header);

  return res.data;
};

export const deleteOne = async (pno) => {
  const res = await jwtAxios.delete(`${host}/${pno}`);

  return res.data;
};
