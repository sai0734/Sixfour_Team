import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/quotes`;

export const uploadQuote = async (file) => {
  const formData = new FormData();
  formData.append("file", file);

  const res = await jwtAxios.post(`${prefix}/upload`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

  return res.data;
};

export const listMyQuotes = async () => {
  const res = await jwtAxios.get(prefix);

  return res.data;
};

// ids=1&ids=2 형태로 보내야 백엔드 @RequestParam List<Long>가 받음
export const compareQuotes = async (idA, idB) => {
  const res = await jwtAxios.get(`${prefix}/compare`, {
    params: { ids: [idA, idB] },
    paramsSerializer: { indexes: null },
  });

  return res.data;
};

// 비교 기록 목록 - 최신순, 페이지 이동 후 돌아와도 이걸로 다시 불러와서 복원함
export const listComparisons = async () => {
  const res = await jwtAxios.get(`${prefix}/comparisons`);

  return res.data;
};

export const getComparison = async (comparisonId) => {
  const res = await jwtAxios.get(`${prefix}/comparisons/${comparisonId}`);

  return res.data;
};

export const deleteQuote = async (quoteId) => {
  const res = await jwtAxios.delete(`${prefix}/${quoteId}`);

  return res.data;
};

// 견적서 이미지는 본인 확인이 필요해서 <img src="...">에 URL을 직접 못 박는다.
// jwtAxios로 인증 요청 후 blob을 받아서 로컬 object URL로 변환해 써야 한다.
export const fetchQuoteImageUrl = async (quoteId) => {
  const res = await jwtAxios.get(`${prefix}/${quoteId}/image`, {
    responseType: "blob",
  });

  return URL.createObjectURL(res.data);
};
