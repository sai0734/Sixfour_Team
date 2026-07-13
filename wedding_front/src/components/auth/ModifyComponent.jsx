import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import {
  modifyAuth,
  getSocialAccounts,
  unlinkSocialAccount,
  withdrawPost,
} from "../../api/authApi";
import { getKakaoLinkLink } from "../../api/kakaoAuthApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import TapeLabel from "../common/TapeLabel";

const initState = {
  email: "",
  pw: "",
  nickname: "",
};

const WITHDRAW_CONFIRM_TEXT = "회원탈퇴";
const PW_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+=-]).{8,}$/;

// TODO(권용익): 이 화면은 "틀"만 잡아둔 상태야. 아래 로직은 이미 동작하게
// 만들어놨는데 (닉네임 변경 + 비밀번호 변경, 빈칸이면 비번은 안 바뀜).
//  - 이메일 변경, 프로필 사진 업로드 같은 필드 추가는 여기 카드 구조 그대로
//    복사해서 섹션만 늘리면 됨
//  - 비밀번호 변경 시 "현재 비밀번호 확인" 절차가 필요하면 이 컴포넌트에
//    currentPw state 하나 추가해서 modifyAuth 호출 전에 검증하면 됨
//
// 추가(로그인팀): 카카오로만 가입한 회원(social=true)은 원래 비밀번호가
// 랜덤 자동생성값이라 본인이 몰라. 여기서 새로 설정하면 서버에서 social도
// 함께 false로 전환해줘서(MemberServiceImpl.modifyMember), 그 즉시 아래
// "카카오 연동 해제"가 가능해짐.
//
// 추가(로그인팀 2차): 마이페이지 > 계정 설정(AccountSettingsTab)에 있던
// "소셜 계정 연동"과 "회원탈퇴"를 이 화면 하나로 합침 - 소셜 연동은 맨 위,
// 회원탈퇴는 비밀번호 변경 카드 바로 아래. AccountSettingsTab.jsx는 이제
// 이 화면에 흡수됐으니 참고만 하고 실제로는 안 씀(마이페이지 "계정 설정" 탭도 제거함).
const ModifyComponent = () => {
  const [auth, setAuth] = useState(initState);
  const [pwConfirm, setPwConfirm] = useState("");
  const loginInfo = useSelector((state) => state.loginSlice);

  const { doLogout, moveToPath } = useCustomLogin();
  const [result, setResult] = useState();
  const [error, setError] = useState("");

  const isSocialOnly = Boolean(loginInfo.social);
  // loginInfo.social은 로그인할 때 발급된 토큰 안 값이라 그 이후에 비밀번호를
  // 설정해도 즉시 반영이 안 됨(다시 로그인해야 갱신됨). 그래서 "방금 이 화면에서
  // 비밀번호를 설정했는지"는 별도로 로컬 상태로 들고 있다가, 배너 표시 여부에 반영함
  const [passwordJustSet, setPasswordJustSet] = useState(false);

  // ---- 소셜 계정 연동 ----
  const [linkedProviders, setLinkedProviders] = useState([]);
  const [loadingProviders, setLoadingProviders] = useState(true);

  // ---- 회원탈퇴 ----
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [confirmInput, setConfirmInput] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);

  useEffect(() => {
    // 비밀번호란은 항상 비워둔다. 값을 미리 채워두면 사용자가 안 건드려도
    // "값이 있다"고 판단해 비밀번호가 의도치 않게 바뀌어버릴 수 있다.
    setAuth({ ...loginInfo, pw: "" });
    setPwConfirm("");
  }, [loginInfo]);

  useEffect(() => {
    if (!loginInfo.email) return;

    getSocialAccounts(loginInfo.email)
      .then((data) => setLinkedProviders(data.providers || []))
      .catch((err) => console.error(err))
      .finally(() => setLoadingProviders(false));
  }, [loginInfo.email]);

  const isKakaoLinked = linkedProviders.includes("kakao");

  const handleUnlinkKakao = () => {
    if (!window.confirm("카카오 계정 연동을 해제할까요?")) return;

    unlinkSocialAccount(loginInfo.email, "kakao")
      .then(() => {
        alert("카카오 연동이 해제되었습니다.");
        setLinkedProviders((prev) => prev.filter((p) => p !== "kakao"));
      })
      .catch((err) => {
        console.error(err);
        const msg = err.response?.data?.msg;
        alert(msg || "연동 해제 중 오류가 발생했습니다.");
      });
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setAuth((prev) => ({ ...prev, [name]: value }));
    setError("");
  };

  const handleClickModify = () => {
    if (auth.pw && !PW_REGEX.test(auth.pw)) {
      setError(
        "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상이어야 해요.",
      );
      return;
    }

    if (auth.pw && auth.pw !== pwConfirm) {
      setError("새 비밀번호가 일치하지 않아요.");
      return;
    }

    const nicknameChanged = auth.nickname !== loginInfo.nickname;
    const passwordChanged = Boolean(auth.pw);

    // 닉네임도 그대로, 비밀번호도 안 건드렸으면 저장 요청 자체를 보내지 않음
    // (예전엔 무조건 요청을 보내서, 아무것도 안 바꿔도 "수정되었습니다"가 떴음)
    if (!nicknameChanged && !passwordChanged) {
      alert("변경된 내용이 없어요.");
      return;
    }

    // 비밀번호를 안 건드렸으면(빈 값) 비번 변경 요청 자체를 보내지 않는다.
    const payload = auth.pw ? auth : { ...auth, pw: "" };

    modifyAuth(payload).then(() => {
      if (passwordChanged) {
        setPasswordJustSet(true);
      }
      setResult("Modified");
    });
  };

  // 저장은 탈퇴와 달리 세션을 끊을 이유가 없으므로, 로그아웃/이동 없이
  // 그냥 완료 안내 모달만 닫는다
  const closeModal = () => {
    setResult(null);
  };

  const handleWithdraw = () => {
    if (confirmInput !== WITHDRAW_CONFIRM_TEXT) return;

    setWithdrawing(true);

    withdrawPost(loginInfo.email)
      .then(() => {
        alert("회원탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다.");
        doLogout();
        moveToPath("/");
      })
      .catch((err) => {
        console.error(err);
        alert("탈퇴 처리 중 오류가 발생했습니다.");
      })
      .finally(() => setWithdrawing(false));
  };

  return (
    <div className="max-w-lg">
      {result && (
        <div
          className="fixed inset-0 z-[1055] flex items-center justify-center bg-black/30 px-4"
          onClick={closeModal}
        >
          <div
            className="bg-white rounded-2xl shadow-[0_20px_50px_-12px_rgba(58,54,47,0.35)] max-w-sm w-full p-8 text-center"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="w-14 h-14 rounded-full bg-brand-light flex items-center justify-center mx-auto mb-4 text-2xl text-brand-accent">
              ✓
            </div>
            <p className="font-serif text-lg text-ink mb-1">수정되었습니다</p>
            <p className="text-sm text-ink-muted mb-6">
              변경하신 내용이 저장됐어요.
            </p>
            <button
              onClick={closeModal}
              className="h-11 px-8 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
            >
              확인
            </button>
          </div>
        </div>
      )}

      {/* 소셜 계정 연동 */}
      <div className="bg-white rounded-2xl shadow-[0_8px_24px_-14px_rgba(58,54,47,0.18)] p-6 mb-4">
        <h3 className="text-base font-semibold text-ink mb-4">
          소셜 계정 연동
        </h3>

        <div className="flex items-center justify-between rounded-xl border border-line-soft px-4 py-3">
          <div className="flex items-center gap-3">
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-[#FEE500] text-[#3C1E1E] font-bold text-sm">
              K
            </span>
            <div>
              <div className="text-sm font-medium text-ink">카카오</div>
              <div className="text-xs text-ink-muted">
                {loadingProviders
                  ? "확인 중..."
                  : isKakaoLinked
                    ? "연동됨"
                    : "연동 안 됨"}
              </div>
            </div>
          </div>

          {!loadingProviders &&
            (isKakaoLinked ? (
              <button
                onClick={handleUnlinkKakao}
                className="h-8 px-4 rounded-full border border-line-soft text-xs text-red-600 hover:bg-cream shrink-0"
              >
                연동 해제
              </button>
            ) : (
              <a
                href={getKakaoLinkLink()}
                className="h-8 px-4 flex items-center rounded-full bg-[#FEE500] text-[#3C1E1E] text-xs font-semibold hover:brightness-95 shrink-0"
              >
                연동하기
              </a>
            ))}
        </div>
      </div>

      {/* 기본 정보 */}
      <div className="bg-white rounded-2xl shadow-[0_8px_24px_-14px_rgba(58,54,47,0.18)] p-6 mb-4">
        <div className="flex items-center justify-between mb-4">
          <TapeLabel>기본 정보</TapeLabel>
          <span className="w-10 h-10 rounded-full bg-brand-light text-brand-accent font-serif text-sm flex items-center justify-center">
            {auth.nickname?.[0] || "🙂"}
          </span>
        </div>

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
        <TapeLabel className="mb-4" rotate={3}>
          비밀번호 변경
        </TapeLabel>

        {isSocialOnly && !passwordJustSet && (
          <div className="mb-4 rounded-lg bg-brand-light/60 border border-brand-light px-4 py-3">
            <p className="text-xs text-brand-accent leading-relaxed">
              아직 설정된 비밀번호가 없어요. 여기서 새로 설정하면 이메일로도
              로그인할 수 있고, 아래에서 카카오 연동을 해제할 때도 필요해요.
            </p>
          </div>
        )}

        <label className="block text-xs font-medium text-ink-muted mb-1.5">
          새 비밀번호
        </label>
        <input
          className="w-full h-11 px-4 rounded-lg border border-line-soft text-sm outline-none focus:border-brand"
          name="pw"
          type="password"
          placeholder="변경하지 않으려면 비워두세요"
          value={auth.pw}
          onChange={handleChange}
        />
        <p className="text-xs text-ink-faint mt-1 mb-4">
          영문, 숫자, 특수문자를 모두 포함해 8자 이상
        </p>

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

      {/* 회원탈퇴 - 위험 구역 */}
      <div className="rounded-2xl border border-red-100 bg-red-50/40 p-6 mb-6">
        <h3 className="text-base font-semibold text-red-700 mb-1">회원탈퇴</h3>
        <p className="text-sm text-ink-muted mb-4">
          탈퇴 전 아래 내용을 꼭 확인해주세요.
        </p>

        {!withdrawOpen ? (
          <button
            onClick={() => setWithdrawOpen(true)}
            className="h-9 px-4 rounded-full border border-red-300 text-sm text-red-600 hover:bg-red-100"
          >
            회원탈퇴 진행하기
          </button>
        ) : (
          <div>
            <ul className="mb-4 space-y-1.5 text-sm text-ink leading-relaxed list-disc list-inside">
              <li>
                로그인 정보(이메일/비밀번호)가 즉시 삭제되며, 같은 이메일로도
                재로그인할 수 없어요.
              </li>
              <li>
                작성한 게시글·댓글·리뷰는 삭제되지 않지만, 더 이상 내 계정으로
                접근·수정할 수 없어요.
              </li>
              <li>
                찜 목록, 예약 현황, 준비 플랜 등 마이페이지의 모든 정보에 다시
                접근할 수 없어요.
              </li>
              <li>연동되어 있던 카카오 계정 연결도 함께 해지돼요.</li>
              <li>이 작업은 되돌릴 수 없어요.</li>
            </ul>

            <label className="block text-xs font-medium text-ink-muted mb-1.5">
              계속하려면 아래 칸에{" "}
              <span className="font-semibold text-red-600">"회원탈퇴"</span> 를
              정확히 입력해주세요.
            </label>
            <input
              type="text"
              value={confirmInput}
              onChange={(e) => setConfirmInput(e.target.value)}
              placeholder="회원탈퇴"
              className="w-full px-4 py-2.5 rounded-lg border border-red-200 bg-white text-sm outline-none focus:border-red-400 focus:ring-2 focus:ring-red-100 mb-3"
            />

            <div className="flex gap-2">
              <button
                onClick={() => {
                  setWithdrawOpen(false);
                  setConfirmInput("");
                }}
                className="h-9 px-4 rounded-full text-sm text-ink-muted hover:bg-cream"
              >
                취소
              </button>
              <button
                disabled={confirmInput !== WITHDRAW_CONFIRM_TEXT || withdrawing}
                onClick={handleWithdraw}
                className="h-9 px-4 rounded-full bg-red-600 text-white text-sm font-semibold disabled:opacity-40 disabled:cursor-not-allowed hover:bg-red-700"
              >
                {withdrawing ? "처리 중..." : "탈퇴 확정"}
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between">
        <span className="font-hand text-sm text-ink-faint -rotate-2">
          언제든 다시 바꿀 수 있어요 :)
        </span>
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
