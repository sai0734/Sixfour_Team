import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/product`;

// 상품 리뷰 목록 조회 (답변 포함)
export const getReviews = async (pno) => {
  const res = await jwtAxios.get(`${host}/${pno}/reviews`);
  return res.data;
};

// 리뷰 작성 자격 확인 (구매 회원인지)
export const checkReviewEligibility = async (pno) => {
  const res = await jwtAxios.get(`${host}/${pno}/reviews/eligibility`);
  return res.data;
};

// 리뷰 작성
export const postReview = async (pno, { rating, content, files }) => {
  const formData = new FormData();

  if (rating) {
    formData.append("rating", rating);
  }
  formData.append("content", content);

  if (files) {
    for (let i = 0; i < files.length; i++) {
      formData.append("files", files[i]);
    }
  }

  const res = await jwtAxios.post(`${host}/${pno}/reviews`, formData);
  return res.data;
};

// 리뷰 수정 (기존 파일 중 유지할 것 + 새로 첨부한 파일)
export const putReview = async (
  pno,
  rno,
  { rating, content, keepFileNames, newFiles },
) => {
  const formData = new FormData();

  if (rating) {
    formData.append("rating", rating);
  }
  formData.append("content", content);

  // 유지할 기존 파일명들
  if (keepFileNames) {
    keepFileNames.forEach((name) => formData.append("keepFileNames", name));
  }

  // 새로 첨부한 파일들
  if (newFiles) {
    for (let i = 0; i < newFiles.length; i++) {
      formData.append("files", newFiles[i]);
    }
  }

  const res = await jwtAxios.put(`${host}/${pno}/reviews/${rno}`, formData);
  return res.data;
};

// 관리자 답변 등록
export const postReply = async (pno, rno, content) => {
  const formData = new FormData();
  formData.append("content", content);

  const res = await jwtAxios.post(
    `${host}/${pno}/reviews/${rno}/reply`,
    formData,
  );
  return res.data;
};

// 관리자 답변 수정
export const putReply = async (pno, rno, content) => {
  const formData = new FormData();
  formData.append("content", content);

  const res = await jwtAxios.put(
    `${host}/${pno}/reviews/${rno}/reply`,
    formData,
  );
  return res.data;
};

// 리뷰 삭제
export const deleteReview = async (pno, rno) => {
  const res = await jwtAxios.delete(`${host}/${pno}/reviews/${rno}`);
  return res.data;
};
