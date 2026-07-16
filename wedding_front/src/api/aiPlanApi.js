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
