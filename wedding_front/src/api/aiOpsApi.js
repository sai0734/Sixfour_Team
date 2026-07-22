import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

// OpenClaw(로컬 자동화 에이전트)가 매일 새벽 점검한 결과를 보여주는 관리자 API

export const getSiteHealthIssues = async () => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/admin/site-health`);
  return res.data;
};

export const getSiteHealthIssueCount = async () => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/admin/site-health/count`);
  return res.data;
};

export const resolveSiteHealthIssue = async (id) => {
  const res = await jwtAxios.put(`${API_SERVER_HOST}/api/admin/site-health/${id}/resolve`);
  return res.data;
};

export const getFlaggedPosts = async () => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/admin/flagged-posts`);
  return res.data;
};

export const getFlaggedPostCount = async () => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/admin/flagged-posts/count`);
  return res.data;
};

export const resolveFlaggedPost = async (id) => {
  const res = await jwtAxios.put(`${API_SERVER_HOST}/api/admin/flagged-posts/${id}/resolve`);
  return res.data;
};

export const getAiBriefings = async () => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/admin/ai-briefing`);
  return res.data;
};

export const triggerDailyCheck = async () => {
  const res = await jwtAxios.post(`${API_SERVER_HOST}/api/admin/openclaw/trigger-daily-check`);
  return res.data;
};

export const triggerWeeklyBriefing = async () => {
  const res = await jwtAxios.post(`${API_SERVER_HOST}/api/admin/openclaw/trigger-weekly-briefing`);
  return res.data;
};

// 특정 작업 하나만 취소하는 방법이 없어서 OpenClaw 게이트웨이 자체를 재시작한다 -
// 그 순간 돌고 있는 다른 작업이 있다면 그것도 같이 끊긴다.
export const cancelOpenClawJob = async () => {
  const res = await jwtAxios.post(`${API_SERVER_HOST}/api/admin/openclaw/cancel`);
  return res.data;
};

// PDF는 <a href>로 바로 못 열림 (Authorization 헤더 필요) - blob으로 받아서 objectURL 생성 후 새 탭으로 연다
export const openAiBriefingPdf = async (id) => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/admin/ai-briefing/${id}/pdf`, {
    responseType: "blob",
  });
  const blobUrl = URL.createObjectURL(res.data);
  window.open(blobUrl, "_blank");
};

export const deleteAiBriefing = async (id) => {
  const res = await jwtAxios.delete(`${API_SERVER_HOST}/api/admin/ai-briefing/${id}`);
  return res.data;
};
