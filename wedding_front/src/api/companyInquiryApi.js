import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/inquiries`;

// 문의하기 클릭 — 채팅방 생성 또는 기존 방 조회
export const openInquiryRoom = async (cmno) => {
  const res = await jwtAxios.post(`${prefix}/rooms`, null, {
    params: { cmno },
  });
  return res.data;
};

// 매니저 화면 — 업체별 문의방 목록
export const getCompanyInquiryRooms = async (cmno) => {
  const res = await jwtAxios.get(`${prefix}/company/${cmno}/rooms`);
  return res.data;
};

// 회원 화면 - 내가 연 모든 문의방 목록 (안 읽음 뱃지 폴링용)
export const getMyInquiryRooms = async () => {
  const res = await jwtAxios.get(`${prefix}/my-rooms`);
  return res.data;
};

// 채팅창 열 때 / 폴링 — 메시지 목록 조회
export const getInquiryMessages = async (roomId) => {
  const res = await jwtAxios.get(`${prefix}/rooms/${roomId}/messages`);
  return res.data;
};
// 메시지 전송
export const sendInquiryMessage = async (roomId, content) => {
  const res = await jwtAxios.post(`${prefix}/rooms/${roomId}/messages`, {
    content,
  });
  return res.data;
};
