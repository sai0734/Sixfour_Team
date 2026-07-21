import axios from "axios";
import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/aiplan`;

// 비로그인 사용자도 결과를 볼 수 있어야 해서(문서 8번) jwtAxios가 아니라 일반 axios로 호출
export const getQuickRecommendations = async (params) => {
  const res = await axios.get(`${prefix}/quick`, { params });

  return res.data;
};

// jwtAxios는 토큰이 없어도 그냥 통과시키므로(비로그인) 여기서도 안전하게 쓸 수 있다.
// 로그인 상태면 토큰을 실어 보내서, 서버가 세션에 회원 이메일을 남길 수 있게 한다
// (detail/ai 모드만 세션을 만듦 - quick 모드는 세션이 없어서 그대로 axios 유지).
export const getDetailRecommendations = async (params) => {
  const res = await jwtAxios.get(`${prefix}/detail`, { params });

  return res.data;
};

// 외부 AI 호출이라 비용이 드는 작업 - POST로 바디에 담아 보냄
export const getAiRecommendations = async (payload) => {
  const res = await jwtAxios.post(`${prefix}/ai`, payload);

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

// 사이드패널 확정/해제 버튼 - AI 안 거치고 바로 슬롯 상태 반영
export const updateSlotStatus = async (payload) => {
  const res = await axios.post(`${prefix}/slot`, payload);

  return res.data;
};

// 새로고침 복원 - URL의 sessionId로 현재 세션 상태를 다시 불러옴
export const getSessionResult = async (sessionId) => {
  const res = await axios.get(`${prefix}/session/${sessionId}`);

  return res.data;
};

// 다듬기 대화 기록 - "다듬은 기록 보기" 펼치기용. aiPlanHistory.js의 getSessionHistory(브라우저
// 로컬 회차 목록)와는 다른 것 - 이건 서버에 저장된 한 세션 안의 턴별 발화 기록.
export const getRefineHistory = async (sessionId) => {
  const res = await axios.get(`${prefix}/session/${sessionId}/history`);

  return res.data;
};

// "이 결과 마이페이지에 담기" - 웨딩플랜/예산관리/체크리스트에 반영. 로그인 필수라 jwtAxios로 호출.
export const applySessionToPlan = async (sessionId) => {
  const res = await jwtAxios.post(`${prefix}/session/${sessionId}/apply-to-plan`);

  return res.data;
};

// 메인 화면 "AI 매칭 진행중" 위젯 - 로그인 회원의 가장 최근 세션 기준 홀/드레스/스튜디오 진행률
export const getMyAiPlanProgress = async () => {
  const res = await jwtAxios.get(`${prefix}/progress`);

  return res.data;
};
