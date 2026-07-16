import axios from "axios";
import jwtAxios from "../util/jwtUtil";
import { getCompanyImageUrl } from "./companyApi";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/ai-dress`;

export const getDressListForTryOn = async ({ page = 1, size = 12 } = {}) => {
  const res = await axios.get(`${host}/dresses`, { params: { page, size } });
  return res.data;
};

export const uploadMyTryOnPhoto = async (file) => {
  const formData = new FormData();
  formData.append("file", file);

  const res = await jwtAxios.post(`${host}/my-photo`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data;
};

export const getMyTryOnPhoto = async () => {
  const res = await jwtAxios.get(`${host}/my-photo`);
  return res.data;
};

export const requestTryOn = async ({ dressItemId, photoFileName }) => {
  const res = await jwtAxios.post(`${host}/try-on`, {
    dressItemId,
    photoFileName,
  });
  return res.data;
};

export const getTryOnImageUrl = (fileName) => getCompanyImageUrl(fileName);
