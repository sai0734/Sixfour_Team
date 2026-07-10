import { useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { getAccessToken, getAuthWithAccessToken } from "../../api/kakaoAuthApi";
import { linkKakaoAccountPost, confirmKakaoLinkPost } from "../../api/authApi";
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

          if (authInfo?.status === "PENDING_SIGNUP") {
            // 아직 가입이 완료되지 않음(신규 또는 중단된 가입) - 로그인 상태로 만들지 않고
            // pendingToken만 들려서 추가정보 입력 화면으로 보냄
            navigate("/auth/social-complete", {
              replace: true,
              state: {
                pendingToken: authInfo.pendingToken,
                kakaoEmail: authInfo.kakaoEmail,
              },
            });
            return;
          }

          if (authInfo?.status === "CONFIRM_LINK") {
            // 연동 기록은 없지만 이메일이 같은 기존 회원을 찾음 - 조용히 자동 로그인시키지 않고 먼저 확인
            const confirmed = window.confirm(
              `이미 ${authInfo.kakaoEmail}(으)로 가입된 계정이 있어요. 이 계정과 카카오 연동을 진행할까요?`,
            );

            if (!confirmed) {
              moveToPath("/auth/login");
              return;
            }

            confirmKakaoLinkPost(authInfo.confirmToken)
              .then((data) => {
                dispatch(login(data));
                moveToPath("/");
              })
              .catch((err) => {
                console.error(err);
                const msg = err.response?.data?.msg;
                alert(msg || "연동 확인 중 오류가 발생했습니다.");
                moveToPath("/auth/login");
              });
            return;
          }

          // READY
          dispatch(login(authInfo));
          moveToPath("/");
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
    <div className="flex min-h-[60vh] w-full flex-col items-center justify-center gap-4">
      <div
        className="h-10 w-10 animate-spin rounded-full border-4 border-blush-200 border-t-brand-deep"
        role="status"
        aria-label="로그인 처리 중"
      ></div>
      <p className="text-sm text-ink-muted font-body">
        카카오 로그인 처리 중이에요...
      </p>
    </div>
  );
};

export default KakaoRedirectPage;
