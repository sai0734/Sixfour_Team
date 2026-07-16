import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/boards`;

export const getList = async (type) => {
  const url = type ? `${prefix}/?type=${type}` : `${prefix}/`;
  const res = await jwtAxios.get(url);

  return res.data;
};

export const getBest = async () => {
  const res = await jwtAxios.get(`${prefix}/best`);

  return res.data;
};

// 마이페이지 "내가 쓴 글"
export const getListByMember = async (memberEmail) => {
  const res = await jwtAxios.get(`${prefix}/member/${memberEmail}`);

  return res.data;
};

export const getOne = async (boardId) => {
  const res = await jwtAxios.get(`${prefix}/${boardId}`);

  return res.data;
};

// AI 한줄요약 - 처음 호출 시 생성(시간이 좀 걸릴 수 있음), 이후엔 캐시된 값 바로 반환
export const getAiSummary = async (boardId) => {
  const res = await jwtAxios.get(`${prefix}/${boardId}/summary`);

  return res.data.aiSummary;
};

export const postAdd = async (board) => {
  const res = await jwtAxios.post(`${prefix}/`, board);

  return res.data;
};

export const putOne = async (board) => {
  const res = await jwtAxios.put(`${prefix}/${board.boardId}`, board);

  return res.data;
};

export const deleteOne = async (boardId) => {
  const res = await jwtAxios.delete(`${prefix}/${boardId}`);

  return res.data;
};
