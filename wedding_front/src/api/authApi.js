import axios from "axios";
import { API_SERVER_HOST } from "./reservationApi";
import jwtAxios from "../util/jwtUtil";

const host = `${API_SERVER_HOST}/api/auth`;

export const loginPost = async (loginParam) => {
  const header = { headers: { "Content-Type": "x-www-form-urlencoded" } };

  const form = new FormData();
  form.append("username", loginParam.email);
  form.append("password", loginParam.pw);
  form.append("rememberMe", loginParam.rememberMe ? "true" : "false");

  const res = await axios.post(`${host}/login`, form, header);

  return res.data;
};

export const modifyAuth = async (auth) => {
  const res = await jwtAxios.put(`${host}/modify`, auth);

  return res.data;
};

export const joinPost = async (joinParam) => {
  const res = await axios.post(`${host}/join`, joinParam);

  return res.data;
};

export const socialCompletePost = async (socialCompleteParam) => {
  const memberHost = `${API_SERVER_HOST}/api/member`;

  const res = await jwtAxios.put(
    `${memberHost}/social-complete`,
    socialCompleteParam,
  );

  return res.data;
};

// pendingToken을 들고 카카오 가입을 최종 완료. 아직 로그인 전 상태라 jwtAxios가 아니라 그냥 axios로 호출
// (성공하면 응답에 accessToken/refreshToken이 담겨오는데, 이때부터가 진짜 로그인 시작 시점)
export const completeKakaoSignupPost = async (payload) => {
  const res = await axios.put(`${host}/kakao/complete`, payload);

  return res.data;
};

// CONFIRM_LINK 상태에서 "연동할게요"를 선택했을 때 - 로그인 전 상태라 그냥 axios로 호출
export const confirmKakaoLinkPost = async (confirmToken) => {
  const res = await axios.put(`${host}/kakao/confirm-link`, { confirmToken });

  return res.data;
};

export const passwordResetRequestPost = async (email) => {
  const res = await axios.post(`${host}/password-reset/request`, { email });

  return res.data;
};

export const passwordResetConfirmPost = async (token, newPw) => {
  const res = await axios.post(`${host}/password-reset/confirm`, {
    token,
    newPw,
  });

  return res.data;
};

export const logoutPost = async () => {
  const res = await jwtAxios.post(`${host}/logout`);

  return res.data;
};

export const resendVerificationPost = async (email) => {
  const res = await axios.post(`${host}/resend-verification`, { email });

  return res.data;
};

export const checkEmailAvailable = async (email) => {
  const res = await axios.get(`${host}/check-email`, { params: { email } });

  return res.data; // { available: true/false }
};

export const checkNicknameAvailable = async (nickname) => {
  const res = await axios.get(`${host}/check-nickname`, {
    params: { nickname },
  });

  return res.data; // { available: true/false }
};

export const checkPhoneAvailable = async (phone) => {
  const res = await axios.get(`${host}/check-phone`, {
    params: { phone },
  });

  return res.data; // { available: true/false }
};

export const withdrawPost = async (email) => {
  const res = await jwtAxios.delete(`${API_SERVER_HOST}/api/member/withdraw`, {
    data: { email },
  });

  return res.data;
};

export const linkKakaoAccountPost = async (email, kakaoAccessToken) => {
  const res = await jwtAxios.post(`${API_SERVER_HOST}/api/member/social-link`, {
    email,
    kakaoAccessToken,
  });

  return res.data;
};

export const unlinkSocialAccount = async (email, provider) => {
  const res = await jwtAxios.delete(
    `${API_SERVER_HOST}/api/member/social-unlink/${provider}`,
    { params: { email } },
  );

  return res.data;
};

export const getSocialAccounts = async (email) => {
  const res = await jwtAxios.get(
    `${API_SERVER_HOST}/api/member/social-accounts`,
    {
      params: { email },
    },
  );

  return res.data; // { providers: ["kakao", ...] }
};

// 마이페이지 회원정보수정 - 이름/전화번호/생년월일/주소 조회·수정
export const getMemberDetail = async (email) => {
  const res = await jwtAxios.get(`${API_SERVER_HOST}/api/member/detail`, {
    params: { email },
  });

  return res.data;
};

export const modifyMemberDetail = async (detailParam) => {
  const res = await jwtAxios.put(
    `${API_SERVER_HOST}/api/member/detail`,
    detailParam,
  );

  return res.data;
};
