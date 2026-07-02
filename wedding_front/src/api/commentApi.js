import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/comments`;

export const getListByBoard = async (boardId) => {
  const res = await jwtAxios.get(`${prefix}/board/${boardId}`);

  return res.data;
};

export const postAdd = async (comment) => {
  const res = await jwtAxios.post(`${prefix}/`, comment);

  return res.data;
};

export const putOne = async (comment) => {
  const res = await jwtAxios.put(`${prefix}/${comment.commentId}`, comment);

  return res.data;
};

export const deleteOne = async (commentId) => {
  const res = await jwtAxios.delete(`${prefix}/${commentId}`);

  return res.data;
};
