import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const prefix = `${API_SERVER_HOST}/api/boardimages`;

// 게시글 등록 후 받은 boardId로 파일들 업로드
export const upload = async (boardId, files) => {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file));

  const res = await jwtAxios.post(`${prefix}/${boardId}`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

  return res.data;
};

export const listByBoard = async (boardId) => {
  const res = await jwtAxios.get(`${prefix}/board/${boardId}`);

  return res.data;
};

// 실제 파일 접근 URL (img/video src에 그대로 사용)
export const fileUrl = (fileName) => `${prefix}/view/${fileName}`;

// 확장자로 이미지/동영상 구분
export const isVideoFile = (fileName) =>
  /\.(mp4|mov|webm|avi|mkv)$/i.test(fileName || "");
