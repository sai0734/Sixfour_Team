import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./reservationApi";

const host = `${API_SERVER_HOST}/api/inquiry`;

// 보안 수정: 이메일은 더 이상 프론트에서 안 보냄 - 서버가 로그인 토큰(JWT)에서
// 직접 꺼내 쓰도록 바뀌어서, 여기서 굳이 email을 넘길 필요/방법이 없어짐

// 일반회원 - 업체에 문의 메시지 보내기
export const sendInquiryMessage = async (cmno, content) => {
  const res = await jwtAxios.post(`${host}/${cmno}/message`, { content });
  return res.data;
};

// 일반회원 - 본인이 그 업체와 나눈 대화 조회
export const getMyInquiryThread = async (cmno) => {
  const res = await jwtAxios.get(`${host}/${cmno}/messages`);
  return res.data;
};

// 담당자 - 자기 업체로 들어온 문의자 목록
export const getManagerThreads = async () => {
  const res = await jwtAxios.get(`${host}/manager/threads`);
  return res.data;
};

// 담당자 - 특정 문의자와의 대화 조회
export const getManagerThread = async (memberEmail) => {
  const res = await jwtAxios.get(`${host}/manager/threads/${memberEmail}`);
  return res.data;
};

// 담당자 - 답장 보내기
export const sendManagerReply = async (memberEmail, content) => {
  const res = await jwtAxios.post(
    `${host}/manager/threads/${memberEmail}/reply`,
    {
      content,
    },
  );
  return res.data;
};
