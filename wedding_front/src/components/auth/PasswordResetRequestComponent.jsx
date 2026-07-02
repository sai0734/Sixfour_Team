import { useState } from "react";
import { passwordResetRequestPost } from "../../api/authApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import AuthLayout from "./AuthLayout";

const inputClass =
  "w-full px-4 py-3 rounded-xl border border-rose-100 bg-blush-50/40 text-plum-900 placeholder:text-plum-500/50 focus:border-rose-400 focus:ring-4 focus:ring-rose-100 outline-none transition";
const labelClass = "block text-sm font-semibold text-plum-900/80 mb-1.5";

const PasswordResetRequestComponent = () => {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const { moveToLogin } = useCustomLogin();

  const handleClickRequest = () => {
    if (!email) {
      alert("이메일을 입력해 주세요.");
      return;
    }

    passwordResetRequestPost(email)
      .then((data) => {
        console.log(data);
        setSent(true);
      })
      .catch((err) => {
        console.log(err);
        alert("요청 중 오류가 발생했습니다.");
      });
  };

  return (
    <AuthLayout
      eyebrow="계정 찾기"
      title={
        <>
          계정 정보를
          <br />
          찾아드릴게요
        </>
      }
      subtitle="본인 확인 후 비밀번호 재설정 메일을 보내드려요"
    >
      <div className="max-w-sm w-full mx-auto">
        {sent ? (
          <div className="text-center">
            <div className="w-16 h-16 rounded-full bg-blush-100 flex items-center justify-center mx-auto mb-6 text-3xl">
              ✉️
            </div>
            <h2 className="font-display text-2xl text-plum-900 mb-3">
              메일함을 확인해 주세요
            </h2>
            <p className="text-plum-500 text-sm leading-relaxed mb-8">
              가입된 이메일이라면 비밀번호 재설정 메일을 발송했습니다.
              <br />
              메일함(스팸함 포함)을 확인해 주세요.
            </p>

            <button
              className="w-full py-3 rounded-xl bg-rose-gradient text-white font-semibold shadow-lg shadow-rose-200 hover:shadow-rose-300 hover:-translate-y-0.5 transition-all mb-3"
              onClick={moveToLogin}
            >
              로그인 화면으로
            </button>
            <button
              className="w-full py-3 rounded-xl border border-rose-200 text-rose-600 font-semibold hover:bg-blush-50 transition"
              onClick={() => {
                setSent(false);
                setEmail("");
              }}
            >
              다른 이메일로 다시 시도
            </button>
          </div>
        ) : (
          <>
            <h2 className="font-display text-2xl text-plum-900 mb-1">
              비밀번호 재설정
            </h2>
            <p className="text-plum-500 text-sm mb-8">
              가입하신 이메일 주소를 입력해주세요
            </p>

            <div className="mb-6">
              <label className={labelClass}>이메일</label>
              <input
                className={inputClass}
                type="text"
                placeholder="example@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              ></input>
            </div>

            <button
              className="w-full py-3 rounded-xl bg-rose-gradient text-white font-semibold shadow-lg shadow-rose-200 hover:shadow-rose-300 hover:-translate-y-0.5 transition-all"
              onClick={handleClickRequest}
            >
              재설정 메일 받기
            </button>
          </>
        )}
      </div>
    </AuthLayout>
  );
};

export default PasswordResetRequestComponent;
