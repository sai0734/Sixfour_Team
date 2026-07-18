import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/chat`;

// 메시지 전송 - intent가 있으면 백엔드가 해당 함수 하나로 고정해서 호출 (버튼으로 미리 정한 의도).
// companyCategory는 "업체 찾기"에서 카테고리 버튼으로 고정한 값 - 지정되면 백엔드가 강제로 그 카테고리만 사용
export const sendChatMessage = async (message, intent, companyCategory) => {
  const res = await jwtAxios.post(prefix, {
    message,
    intent: intent ?? null,
    companyCategory: companyCategory ?? null,
  });
  return {
    answer: res.data.answer,
    references: res.data.references ?? [],
  };
};

// 드레스 사진 업로드 → 비슷한 드레스 아이템 추천 (버튼 메뉴 전용, 기존 대화형 흐름과 별개)
export const sendDressPhoto = async (file) => {
  const formData = new FormData();
  formData.append("photo", file);

  const res = await jwtAxios.post(`${prefix}/dress-photo`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

  return {
    answer: res.data.answer,
    references: res.data.references ?? [],
  };
};
