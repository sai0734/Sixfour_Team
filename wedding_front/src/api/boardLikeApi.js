import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/boardlikes`;

export const checkLiked = async (boardId, memberEmail) => {
  const res = await jwtAxios.get(
    `${prefix}/board/${boardId}/member/${memberEmail}`,
  );

  return res.data.liked;
};

export const likeOne = async (boardId, memberEmail) => {
  const res = await jwtAxios.post(
    `${prefix}/board/${boardId}/member/${memberEmail}`,
  );

  return res.data;
};

export const unlikeOne = async (boardId, memberEmail) => {
  const res = await jwtAxios.delete(
    `${prefix}/board/${boardId}/member/${memberEmail}`,
  );

  return res.data;
};
