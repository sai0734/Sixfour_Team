import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import AuthLayout from "./AuthLayout";
import AlertModal from "./AlertModal";
import KakaoLoginComponent from "./KakaoLoginComponent";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getMyManagedCompany } from "../../api/companyApi";

const inputClass =
  "w-full px-4 py-3 rounded-xl border border-rose-100 bg-blush-50/40 text-plum-900 placeholder:text-plum-500/50 focus:border-rose-400 focus:ring-4 focus:ring-rose-100 outline-none transition";
const labelClass = "block text-sm font-semibold text-plum-900/80 mb-1.5";
const errMsgClass = "text-rose-600 text-xs mt-1";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// APILoginFailHandler.java의 suspendUntil은 Member.suspendUntil(LocalDateTime)의 toString()이라
// "2026-08-15T10:30:00" 형태로 온다. 날짜만 뽑고, 오늘 기준 남은 일수도 같이 계산해서 보여준다.
const formatSuspendUntil = (suspendUntil) => {
  if (!suspendUntil) return null;

  const dateLabel = suspendUntil.slice(0, 10);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const untilDay = new Date(dateLabel);
  const daysLeft = Math.ceil((untilDay - today) / 86400000);

  return daysLeft > 0
    ? `${dateLabel}까지 (${daysLeft}일 남음)`
    : `${dateLabel}까지`;
};

// APILoginFailHandler.java가 내려주는 error 코드/failCount/maxFailCount를 실제로 반영한 안내 문구.
// (예전엔 data.error 존재 여부만 보고 항상 같은 문구를 띄워서 5회 잠금·정지·휴면 안내가 전혀 안 보였음)
const buildLoginErrorMessage = (data) => {
  switch (data.error) {
    case "ERROR_ACCOUNT_LOCKED":
      return "로그인 5회 실패로 계정이 잠겼습니다. 비밀번호 찾기로 재설정해주세요.";
    case "ERROR_ACCOUNT_SUSPENDED": {
      const untilLabel = formatSuspendUntil(data.suspendUntil);
      const parts = [
        `이용이 정지된 계정입니다. ${untilLabel ?? "(정지 해제일 미지정 · 영구 정지)"}`,
      ];
      if (data.suspendReason) parts.push(`사유: ${data.suspendReason}`);
      return parts.join("\n");
    }
    case "ERROR_ACCOUNT_DORMANT":
      return "휴면 처리된 계정입니다. 휴면 해제 절차를 진행해주세요.";
    case "ERROR_ACCOUNT_WITHDRAWN":
      return "탈퇴한 계정입니다.";
    case "ERROR_EMAIL_NOT_VERIFIED":
      return "이메일 인증이 완료되지 않은 계정입니다.";
    default:
      // 실패 횟수는 비밀번호 입력칸 아래 진행바(failInfo)로 시각 표시하므로 여기선 중복하지 않음
      return "이메일 또는 비밀번호가 일치하지 않습니다.";
  }
};

const LoginComponent = () => {
  const [loginParam, setLoginParam] = useState({
    email: "",
    pw: "",
    rememberMe: false,
  });

  const [touched, setTouched] = useState({
    email: false,
    pw: false,
  });

  const [alertMessage, setAlertMessage] = useState("");
  const [failInfo, setFailInfo] = useState(null);

  const location = useLocation();
  const { doLogin, moveToPath, getLoginRedirectPath, clearLoginRedirectPath } =
    useCustomLogin();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setLoginParam({
      ...loginParam,
      [name]: type === "checkbox" ? checked : value,
    });
  };

  const handleBlur = (field) => {
    setTouched({
      ...touched,
      [field]: true,
    });
  };

  const handleClickLogin = (e) => {
    setTouched({
      email: true,
      pw: true,
    });

    if (!loginParam.email || !EMAIL_REGEX.test(loginParam.email)) {
      setAlertMessage("올바른 이메일을 입력해 주세요.");
      return;
    }

    if (!loginParam.pw) {
      setAlertMessage("비밀번호를 입력해 주세요.");
      return;
    }

    doLogin(loginParam).then((data) => {
      if (data.error) {
        setAlertMessage(buildLoginErrorMessage(data));
        setFailInfo(
          data.failCount > 0
            ? { failCount: data.failCount, maxFailCount: data.maxFailCount }
            : null,
        );
        return;
      }

      setFailInfo(null);

      // 로그인 페이지로 오기 전 보고 있던(혹은 가려던) 경로가 있으면 역할과 무관하게 그곳으로 우선 복귀
      const redirectPath =
        location.state?.from || getLoginRedirectPath() || null;
      const isValidRedirect =
        typeof redirectPath === "string" && redirectPath.startsWith("/");

      if (isValidRedirect) {
        clearLoginRedirectPath();
        moveToPath(redirectPath);
        return;
      }

      const isAdmin = data.roleNames?.some((roleName) =>
        ["ADMIN", "ROLE_ADMIN"].includes(roleName),
      );

      if (isAdmin) {
        moveToPath("/admin");
        return;
      }

      // 업체 담당자(매니저)면 문의 관리 화면으로, 아니면 메인으로
      getMyManagedCompany()
        .then((managed) => {
          moveToPath(managed?.isManager ? "/manager/inquiries" : "/");
        })
        .catch(() => {
          moveToPath("/");
        });
    });
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      handleClickLogin(e);
    }
  };

  const renderEmailMsg = () => {
    if (touched.email && !loginParam.email)
      return <div className={errMsgClass}>이메일은 필수 입력 항목입니다.</div>;
    if (
      touched.email &&
      loginParam.email &&
      !EMAIL_REGEX.test(loginParam.email)
    )
      return <div className={errMsgClass}>올바른 이메일 형식이 아니에요</div>;
    return null;
  };

  const renderPwMsg = () => {
    if (touched.pw && !loginParam.pw)
      return (
        <div className={errMsgClass}>비밀번호는 필수 입력 항목입니다.</div>
      );
    return null;
  };

  return (
    <AuthLayout
      eyebrow="Wedding All In One"
      title={
        <>
          소중한 날을 위한
          <br />
          특별한 시작
        </>
      }
      subtitle="로그인하고 나만의 맞춤 웨딩 플랜과 일정을 확인해보세요."
    >
      <div className="max-w-sm w-full mx-auto">
        <h2 className="font-body text-2xl text-plum-900 mb-1">로그인</h2>
        <p className="text-plum-500 text-sm mb-6">
          웨딩올인원 서비스에 오신 것을 환영합니다.
        </p>

        <div className="mb-4">
          <label className={labelClass}>이메일</label>
          <input
            className={inputClass}
            name="email"
            type="text"
            value={loginParam.email}
            placeholder="example@email.com"
            onChange={handleChange}
            onBlur={() => handleBlur("email")}
            onKeyDown={handleKeyDown}
          />
          {renderEmailMsg()}
        </div>

        <div className="mb-6">
          <label className={labelClass}>비밀번호</label>
          <div className="relative">
            <input
              className={inputClass}
              name="pw"
              type="password"
              value={loginParam.pw}
              placeholder="비밀번호를 입력해주세요"
              onChange={handleChange}
              onBlur={() => handleBlur("pw")}
              onKeyDown={handleKeyDown}
            />
          </div>
          {renderPwMsg()}
          {failInfo && (
            <div className="mt-2">
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
        </div>

        <div className="flex items-center justify-between mb-6 text-sm">
          <label className="flex items-center gap-2 cursor-pointer text-plum-700 select-none">
            <input
              type="checkbox"
              name="rememberMe"
              checked={loginParam.rememberMe}
              onChange={handleChange}
              className="w-4 h-4 rounded border-rose-200 text-rose-500 focus:ring-rose-400"
            />
            <span>로그인 유지</span>
          </label>
          <Link
            to="/auth/password-reset"
            className="text-plum-500 hover:text-rose-600 transition"
          >
            비밀번호 찾기
          </Link>
        </div>

        <button
          type="button"
          className="w-full py-4 rounded-xl bg-gradient-to-r from-rose-100 to-pink-100 text-rose-700 font-semibold shadow-md shadow-rose-100 hover:shadow-rose-200 hover:-translate-y-0.5 transition-all mb-4"
          onClick={handleClickLogin}
        >
          로그인하기
        </button>

        <div className="relative flex py-4 items-center">
          <div className="flex-grow border-t border-rose-100"></div>
          <span className="flex-shrink mx-4 text-plum-400 text-xs uppercase tracking-wider">
            간편 로그인
          </span>
          <div className="flex-grow border-t border-rose-100"></div>
        </div>

        <div className="mb-6">
          <KakaoLoginComponent />
        </div>

        <p className="text-center text-sm text-plum-500">
          계정이 없으신가요?{" "}
          <Link
            to="/auth/join"
            className="text-rose-600 font-semibold hover:underline"
          >
            회원가입하기
          </Link>
        </p>
      </div>

      <AlertModal message={alertMessage} onClose={() => setAlertMessage("")} />
    </AuthLayout>
  );
};

export default LoginComponent;
