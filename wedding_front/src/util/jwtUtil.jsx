import axios from "axios";
import { getCookie, setCookie, removeCookie } from "./cookieUtil";
import { API_SERVER_HOST } from "../api/reservationApi";
import { showAlert } from "./globalAlert";

const jwtAxios = axios.create();

const refreshJWT = async (accessToken, refreshToken) => {
  const host = API_SERVER_HOST;

  const header = { headers: { Authorization: `Bearer ${accessToken}` } };

  const res = await axios.get(
    `${host}/api/auth/refresh?refreshToken=${refreshToken}`,
    header,
  );

  console.log("----------------------");
  console.log(res.data);

  return res.data;
};

//before request
const beforeReq = (config) => {
  console.log("before request.............");
  const authInfo = getCookie("auth");

  // 수정: 토큰이 없어도 요청을 강제로 막지 않고 그대로 통과시킴
  // (공개 API는 서버의 JWTCheckFilter 화이트리스트가 인증 없이 처리하고,
  //  인증이 진짜 필요한 API는 서버가 401/403으로 걸러주므로 여기서 미리 막을 필요가 없음)
  if (!authInfo) {
    console.log("Auth NOT FOUND - 비회원 요청으로 간주하고 그대로 진행");
    return config;
  }

  const { accessToken } = authInfo;

  // Authorization
  config.headers.Authorization = `Bearer ${accessToken}`;

  return config;
};

//fail request
const requestFail = (err) => {
  console.log("request error............");

  return Promise.reject(err);
};

// 정지/휴면 등으로 강제 로그아웃 처리 (쿠키 삭제 + 안내 + 로그인 페이지로 이동)
const forceLogout = (message) => {
  removeCookie("auth");
  showAlert(message, () => {
    window.location.href = "/auth/login";
  });
};

//before return response
const beforeRes = (res) => {
  console.log("before return response...........");
  console.log(res);

  return res;
};

//fail response
const responseFail = async (err) => {
  console.log("response fail error.............");

  const data = err.response?.data;

  // 정지/휴면 회원: accessToken 자체는 아직 안 만료됐어도 즉시 차단된 경우
  if (data?.error === "ERROR_ACCOUNT_BLOCKED") {
    forceLogout("차단(정지·휴면)된 회원입니다. 관리자에게 문의해주세요.");
    return Promise.reject(err);
  }

  if (data?.error === "ERROR_ACCOUNT_WITHDRAWN") {
    forceLogout("탈퇴한 계정입니다.");
    return Promise.reject(err);
  }

  // 소셜 최초가입인데 추가정보를 아직 안 넣은 상태로 다른 API를 호출한 경우
  // (정상적인 경우 화면단 가드가 먼저 막지만, 직접 API를 호출하는 등 우회 시도에 대한 최종 방어선)
  if (data?.error === "ERROR_PROFILE_INCOMPLETE") {
    window.location.href = "/auth/social-complete";
    return Promise.reject(err);
  }

  // accessToken 만료: refreshToken으로 자동 갱신 후 원래 요청 재시도
  if (data?.error === "ERROR_ACCESS_TOKEN") {
    const authCookieValue = getCookie("auth");

    // 애초에 토큰이 없는 상태(비회원)에서 이 에러가 온 경우 - 갱신 시도 자체가 무의미
    if (!authCookieValue) {
      return Promise.reject(err);
    }

    try {
      const result = await refreshJWT(
        authCookieValue.accessToken,
        authCookieValue.refreshToken,
      );
      console.log("refreshJWT RESULT", result);

      authCookieValue.accessToken = result.accessToken;
      authCookieValue.refreshToken = result.refreshToken;

      setCookie("auth", JSON.stringify(authCookieValue), 1);

      //원래의 호출
      const originalRequest = err.response.config;

      originalRequest.headers.Authorization = `Bearer ${result.accessToken}`;

      return await axios(originalRequest);
    } catch (refreshErr) {
      const refreshErrData = refreshErr.response?.data;

      // refresh 자체도 정지/휴면/탈퇴로 거부된 경우
      if (
        refreshErrData?.error === "ERROR_ACCOUNT_SUSPENDED" ||
        refreshErrData?.error === "ERROR_ACCOUNT_DORMANT"
      ) {
        forceLogout("차단(정지·휴면)된 회원입니다. 관리자에게 문의해주세요.");
      } else if (refreshErrData?.error === "ERROR_ACCOUNT_WITHDRAWN") {
        forceLogout("탈퇴한 계정입니다.");
      }

      return Promise.reject(refreshErr);
    }
  }

  return Promise.reject(err);
};

jwtAxios.interceptors.request.use(beforeReq, requestFail);

jwtAxios.interceptors.response.use(beforeRes, responseFail);

export default jwtAxios;
