import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { getAccessToken, getAuthWithAccessToken } from "../../api/kakaoAuthApi";
import { login } from "../../slices/loginSlice";
import { useDispatch } from "react-redux";
import useCustomLogin from "../../hooks/useCustomLogin";

const KakaoRedirectPage = () => {
  const [searchParams] = useSearchParams();

  const { moveToPath } = useCustomLogin();

  const dispatch = useDispatch();

  const authCode = searchParams.get("code");

  useEffect(() => {
    getAccessToken(authCode).then((accessToken) => {
      console.log(accessToken);

      getAuthWithAccessToken(accessToken)
        .then((authInfo) => {
          console.log("------------------");
          console.log(authInfo);

          dispatch(login(authInfo));

          if (authInfo && authInfo.social && !authInfo.profileComplete) {
            moveToPath("/auth/social-complete");
          } else {
            moveToPath("/");
          }
        })
        .catch((err) => {
          console.error(err);

          const data = err.response?.data;

          if (
            data?.error === "ERROR_ACCOUNT_SUSPENDED" ||
            data?.error === "ERROR_ACCOUNT_DORMANT"
          ) {
            alert("차단(정지·휴면)된 회원입니다. 관리자에게 문의해주세요.");
          } else {
            alert("카카오 로그인 중 오류가 발생했습니다.");
          }

          moveToPath("/auth/login");
        });
    });
  }, [authCode]);
  return (
    <div>
      <div>Kakao Login Redirect</div>
      <div>{authCode}</div>
    </div>
  );
};

export default KakaoRedirectPage;
