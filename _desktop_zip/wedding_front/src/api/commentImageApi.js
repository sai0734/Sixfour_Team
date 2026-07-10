import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/commentimages`;

export const upload = async (commentId, files) => {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file));

  const res = await jwtAxios.post(`${prefix}/${commentId}`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

  return res.data;
};

export const listByComment = async (commentId) => {
  const res = await jwtAxios.get(`${prefix}/comment/${commentId}`);

  return res.data;
};

export const deleteOne = async (imageId) => {
  const res = await jwtAxios.delete(`${prefix}/${imageId}`);

  return res.data;
};
