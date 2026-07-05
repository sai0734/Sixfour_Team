import axios from "axios";
import { getCookie, setCookie } from "./cookieUtil";
import { API_SERVER_HOST } from "../api/reservationApi";

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

//before return response
const beforeRes = async (res) => {
  console.log("before return response...........");

  console.log(res);

  //'ERROR_ACCESS_TOKEN'
  const data = res.data;

  if (data && data.error === "ERROR_ACCESS_TOKEN") {
    const authCookieValue = getCookie("auth");

    // 수정: 애초에 토큰이 없는 상태(비회원)에서 이 에러가 온 경우
    // (예: 공개 API인데 서버가 무언가 다른 이유로 이 에러를 준 경우)
    // 갱신 시도 자체가 무의미하므로 그대로 응답을 반환
    if (!authCookieValue) {
      return res;
    }

    const result = await refreshJWT(
      authCookieValue.accessToken,
      authCookieValue.refreshToken,
    );
    console.log("refreshJWT RESULT", result);

    authCookieValue.accessToken = result.accessToken;
    authCookieValue.refreshToken = result.refreshToken;

    setCookie("auth", JSON.stringify(authCookieValue), 1);

    //원래의 호출
    const originalRequest = res.config;

    originalRequest.headers.Authorization = `Bearer ${result.accessToken}`;

    return await axios(originalRequest);
  }

  return res;
};

//fail response
const responseFail = (err) => {
  console.log("response fail error.............");
  return Promise.reject(err);
};

jwtAxios.interceptors.request.use(beforeReq, requestFail);

jwtAxios.interceptors.response.use(beforeRes, responseFail);

export default jwtAxios;
