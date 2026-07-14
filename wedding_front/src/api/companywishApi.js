import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/companywishes`;

/** 현재 로그인 사용자가 해당 업체를 찜했는지 확인 (옵션 상관없이 하나라도 찜했으면 true) */
export const checkCompanyWish = async (cmno) => {
  const res = await jwtAxios.get(`${prefix}/${cmno}/check`);
  return res.data;
};

/**
 * 업체 찜 등록.
 * 재원 추가 - optionName을 넘기면 홀/드레스/메이크업 옵션과 함께 찜 등록.
 * 옵션 없이 호출하면 기존과 동일(스튜디오 등 옵션 없는 업체 하트 토글).
 */
export const addCompanyWish = async (cmno, optionName) => {
  const res = await jwtAxios.post(
    `${prefix}/${cmno}`,
    null,
    optionName ? { params: { optionName } } : undefined,
  );
  return res.data;
};

/** 업체 찜 해제 (옵션 없는 업체 - 기존 하트 토글용) */
export const removeCompanyWish = async (cmno) => {
  const res = await jwtAxios.delete(`${prefix}/${cmno}`);
  return res.data;
};

// 재원 추가 - 마이페이지 카드/전체선택 삭제는 옵션별로 여러 건일 수 있어서 wishId로 정확히 삭제
export const removeCompanyWishByWishId = async (wishId) => {
  const res = await jwtAxios.delete(`${prefix}/wish/${wishId}`);
  return res.data;
};

/**
 * 마이페이지 - 로그인 사용자의 찜 업체 목록 조회.
 * 재원 수정 - 백엔드 응답이 CompanyDTO 배열 → CompanyWishItemDTO 배열(wishId/optionName 포함)로 바뀜.
 */
export const getMyCompanyWishes = async () => {
  const res = await jwtAxios.get(prefix);
  return res.data;
};
