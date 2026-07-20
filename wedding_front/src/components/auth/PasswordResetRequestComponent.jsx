import { useState } from "react";
import AuthLayout from "./AuthLayout";

const inputClass =
  "w-full px-4 py-3 rounded-xl border border-rose-100 bg-blush-50/40 text-plum-900 placeholder:text-plum-500/50 focus:border-rose-400 focus:ring-4 focus:ring-rose-100 outline-none transition";
const labelClass = "block text-sm font-semibold text-plum-900/80 mb-1.5";
const errMsgClass = "text-rose-600 text-xs mt-1";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const PasswordResetRequestComponent = () => {
  const [email, setEmail] = useState("");
  const [touched, setTouched] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleChange = (e) => {
    setEmail(e.target.value);
  };

  const handleSubmit = () => {
    setTouched(true);

    if (!email) {
      alert("이메일을 입력해 주세요.");
      return;
    }

    if (!EMAIL_REGEX.test(email)) {
      alert("올바른 이메일 형식이 아닙니다.");
      return;
    }

    setSubmitted(true);
  };

  const renderEmailMsg = () => {
    if (touched && !email)
      return <div className={errMsgClass}>이메일은 필수 입력 항목입니다.</div>;
    if (touched && email && !EMAIL_REGEX.test(email))
      return <div className={errMsgClass}>올바른 이메일 형식이 아니에요</div>;
    return null;
  };

  if (submitted) {
    return (
      <AuthLayout
        eyebrow="메일 전송 완료"
        title={
          <>
            메일함을
            <br />
            확인해 주세요
          </>
        }
        subtitle="비밀번호 재설정 링크를 보냈어요"
      >
        <div className="max-w-sm w-full mx-auto text-center">
          <div className="w-16 h-16 rounded-full bg-blush-100 flex items-center justify-center mx-auto mb-6 text-3xl">
            ✉️
          </div>
          <h2 className="font-display text-2xl text-plum-900 mb-2">
            메일함을 확인해 주세요
          </h2>
          <p className="text-plum-500 text-sm mb-1">
            <span className="font-semibold text-rose-600">{email}</span>
          </p>
          <p className="text-plum-500 text-sm mb-8">
            비밀번호 재설정 링크를 발송했습니다. 스팸함도 확인해 주세요.
          </p>

          <button
            type="button"
            className="w-full py-4 rounded-xl bg-gradient-to-r from-rose-500 to-pink-500 text-white font-semibold shadow-lg shadow-rose-200 hover:shadow-rose-300 hover:-translate-y-0.5 transition-all mb-3"
            onClick={() => setSubmitted(false)}
          >
            다시 보내기
          </button>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      eyebrow="비밀번호 찾기"
      title={
        <>
          비밀번호를
          <br />
          잊으셨나요?
        </>
      }
      subtitle="가입하신 이메일로 새 비밀번호를 설정할 수 있어요"
    >
      <div className="max-w-sm w-full mx-auto">
        <h2 className="font-display text-2xl text-plum-900 mb-1">
          비밀번호 재설정
        </h2>
        <p className="text-plum-500 text-sm mb-6">
          가입하신 이메일 주소를 입력해주세요
        </p>

        <div className="mb-6">
          <label className={labelClass}>이메일 *</label>
          <input
            className={inputClass}
            name="email"
            type="text"
            placeholder="example@email.com"
            value={email}
            onChange={handleChange}
            onBlur={() => setTouched(true)}
          />
          {renderEmailMsg()}
        </div>

        <button
          type="button"
          className="w-full py-4 rounded-xl bg-gradient-to-r from-rose-100 to-pink-100 text-rose-700 font-semibold shadow-md shadow-rose-100 hover:shadow-rose-200 hover:-translate-y-0.5 transition-all"
          onClick={handleSubmit}
        >
          재설정 메일 받기
        </button>
      </div>
    </AuthLayout>
  );
};

export default PasswordResetRequestComponent;
