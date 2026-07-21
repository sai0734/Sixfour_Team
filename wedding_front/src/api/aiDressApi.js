import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/ai-dress`;

export const getDressList = async ({ page = 1, size = 12 } = {}) => {
  const res = await jwtAxios.get(`${host}/dresses`, { params: { page, size } });
  return res.data;
};

export const getMyPhoto = async () => {
  const res = await jwtAxios.get(`${host}/my-photo`);
  return res.data;
};

export const uploadMyPhoto = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  const res = await jwtAxios.post(`${host}/my-photo`, formData);
  return res.data;
};

export const requestTryOn = async ({
  dressItemId,
  photoFileName,
  backgroundPrompt,
}) => {
  const res = await jwtAxios.post(`${host}/try-on`, {
    dressItemId,
    photoFileName,
    backgroundPrompt: backgroundPrompt?.trim() || null,
  });
  return res.data;
};

export const getTryOnHistory = async () => {
  const res = await jwtAxios.get(`${host}/history`);
  return res.data;
};

export const updateTryOnHistory = async (historyId, { backgroundPrompt }) => {
  const res = await jwtAxios.put(`${host}/history/${historyId}`, {
    backgroundPrompt: backgroundPrompt?.trim() || null,
  });
  return res.data;
};

export const deleteTryOnHistory = async (historyId) => {
  const res = await jwtAxios.delete(`${host}/history/${historyId}`);
  return res.data;
};
