import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/chat`;

// 메시지 전송
export const sendChatMessage = async (message) => {
  const res = await jwtAxios.post(prefix, { message });
  return res.data.answer;
};
