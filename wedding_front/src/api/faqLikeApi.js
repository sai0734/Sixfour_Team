import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/faqlikes`;

export const checkLiked = async (faqId, memberEmail) => {
  const res = await jwtAxios.get(
    `${prefix}/faq/${faqId}/member/${memberEmail}`,
  );

  return res.data.liked;
};

export const likeOne = async (faqId, memberEmail) => {
  const res = await jwtAxios.post(
    `${prefix}/faq/${faqId}/member/${memberEmail}`,
  );

  return res.data;
};

export const unlikeOne = async (faqId, memberEmail) => {
  const res = await jwtAxios.delete(
    `${prefix}/faq/${faqId}/member/${memberEmail}`,
  );

  return res.data;
};
