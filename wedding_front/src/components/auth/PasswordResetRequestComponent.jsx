import { useState } from "react";
import { passwordResetRequestPost } from "../../api/authApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import AuthLayout from "./AuthLayout";

const inputClass =
  "w-full px-4 py-3 rounded-xl border border-line bg-white/70 text-ink placeholder:text-ink-faint focus:border-brand focus:ring-4 focus:ring-blush-100 outline-none transition font-body";
const labelClass = "block text-sm font-semibold text-ink-soft mb-1.5 font-body";

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
      stickerEmoji="🔑"
    >
      <div className="max-w-sm w-full mx-auto">
        {sent ? (
          <div className="text-center">
            <div className="w-16 h-16 rounded-full bg-blush-100 flex items-center justify-center mx-auto mb-6 text-3xl">
              ✉️
            </div>
            <h2 className="font-serifkr text-2xl text-ink mb-3">
              메일함을 확인해 주세요
            </h2>
            <p className="text-ink-muted text-sm leading-relaxed mb-8">
              가입된 이메일이라면 비밀번호 재설정 메일을 발송했습니다.
              <br />
              메일함(스팸함 포함)을 확인해 주세요.
            </p>

            <button
              className="w-full py-3 rounded-full bg-brand-gradient text-ink font-semibold shadow-lg shadow-blush-200/60 hover:shadow-blush-300/70 hover:-translate-y-0.5 transition-all mb-3"
              onClick={() => moveToLogin()}
            >
              로그인 화면으로
            </button>
            <button
              className="w-full py-3 rounded-xl border border-line text-brand-deep font-semibold hover:bg-blush-50 transition"
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
            <h2 className="font-serifkr text-2xl text-ink mb-1">
              비밀번호 재설정
            </h2>
            <p className="text-ink-muted text-sm mb-8">
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
              className="w-full py-3 rounded-full bg-brand-gradient text-ink font-semibold shadow-lg shadow-blush-200/60 hover:shadow-blush-300/70 hover:-translate-y-0.5 transition-all"
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
