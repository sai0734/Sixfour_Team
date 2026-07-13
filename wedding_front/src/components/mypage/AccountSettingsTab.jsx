import { useEffect, useState } from "react";
import useCustomLogin from "../../hooks/useCustomLogin";
import {
  getSocialAccounts,
  unlinkSocialAccount,
  withdrawPost,
} from "../../api/authApi";
import { getKakaoLinkLink } from "../../api/kakaoAuthApi";

const WITHDRAW_CONFIRM_TEXT = "회원탈퇴";

const AccountSettingsTab = () => {
  const { loginState, doLogout, moveToPath } = useCustomLogin();

  const [linkedProviders, setLinkedProviders] = useState([]);
  const [loadingProviders, setLoadingProviders] = useState(true);

  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [confirmInput, setConfirmInput] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);

  useEffect(() => {
    if (!loginState.email) return;

    getSocialAccounts(loginState.email)
      .then((data) => setLinkedProviders(data.providers || []))
      .catch((err) => console.error(err))
      .finally(() => setLoadingProviders(false));
  }, [loginState.email]);

  const isKakaoLinked = linkedProviders.includes("kakao");

  const handleUnlinkKakao = () => {
    if (!window.confirm("카카오 계정 연동을 해제할까요?")) return;

    unlinkSocialAccount(loginState.email, "kakao")
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

  const handleWithdraw = () => {
    if (confirmInput !== WITHDRAW_CONFIRM_TEXT) return;

    setWithdrawing(true);

    withdrawPost(loginState.email)
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
    <div className="max-w-xl">
      {/* 소셜 계정 연동 */}
      <section className="mb-10">
        <h3 className="text-base font-semibold text-ink mb-1">
          소셜 계정 연동
        </h3>
        <p className="text-sm text-ink-muted mb-4">
          카카오 계정을 연동하면 다음부터 카카오로도 로그인할 수 있어요.
        </p>

        <div className="flex items-center justify-between rounded-xl border border-line px-4 py-3">
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
      </section>

      {/* 회원탈퇴 - 위험 구역 */}
      <section className="rounded-xl border border-red-100 bg-red-50/40 p-5">
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
                재가입 할 수 없어요.
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
      </section>
    </div>
  );
};

export default AccountSettingsTab;
