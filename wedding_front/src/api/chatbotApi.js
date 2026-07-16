import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/chat`;

// 메시지 전송 - 답변 텍스트와 카드로 보여줄 참조 목록을 같이 반환
export const sendChatMessage = async (message) => {
  const res = await jwtAxios.post(prefix, { message });
  return {
    answer: res.data.answer,
    references: res.data.references ?? [],
  };
};
