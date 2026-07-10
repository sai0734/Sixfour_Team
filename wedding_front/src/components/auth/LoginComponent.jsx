import { useState } from "react";
import { useLocation } from "react-router-dom";
import useCustomLogin from "../../hooks/useCustomLogin";
import KakaoLoginComponent from "./KakaoLoginComponent";
import AuthLayout from "./AuthLayout";

const initState = {
  email: "",
  pw: "",
  rememberMe: false,
};

const inputClass =
  "w-full px-4 py-3 rounded-xl border border-rose-100 bg-blush-50/40 text-plum-900 placeholder:text-plum-500/50 focus:border-rose-400 focus:ring-4 focus:ring-rose-100 outline-none transition";
const labelClass = "block text-sm font-semibold text-plum-900/80 mb-1.5";

const LoginComponent = () => {
  const [loginParam, setLoginParam] = useState({ ...initState });
  const [failInfo, setFailInfo] = useState(null);

  const location = useLocation();
  const { doLogin, moveToPath, getLoginRedirectPath, clearLoginRedirectPath } =
    useCustomLogin();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;

    loginParam[name] = type === "checkbox" ? checked : value;

    setLoginParam({ ...loginParam });
  };

  const handleClickLogin = (e) => {
    e.preventDefault();

    doLogin(loginParam).then((data) => {
      console.log(data);
      if (data.error) {
        if (data.error === "ERROR_ACCOUNT_LOCKED") {
          alert(
            "로그인 5회 실패로 계정이 잠겼습니다. 비밀번호 재설정 페이지로 이동합니다.",
          );
          moveToPath("/auth/password-reset");
        } else if (
          data.error === "ERROR_ACCOUNT_SUSPENDED" ||
          data.error === "ERROR_ACCOUNT_DORMANT"
        ) {
          alert("차단(정지·휴면)된 회원입니다. 관리자에게 문의해주세요.");
        } else if (data.error === "ERROR_ACCOUNT_WITHDRAWN") {
          alert("탈퇴한 계정입니다.");
        } else if (data.error === "ERROR_EMAIL_NOT_VERIFIED") {
          alert(
            "전송된 이메일을 확인해주세요. 인증을 완료해야 로그인할 수 있어요.",
          );
        } else {
          if (data.failCount) {
            setFailInfo({
              failCount: data.failCount,
              maxFailCount: data.maxFailCount,
            });
          }
          alert("이메일과 패스워드를 다시 확인하세요");
        }
      } else {
        setFailInfo(null);

        const redirectPath =
          location.state?.from || getLoginRedirectPath() || null;

        // location.state.from은 다른 화면에서 실수로 객체를 넘겼을 가능성을 배제할 수 없어서,
        // "/"로 시작하는 문자열일 때만 실제 경로로 인정 (아니면 무시하고 기본 이동)
        const isValidRedirect =
          typeof redirectPath === "string" && redirectPath.startsWith("/");

        if (isValidRedirect) {
          clearLoginRedirectPath();
          alert("로그인 성공");
          moveToPath(redirectPath);
          return;
        }

        alert("로그인 성공");

        const isAdmin = data.roleNames?.some((roleName) =>
          ["ADMIN", "ROLE_ADMIN"].includes(roleName),
        );

        moveToPath(isAdmin ? "/admin" : "/");
      }
    });
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      handleClickLogin(e);
    }
  };

  return (
    <AuthLayout
      eyebrow="안전한 로그인"
      title={
        <>
          다시 만나서
          <br />
          반가워요
        </>
      }
      subtitle="로그인하고 다양한 서비스를 이용해보세요"
    >
      <div className="max-w-sm w-full mx-auto">
        <h2 className="font-display text-2xl text-plum-900 mb-1">로그인</h2>
        <p className="text-plum-500 text-sm mb-8">계정에 로그인하세요</p>

        <form onSubmit={handleClickLogin}>
          <div className="mb-4">
            <label className={labelClass}>이메일</label>
            <input
              className={inputClass}
              name="email"
              type="email"
              lang="en"
              inputMode="email"
              autoComplete="email"
              autoCapitalize="off"
              autoCorrect="off"
              spellCheck={false}
              placeholder="example@email.com"
              value={loginParam.email}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
            ></input>
          </div>

          <div className="mb-2">
            <label className={labelClass}>비밀번호</label>
            <input
              className={inputClass}
              name="pw"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={loginParam.pw}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
            ></input>
          </div>

          {failInfo && (
            <div className="mb-4">
              <div className="text-rose-600 text-xs font-medium mb-1">
                ⚠ 비밀번호가 틀렸어요 ({failInfo.failCount}/
                {failInfo.maxFailCount}회)
              </div>
              <div className="w-full h-1.5 bg-rose-100 rounded-full overflow-hidden">
                <div
                  className="h-full bg-rose-500 transition-all"
                  style={{
                    width: `${(failInfo.failCount / failInfo.maxFailCount) * 100}%`,
                  }}
                ></div>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between mb-6 mt-4">
            <label className="flex items-center gap-2 text-sm text-plum-900/70">
              <input
                type="checkbox"
                name="rememberMe"
                checked={loginParam.rememberMe}
                onChange={handleChange}
                className="accent-rose-500"
              />
              로그인 유지 (30일)
            </label>
            <button
              type="button"
              className="text-sm text-rose-600 hover:text-rose-700 font-medium"
              onClick={() => moveToPath("/auth/password-reset")}
            >
              비밀번호 찾기
            </button>
          </div>

          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-rose-gradient text-white font-semibold shadow-lg shadow-rose-200 hover:shadow-rose-300 hover:-translate-y-0.5 transition-all"
          >
            로그인
          </button>
        </form>

        <div className="flex items-center gap-3 my-6">
          <div className="flex-1 h-px bg-plum-900/10"></div>
          <span className="text-xs text-plum-500">또는</span>
          <div className="flex-1 h-px bg-plum-900/10"></div>
        </div>

        <KakaoLoginComponent />

        <div className="text-center mt-6 text-sm text-plum-500">
          계정이 없으신가요?{" "}
          <button
            className="text-rose-600 font-semibold hover:text-rose-700"
            onClick={() => moveToPath("/auth/join")}
          >
            회원가입
          </button>
        </div>
      </div>
    </AuthLayout>
  );
};

export default LoginComponent;
