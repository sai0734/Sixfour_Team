import { useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { getAccessToken, getAuthWithAccessToken } from "../../api/kakaoAuthApi";
import { linkKakaoAccountPost } from "../../api/authApi";
import { login } from "../../slices/loginSlice";
import { useDispatch, useSelector } from "react-redux";
import useCustomLogin from "../../hooks/useCustomLogin";

const KakaoRedirectPage = () => {
  const [searchParams] = useSearchParams();

  const { moveToPath } = useCustomLogin();
  const navigate = useNavigate();

  const dispatch = useDispatch();

  const loginState = useSelector((state) => state.loginSlice);

  const authCode = searchParams.get("code");
  const mode = searchParams.get("state"); // "link"면 연동 요청, 없으면 일반 로그인

  useEffect(() => {
    getAccessToken(authCode).then((accessToken) => {
      console.log(accessToken);

      if (mode === "link") {
        // 마이페이지에서 "카카오 계정 연동하기"로 온 경우 - 로그인 처리 대신 현재 로그인된 계정에 연동
        // 결과와 상관없이 처리 후엔 마이페이지의 "계정 설정" 탭으로 바로 돌아감
        linkKakaoAccountPost(loginState.email, accessToken)
          .then(() => {
            alert("카카오 계정이 연동되었습니다.");
            navigate("/mypage?tab=account", { replace: true });
          })
          .catch((err) => {
            console.error(err);
            const msg = err.response?.data?.msg;
            alert(msg || "연동 중 오류가 발생했습니다.");
            navigate("/mypage?tab=account", { replace: true });
          });
        return;
      }

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
          } else if (data?.error === "ERROR_ACCOUNT_WITHDRAWN") {
            alert("탈퇴한 계정입니다.");
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
