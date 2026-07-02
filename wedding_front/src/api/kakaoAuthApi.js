import axios from "axios";
import { API_SERVER_HOST } from "./reservationApi";

const rest_api_key = `47cebef37a089d52919f8db13c601ee1`; // rest키값 (social 앱)
const redirect_uri = `http://localhost:3000/auth/kakao`;
const logout_redirect_uri = `http://localhost:3000/`;

const auth_code_path = `https://kauth.kakao.com/oauth/authorize`;

const access_token_url = `https://kauth.kakao.com/oauth/token`;

const logout_path = `https://kauth.kakao.com/oauth/logout`;

export const getKakaoLoginLink = () => {
  const kakaoURL = `${auth_code_path}?client_id=${rest_api_key}&redirect_uri=${redirect_uri}&response_type=code`;

  return kakaoURL;
};

export const getKakaoLogoutLink = () => {
  const kakaoLogoutURL = `${logout_path}?client_id=${rest_api_key}&logout_redirect_uri=${logout_redirect_uri}`;

  return kakaoLogoutURL;
};

export const getAccessToken = async (authCode) => {
  const header = {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
  };
  const params = {
    grant_type: "authorization_code",
    client_id: rest_api_key,
    redirect_uri: redirect_uri,
    code: authCode,
  };

  const res = await axios.post(access_token_url, params, header);

  const accessToken = res.data.access_token;

  return accessToken;
};

export const getAuthWithAccessToken = async (accessToken) => {
  const res = await axios.get(
    `${API_SERVER_HOST}/api/auth/kakao?accessToken=${accessToken}`,
  );

  return res.data;
};
