import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useSelector } from "react-redux";
import { modifyAuth } from "../../api/authApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import TapeLabel from "../common/TapeLabel";
import ResultModal from "../common/ResultModal";

const initState = {
  email: "",
  pw: "",
  nickname: "",
};

// TODO(권용익): 이 화면은 "틀"만 잡아둔 상태야. 아래 로직은 이미 동작하게
// 만들어놨는데 (닉네임 변경 + 비밀번호 변경, 빈칸이면 비번은 안 바뀜),
// 카카오 연동/회원탈퇴는 마이페이지 > 계정 설정(AccountSettingsTab)에
// 이미 만들어져 있어서 여기엔 안 넣었어. 필요하면:
//  - 이메일 변경, 프로필 사진 업로드 같은 필드 추가는 여기 카드 구조 그대로
//    복사해서 섹션만 늘리면 됨
//  - 비밀번호 변경 시 "현재 비밀번호 확인" 절차가 필요하면 이 컴포넌트에
//    currentPw state 하나 추가해서 modifyAuth 호출 전에 검증하면 됨
const ModifyComponent = () => {
  const [auth, setAuth] = useState(initState);
  const [pwConfirm, setPwConfirm] = useState("");
  const loginInfo = useSelector((state) => state.loginSlice);

  const { moveToLogin } = useCustomLogin();
  const [result, setResult] = useState();
  const [error, setError] = useState("");

  useEffect(() => {
    // 비밀번호란은 항상 비워둔다. 값을 미리 채워두면 사용자가 안 건드려도
    // "값이 있다"고 판단해 비밀번호가 의도치 않게 바뀌어버릴 수 있다.
    setAuth({ ...loginInfo, pw: "" });
    setPwConfirm("");
  }, [loginInfo]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setAuth((prev) => ({ ...prev, [name]: value }));
    setError("");
  };

  const handleClickModify = () => {
    if (auth.pw && auth.pw !== pwConfirm) {
      setError("새 비밀번호가 일치하지 않아요.");
      return;
    }

    // 비밀번호를 안 건드렸으면(빈 값) 비번 변경 요청 자체를 보내지 않는다.
    const payload = auth.pw ? auth : { ...auth, pw: "" };

    modifyAuth(payload).then(() => {
      setResult("Modified");
    });
  };

  const closeModal = () => {
    setResult(null);
    moveToLogin();
  };

  return (
    <div className="max-w-lg">
      {result && (
        <ResultModal
          title={"회원정보"}
          content={"정보수정완료"}
          callbackFn={closeModal}
        />
      )}

      {/* 기본 정보 */}
      <div className="bg-white rounded-2xl shadow-[0_8px_24px_-14px_rgba(58,54,47,0.18)] p-6 mb-4">
        <TapeLabel className="mb-4">기본 정보</TapeLabel>

        <label className="block text-xs font-medium text-ink-muted mb-1.5">
          이메일
        </label>
        <input
          className="w-full h-11 px-4 rounded-lg border border-line-soft bg-surface text-sm text-ink-faint mb-4"
          name="email"
          type="text"
          value={auth.email}
          readOnly
        />

        <label className="block text-xs font-medium text-ink-muted mb-1.5">
          닉네임
        </label>
        <input
          className="w-full h-11 px-4 rounded-lg border border-line-soft text-sm outline-none focus:border-brand"
          name="nickname"
          type="text"
          value={auth.nickname}
          onChange={handleChange}
        />
      </div>

      {/* 비밀번호 변경 */}
      <div className="bg-white rounded-2xl shadow-[0_8px_24px_-14px_rgba(58,54,47,0.18)] p-6 mb-4">
        <TapeLabel className="mb-4" rotate="right">
          비밀번호 변경
        </TapeLabel>

        <label className="block text-xs font-medium text-ink-muted mb-1.5">
          새 비밀번호
        </label>
        <input
          className="w-full h-11 px-4 rounded-lg border border-line-soft text-sm outline-none focus:border-brand mb-4"
          name="pw"
          type="password"
          placeholder="변경하지 않으려면 비워두세요"
          value={auth.pw}
          onChange={handleChange}
        />

        <label className="block text-xs font-medium text-ink-muted mb-1.5">
          새 비밀번호 확인
        </label>
        <input
          className="w-full h-11 px-4 rounded-lg border border-line-soft text-sm outline-none focus:border-brand"
          type="password"
          placeholder="변경하지 않으려면 비워두세요"
          value={pwConfirm}
          onChange={(e) => {
            setPwConfirm(e.target.value);
            setError("");
          }}
        />

        {error && <p className="text-xs text-red-600 mt-2">{error}</p>}
      </div>

      <p className="text-xs text-ink-faint mb-6">
        카카오 계정 연동이나 회원탈퇴는{" "}
        <Link to="/mypage?tab=account" className="text-brand-accent underline">
          마이페이지 &gt; 계정 설정
        </Link>
        에서 하실 수 있어요.
      </p>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={handleClickModify}
          className="h-11 px-8 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
        >
          저장하기
        </button>
      </div>
    </div>
  );
};

export default ModifyComponent;
