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
