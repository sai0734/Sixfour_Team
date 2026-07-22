import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/ai-dress`;

/** 드레스 합성·배경 적용은 수 분 걸릴 수 있음 */
const longTimeout = { timeout: 5 * 60 * 1000 };

export const getDressList = async ({ page = 1, size = 12 } = {}) => {
  const res = await jwtAxios.get(`${host}/dresses`, { params: { page, size } });
  return res.data;
};

/** 드레스 합성 — 사람 사진은 multipart로만 전달 (서버 저장 없음) */
export const requestTryOn = async ({ dressItemId, file }) => {
  const formData = new FormData();
  formData.append("dressItemId", String(dressItemId));
  formData.append("file", file);
  const res = await jwtAxios.post(`${host}/try-on`, formData, longTimeout);
  return res.data;
};

/** 합성 결과에 배경만 적용 */
export const applyBackground = async ({ imageBase64, backgroundPrompt }) => {
  const res = await jwtAxios.post(
    `${host}/apply-background`,
    {
      imageBase64,
      backgroundPrompt: backgroundPrompt?.trim() || null,
    },
    longTimeout,
  );
  return res.data;
};
