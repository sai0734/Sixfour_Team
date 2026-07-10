import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { passwordResetConfirmPost } from "../../api/authApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import AuthLayout from "./AuthLayout";

const inputClass =
  "w-full px-4 py-3 rounded-xl border border-line bg-white/70 text-ink placeholder:text-ink-faint focus:border-brand focus:ring-4 focus:ring-blush-100 outline-none transition font-body";
const labelClass = "block text-sm font-semibold text-ink-soft mb-1.5 font-body";
const okMsgClass = "text-green-600 text-xs mt-1";
const errMsgClass = "text-rose-600 text-xs mt-1";

const PW_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

const PasswordResetConfirmComponent = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [newPw, setNewPw] = useState("");
  const [newPwCheck, setNewPwCheck] = useState("");
  const [touched, setTouched] = useState({ newPw: false, newPwCheck: false });

  const { moveToLogin } = useCustomLogin();

  const markTouched = (name) => {
    setTouched((prev) => ({ ...prev, [name]: true }));
  };

  const handleClickConfirm = () => {
    setTouched({ newPw: true, newPwCheck: true });

    if (!token) {
      alert("잘못된 접근입니다. 메일에 있는 링크를 통해 다시 시도해 주세요.");
      return;
    }

    if (!newPw) {
      alert("새 비밀번호를 입력해 주세요.");
      return;
    }

    if (!PW_REGEX.test(newPw)) {
      alert("비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다.");
      return;
    }

    if (newPw !== newPwCheck) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    passwordResetConfirmPost(token, newPw)
      .then((data) => {
        console.log(data);

        if (data.result === "success") {
          alert("비밀번호가 재설정되었습니다. 다시 로그인해 주세요.");
          moveToLogin();
        } else {
          alert(
            "재설정에 실패했습니다. (" +
              data.reason +
              ") 링크가 만료되었을 수 있습니다.",
          );
        }
      })
      .catch((err) => {
        console.log(err);
        alert("처리 중 오류가 발생했습니다.");
      });
  };

  const renderNewPwMsg = () => {
    if (touched.newPw && !newPw)
      return (
        <div className={errMsgClass}>새 비밀번호는 필수 입력 항목입니다.</div>
      );
    if (newPw.length > 0 && !PW_REGEX.test(newPw))
      return (
        <div className={errMsgClass}>영문+숫자 조합 8자 이상이어야 해요</div>
      );
    if (newPw.length > 0 && PW_REGEX.test(newPw))
      return <div className={okMsgClass}>✓ 사용가능!</div>;
    return null;
  };

  const renderNewPwCheckMsg = () => {
    // 새 비밀번호 자체가 유효하지 않으면 확인란은 반응하지 않음
    if (!PW_REGEX.test(newPw)) return null;

    if (touched.newPwCheck && !newPwCheck)
      return (
        <div className={errMsgClass}>비밀번호 확인은 필수 입력 항목입니다.</div>
      );
    if (newPwCheck.length > 0 && newPw !== newPwCheck)
      return <div className={errMsgClass}>비밀번호가 서로 달라요</div>;
    if (newPwCheck.length > 0 && newPw === newPwCheck)
      return <div className={okMsgClass}>✓ 확인!</div>;
    return null;
  };

  return (
    <AuthLayout
      eyebrow="새 비밀번호 설정"
      title={
        <>
          새로운
          <br />
          시작이에요
        </>
      }
      subtitle="안전한 새 비밀번호로 계정을 지켜주세요"
      stickerEmoji="🔐"
    >
      <div className="max-w-sm w-full mx-auto">
        <h2 className="font-serifkr text-2xl text-ink mb-1">비밀번호 재설정</h2>
        <p className="text-ink-muted text-sm mb-8">
          새 비밀번호를 입력해주세요
        </p>

        <div className="mb-4">
          <label className={labelClass}>새 비밀번호</label>
          <input
            className={inputClass}
            type="password"
            value={newPw}
            onChange={(e) => setNewPw(e.target.value)}
            onBlur={() => markTouched("newPw")}
          ></input>
          {renderNewPwMsg()}
        </div>

        <div className="mb-6">
          <label className={labelClass}>새 비밀번호 확인</label>
          <input
            className={inputClass}
            type="password"
            value={newPwCheck}
            onChange={(e) => setNewPwCheck(e.target.value)}
            onBlur={() => markTouched("newPwCheck")}
          ></input>
          {renderNewPwCheckMsg()}
        </div>

        <button
          className="w-full py-3 rounded-full bg-brand-gradient text-ink font-semibold shadow-lg shadow-blush-200/60 hover:shadow-blush-300/70 hover:-translate-y-0.5 transition-all"
          onClick={handleClickConfirm}
        >
          비밀번호 변경하기
        </button>
      </div>
    </AuthLayout>
  );
};

export default PasswordResetConfirmComponent;
