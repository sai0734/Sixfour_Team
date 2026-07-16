import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/product`;

// 상품 Q&A 목록 조회 (답변 포함)
export const getQnaList = async (pno) => {
  const res = await jwtAxios.get(`${host}/${pno}/qna`);
  return res.data;
};

// 질문 등록
export const postQna = async (pno, content) => {
  const formData = new FormData();
  formData.append("content", content);

  const res = await jwtAxios.post(`${host}/${pno}/qna`, formData);
  return res.data;
};

// 질문 수정
export const putQna = async (pno, qno, content) => {
  const formData = new FormData();
  formData.append("content", content);

  const res = await jwtAxios.put(`${host}/${pno}/qna/${qno}`, formData);
  return res.data;
};

// 관리자 - 답변 없는 상품 Q&A 목록 (전체 상품 기준)
export const getUnansweredQnaList = async ({ page, size }) => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/admin/qna/unanswered`, {
    params: { page, size },
  });
  return res.data;
};

// 관리자 답변 등록
export const postQnaReply = async (pno, qno, content) => {
  const formData = new FormData();
  formData.append("content", content);

  const res = await jwtAxios.post(`${host}/${pno}/qna/${qno}/reply`, formData);
  return res.data;
};

// 관리자 답변 수정
export const putQnaReply = async (pno, qno, content) => {
  const formData = new FormData();
  formData.append("content", content);

  const res = await jwtAxios.put(`${host}/${pno}/qna/${qno}/reply`, formData);
  return res.data;
};

// 질문/답변 삭제
export const deleteQna = async (pno, qno) => {
  const res = await jwtAxios.delete(`${host}/${pno}/qna/${qno}`);
  return res.data;
};
