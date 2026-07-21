import axios from "axios";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/home`;

// 메인 화면 비로그인 폴라로이드 3장(웨딩홀/스드메 매출 1위, 답례품 구매 1위) - 로그인 여부와 무관하게 공개 조회
export const getMainHighlights = async () => {
  const res = await axios.get(`${prefix}/highlights`);

  return res.data;
};
