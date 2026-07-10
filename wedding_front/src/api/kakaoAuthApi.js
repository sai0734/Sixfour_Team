import axios from "axios";
import { API_SERVER_HOST } from "./reservationApi";

const rest_api_key = `47cebef37a089d52919f8db13c601ee1`; // rest키값 (social 앱)
const redirect_uri = `http://localhost:3000/auth/kakao`;
const logout_redirect_uri = `http://localhost:3000/`;

const auth_code_path = `https://kauth.kakao.com/oauth/authorize`;

const access_token_url = `https://kauth.kakao.com/oauth/token`;

const logout_path = `https://kauth.kakao.com/oauth/logout`;

export const getKakaoLoginLink = () => {
  // prompt=login: 브라우저에 카카오 로그인 세션이 남아있어도 그걸 그냥 재사용하지 않고,
  // 매번 카카오 로그인 화면을 다시 띄워서 사용자가 계정을 새로 선택/입력할 수 있게 함
  // (이게 없으면 예전에 로그인했던 카카오 계정으로 묻지도 않고 자동 재로그인됨)
  const kakaoURL = `${auth_code_path}?client_id=${rest_api_key}&redirect_uri=${redirect_uri}&response_type=code&prompt=login`;

  return kakaoURL;
};

// 마이페이지에서 "카카오 계정 연동하기" 눌렀을 때 사용 - state 파라미터로 "이건 로그인이 아니라 연동 요청"임을 표시
// (카카오는 이 state 값을 그대로 콜백에 돌려주므로, 리다이렉트 페이지에서 이 값을 보고 로그인/연동을 분기함)
export const getKakaoLinkLink = () => {
  const kakaoURL = `${auth_code_path}?client_id=${rest_api_key}&redirect_uri=${redirect_uri}&response_type=code&state=link&prompt=login`;

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
