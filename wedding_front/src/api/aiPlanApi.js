import axios from "axios";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/aiplan`;

// 비로그인 사용자도 결과를 볼 수 있어야 해서(문서 8번) jwtAxios가 아니라 일반 axios로 호출
export const getQuickRecommendations = async (params) => {
  const res = await axios.get(`${prefix}/quick`, { params });

  return res.data;
};

export const getDetailRecommendations = async (params) => {
  const res = await axios.get(`${prefix}/detail`, { params });

  return res.data;
};

// 외부 AI 호출이라 비용이 드는 작업 - POST로 바디에 담아 보냄
export const getAiRecommendations = async (payload) => {
  const res = await axios.post(`${prefix}/ai`, payload);

  return res.data;
};

// 6단계 - 결과 화면에서 자유 발화로 다듬기
export const refineRecommendation = async (payload) => {
  const res = await axios.post(`${prefix}/refine`, payload);

  return res.data;
};

// 6단계 - 바로 직전 턴으로 되돌리기
export const rollbackRecommendation = async (sessionId) => {
  const res = await axios.post(`${prefix}/rollback/${sessionId}`);

  return res.data;
};
