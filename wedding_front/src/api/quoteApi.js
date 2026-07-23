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

// 백엔드가 @RequestParam List<Long> ids로 받으므로 ids=1&ids=2 형태로 직렬화되어야 함
export const compareQuotes = async (idA, idB) => {
  const res = await jwtAxios.get(`${prefix}/compare`, {
    params: { ids: [idA, idB] },
    paramsSerializer: { indexes: null },
  });

  return res.data;
};

export const deleteQuote = async (quoteId) => {
  const res = await jwtAxios.delete(`${prefix}/${quoteId}`);

  return res.data;
};

// 견적서 이미지는 본인 확인이 필요해서 <img src="..."> 에 URL을 직접 못 박는다.
// jwtAxios로 인증된 요청을 보내 blob을 받고, 로컬 object URL로 변환해서 써야 한다.
// 호출한 쪽에서 다 쓴 뒤 URL.revokeObjectURL(url)로 정리해줘야 메모리 누수가 없다.
export const fetchQuoteImageUrl = async (quoteId) => {
  const res = await jwtAxios.get(`${prefix}/${quoteId}/image`, {
    responseType: "blob",
  });

  return URL.createObjectURL(res.data);
};
