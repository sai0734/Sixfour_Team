import { useState } from "react";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getKakaoLogoutLink } from "../../api/kakaoAuthApi";
import { logoutPost } from "../../api/authApi";
import AuthLayout from "./AuthLayout";
import AlertModal from "./AlertModal";

const LogoutComponent = () => {
  const { doLogout, moveToPath, loginState } = useCustomLogin();
  const [alertMessage, setAlertMessage] = useState("");

  const handleClickLogout = async () => {
    const wasSocial = loginState.social;

    try {
      await logoutPost();
    } catch (err) {
      console.log("server logout failed (ignored):", err);
    }

    doLogout();

    if (wasSocial) {
      window.location.href = getKakaoLogoutLink();
      return;
    }

    setAlertMessage("로그아웃되었습니다.");
  };

  return (
    <AuthLayout
      eyebrow="Goodbye"
      title={
        <>
          다음에
          <br />또 만나요
        </>
      }
      subtitle="언제든 다시 로그인해서 이용해주세요"
      stickerEmoji="👋"
    >
      <div className="max-w-sm w-full mx-auto text-center">
        <h2 className="font-body text-2xl text-ink mb-2">로그아웃</h2>
        <p className="text-ink-muted text-sm mb-8">정말 로그아웃 하시겠어요?</p>

        <button
          className="w-full py-3 rounded-full bg-gradient-to-r from-rose-100 to-pink-100 text-rose-700 font-semibold shadow-md shadow-rose-100 hover:shadow-rose-200 hover:-translate-y-0.5 transition-all"
          onClick={handleClickLogout}
        >
          로그아웃
        </button>
        <button
          className="w-full py-3 mt-3 rounded-xl border border-line text-brand-deep font-semibold hover:bg-blush-50 transition"
          onClick={() => moveToPath("/")}
        >
          취소
        </button>
      </div>

      <AlertModal
        message={alertMessage}
        onClose={() => {
          setAlertMessage("");
          moveToPath("/");
        }}
      />
    </AuthLayout>
  );
};

export default LogoutComponent;
