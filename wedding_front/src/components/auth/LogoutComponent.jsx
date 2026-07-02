import useCustomLogin from "../../hooks/useCustomLogin";
import { getKakaoLogoutLink } from "../../api/kakaoAuthApi";
import { logoutPost } from "../../api/authApi";
import AuthLayout from "./AuthLayout";

const LogoutComponent = () => {
  const { doLogout, moveToPath, loginState } = useCustomLogin();

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

    alert("로그아웃되었습니다.");
    moveToPath("/");
  };

  return (
    <AuthLayout
      eyebrow="안녕히 가세요"
      title={
        <>
          다음에
          <br />또 만나요
        </>
      }
      subtitle="언제든 다시 로그인해서 이용해주세요"
    >
      <div className="max-w-sm w-full mx-auto text-center">
        <h2 className="font-display text-2xl text-plum-900 mb-2">로그아웃</h2>
        <p className="text-plum-500 text-sm mb-8">정말 로그아웃 하시겠어요?</p>

        <button
          className="w-full py-3 rounded-xl bg-rose-gradient text-white font-semibold shadow-lg shadow-rose-200 hover:shadow-rose-300 hover:-translate-y-0.5 transition-all"
          onClick={handleClickLogout}
        >
          로그아웃
        </button>
        <button
          className="w-full py-3 mt-3 rounded-xl border border-rose-200 text-rose-600 font-semibold hover:bg-blush-50 transition"
          onClick={() => moveToPath("/")}
        >
          취소
        </button>
      </div>
    </AuthLayout>
  );
};

export default LogoutComponent;
