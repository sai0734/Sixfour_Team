import axios from "axios";
import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/packages`;

export const getPackageList = async (pageParam = {}) => {
  const { page = 1, size = 12, keyword, sort } = pageParam;

  const res = await axios.get(`${host}/list`, {
    params: { page, size, keyword, sort },
  });

  return res.data;
};

export const getPackageOne = async (weddingPackageId) => {
  const res = await axios.get(`${host}/${weddingPackageId}`);
  return res.data;
};

export const postPackageAdd = async (payload) => {
  const res = await jwtAxios.post(`${host}/`, payload);
  return res.data;
};

export const putPackageOne = async (weddingPackageId, payload) => {
  const res = await jwtAxios.put(`${host}/${weddingPackageId}`, payload);
  return res.data;
};

export const deletePackageOne = async (weddingPackageId) => {
  const res = await jwtAxios.delete(`${host}/${weddingPackageId}`);
  return res.data;
};
