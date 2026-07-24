import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import ShopTapeLabel from "../../components/product/ShopTapeLabel";
import AiPlanLoadingModal from "../../components/aiplan/AiPlanLoadingModal";
import ResultCards from "../../components/aiplan/ResultCards";
import SessionHistoryPanel from "../../components/aiplan/SessionHistoryPanel";
import {
  getAiRecommendations,
  refineRecommendation,
  updateSlotStatus,
  getSessionResult,
  applySessionToPlan,
  getRefineHistory,
  viewSessionTurn,
} from "../../api/aiPlanApi";
import {
  getSessionHistory,
  addSessionHistoryEntry,
  renameSessionHistoryEntry,
  removeSessionHistoryEntry,
} from "../../api/aiPlanHistory";
import { showAlert } from "../../util/globalAlert";
import { showConfirm } from "../../util/globalConfirm";

// com.wedding.company.domain.HallType 그대로 - 한글 라벨만 붙임
const HALL_TYPE_OPTIONS = [
  { value: "", label: "상관없음" },
  { value: "GARDEN", label: "야외 / 가든" },
  { value: "CHAPEL", label: "채플 / 플라워홀" },
  { value: "HOTEL", label: "호텔" },
  { value: "HOUSE", label: "하우스웨딩" },
  { value: "GRAND", label: "그랜드홀" },
  { value: "CONVENTION", label: "컨벤션" },
  { value: "BANQUET", label: "연회장" },
];

// StudioDetail.themeTags 실제 더미데이터 값 그대로 - LIKE 매칭이라 여기 적힌 문자열과 정확히 같아야 함
const STUDIO_MOOD_OPTIONS = [
  { value: "", label: "상관없음" },
  { value: "인물중심", label: "인물중심" },
  { value: "클래식/럭셔리", label: "클래식/럭셔리" },
  { value: "트렌디/MZ", label: "트렌디/MZ" },
  { value: "내추럴/감성", label: "내추럴/감성" },
  { value: "유럽풍/로맨틱", label: "유럽풍/로맨틱" },
  { value: "모던/미니멀", label: "모던/미니멀" },
  { value: "스몰웨딩", label: "스몰웨딩" },
  { value: "야외/자연", label: "야외/자연" },
];

// DressItem.styleTags 실제 더미데이터에 쓰인 값 전부 - 마찬가지로 LIKE 매칭 대상 문자열 그대로
const DRESS_STYLE_OPTIONS = [
  { value: "", label: "상관없음" },
  { value: "셀프웨딩", label: "셀프웨딩" },
  { value: "미니드레스", label: "미니드레스" },
  { value: "청순", label: "청순" },
  { value: "로맨틱", label: "로맨틱" },
  { value: "스몰웨딩", label: "스몰웨딩" },
  { value: "화동", label: "화동" },
  { value: "A라인", label: "A라인" },
  { value: "벨라인", label: "벨라인" },
  { value: "머메이드", label: "머메이드" },
  { value: "클래식", label: "클래식" },
  { value: "우아한", label: "우아한" },
  { value: "글래머러스", label: "글래머러스" },
  { value: "모던", label: "모던" },
  { value: "미니멀", label: "미니멀" },
  { value: "본식", label: "본식" },
  { value: "만삭", label: "만삭" },
  { value: "파티", label: "파티" },
  { value: "앵클라인", label: "앵클라인" },
  { value: "오프숄더", label: "오프숄더" },
  { value: "실크", label: "실크" },
  { value: "수입실크", label: "수입실크" },
  { value: "비즈", label: "비즈" },
  { value: "화려한", label: "화려한" },
  { value: "맞춤제작", label: "맞춤제작" },
  { value: "2부", label: "2부" },
  { value: "콩쿨복", label: "콩쿨복" },
];

// com.wedding.company.domain.MakeupPackageType 그대로 - 한글 라벨만 붙임
const MAKEUP_TYPE_OPTIONS = [
  { value: "", label: "상관없음" },
  { value: "HAIR", label: "헤어만" },
  { value: "MAKEUP", label: "메이크업만" },
  { value: "NAIL", label: "네일만" },
  { value: "HAIR_MAKEUP", label: "헤어 + 메이크업" },
  { value: "HAIR_NAIL", label: "헤어 + 네일" },
  { value: "MAKEUP_NAIL", label: "메이크업 + 네일" },
  { value: "FULL", label: "풀세트 (헤어+메이크업+네일)" },
];

// 오늘부터 14일 뒤 날짜 - 결혼 날짜 입력 최소값 (준비 기간 최소 2주 확보)
const MIN_WEDDING_DATE = (() => {
  const d = new Date();
  d.setDate(d.getDate() + 14);
  return d.toISOString().slice(0, 10);
})();

// 다듬기 텍스트를 아예 처음부터 쓰기 막막해하는 경우가 많아서 자주 쓸 법한 문구를 칩으로 준비해둠
const REFINE_SUGGESTIONS = [
  "스튜디오는 빼줘",
  "드레스 다른 스타일로 다시 찾아줘",
  "홀은 마음에 들어서 확정할게",
  "예산 좀 더 늘려서 다시 찾아줘",
];

// 다른 페이지 갔다가 AI 웨딩플랜으로 돌아와도(=URL에 sessionId 없이 새로 진입해도) 마지막으로
// 보던 세션을 이어볼 수 있게, URL과 별개로 브라우저에 마지막 sessionId를 하나 더 남겨둔다.
const LAST_SESSION_KEY = "aiplan_last_session_id";

const DetailPlanPage = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [form, setForm] = useState({
    budgetManwon: searchParams.get("budgetManwon") || "",
    region: searchParams.get("region") || "",
    groomName: searchParams.get("groomName") || "",
    brideName: searchParams.get("brideName") || "",
    weddingDate: searchParams.get("weddingDate") || "",
    guestCount: "",
    hallTypes: [],
    studioMoods: [],
    dressStyles: [],
    makeupStyle: "",
    freeText: "",
  });
  const [result, setResult] = useState(null);
  // URL에 sessionId가 있거나, 예전에 저장해둔 마지막 세션이 있으면 아래 복원 useEffect가
  // 바로 fetch를 시작하므로, 첫 렌더부터 로딩 상태로 시작한다
  // (effect 안에서 setLoading(true)를 동기 호출하지 않기 위함)
  const [loading, setLoading] = useState(() =>
    Boolean(
      searchParams.get("sessionId") || localStorage.getItem(LAST_SESSION_KEY),
    ),
  );
  const [error, setError] = useState(null);
  const [retryAction, setRetryAction] = useState(null);
  const [linkCopied, setLinkCopied] = useState(false);

  const [refineOpen, setRefineOpen] = useState(false);
  const [refineText, setRefineText] = useState("");
  const [refineLoading, setRefineLoading] = useState(false);

  // 상단 조합 히스토리 배지 - "첫 추천 조합" + 다듬을 때마다 오른쪽으로 늘어나는 턴 목록.
  // activeTurnNo는 지금 화면에 보이는 조합이 몇 번째 턴인지 - 실제 변경 액션 뒤엔 항상 최신
  // 턴이 되고, 배지를 눌러 예전 턴을 "보기"만 했을 때는 그 턴 번호로 따로 고정된다.
  const [turns, setTurns] = useState([]);
  const [activeTurnNo, setActiveTurnNo] = useState(null);

  // 추천 회차 사이드바 - "1번째 추천/2번째 추천" 목록. 순전히 이 브라우저 안에서만 의미있는
  // 라벨이라 서버가 아니라 localStorage에 둔다 (aiPlanHistory.js 참고).
  const [history, setHistory] = useState(() => getSessionHistory());
  // 모바일에서 사이드바가 화면을 다 차지하지 않도록 기본은 접어둠 (데스크톱은 md:block으로 항상 보임)
  const [historyOpen, setHistoryOpen] = useState(false);

  // "이 조합으로 예약 진행" 큐를 다 돌고 나면 ReservationReserveComponent가 여기로
  // ?reserved=1을 붙여서 돌려보낸다. URL에 계속 남겨두면 이후 액션(다듬기 등)에도
  // applyResult가 그대로 들고 다녀서 배너가 안 사라지므로, 한 번 본 뒤 URL에서는 지우고
  // 화면 표시는 별도 state로 유지한다.
  const [showReservedBanner, setShowReservedBanner] = useState(
    () => searchParams.get("reserved") === "1",
  );

  // 결과에 sessionId가 생기면 URL/localStorage/회차 목록에 다 반영해둔다.
  // URL은 "새로고침"(같은 주소 유지) 복원용, localStorage(LAST_SESSION_KEY)는 "다른 페이지 갔다가
  // 돌아오기" 복원용, 회차 목록은 새 sessionId일 때만 새 항목이 늘어난다(다듬기/확정은 같은
  // sessionId를 재사용하므로 목록이 늘지 않음).
  // viewedTurnNo는 "배지로 예전 턴을 보기만 했을 때"만 넘어옴 - 그 외(확정/다시찾기/다듬기/
  // 되돌리기/새 추천)는 항상 방금 생긴 최신 턴이 화면에 보이는 턴이 된다.
  const applyResult = (data, meta, viewedTurnNo) => {
    setResult(data);
    if (data?.sessionId) {
      localStorage.setItem(LAST_SESSION_KEY, String(data.sessionId));
      setHistory(addSessionHistoryEntry(data.sessionId, meta));
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          next.set("sessionId", data.sessionId);
          return next;
        },
        { replace: true },
      );

      getRefineHistory(data.sessionId)
        .then((list) => {
          setTurns(list);
          if (viewedTurnNo != null) {
            setActiveTurnNo(viewedTurnNo);
          } else {
            setActiveTurnNo(list.length > 0 ? list[list.length - 1].turnNo : 0);
          }
        })
        .catch((err) => console.error(err));
    }
  };

  // 상단 히스토리 배지 클릭 - 그 턴의 조합을 그대로 불러온다 (새 턴을 만들지 않음)
  const handleSelectTurn = (turnNo) => {
    if (!result?.sessionId || turnNo === activeTurnNo) return Promise.resolve();

    return viewSessionTurn(result.sessionId, turnNo)
      .then((data) => applyResult(data, undefined, turnNo))
      .catch((err) => {
        console.error(err);
        setError("이전 조합을 불러오는 중 문제가 발생했어요.");
      });
  };

  // 복원 - URL에 sessionId가 있으면 그걸, 없으면 localStorage에 남은 마지막 세션을 불러옴
  useEffect(() => {
    const sessionId =
      searchParams.get("sessionId") || localStorage.getItem(LAST_SESSION_KEY);
    if (!sessionId || result) {
      return;
    }

    getSessionResult(sessionId)
      .then((data) => applyResult(data))
      .catch((err) => {
        console.error(err);
        localStorage.removeItem(LAST_SESSION_KEY);
        setError("이전 결과를 불러오지 못했어요. 조건을 다시 입력해주세요.");
        setRetryAction(null);
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // reserved=1은 한 번 보여주기용 신호일 뿐이라, URL에서는 바로 지운다 (배너 자체는 state로 유지됨)
  useEffect(() => {
    if (searchParams.get("reserved") === "1") {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          next.delete("reserved");
          return next;
        },
        { replace: true },
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  // 칩 하나 클릭 = 단일 선택 (메이크업 패키지 전용). 이미 선택된 칩을 다시 누르면 "상관없음"(빈 값)으로 해제.
  const handleChipSelect = (field, value) => {
    setForm((prev) => ({
      ...prev,
      [field]: prev[field] === value ? "" : value,
    }));
  };

  // 칩 여러 개 클릭 = 중복 선택 (웨딩홀/스튜디오/드레스). "상관없음"을 누르면 전체 해제,
  // 그 외 칩은 이미 선택돼 있으면 빼고 아니면 추가.
  const handleChipMultiSelect = (field, value) => {
    if (value === "") {
      setForm((prev) => ({ ...prev, [field]: [] }));
      return;
    }
    setForm((prev) => {
      const current = prev[field];
      const next = current.includes(value)
        ? current.filter((v) => v !== value)
        : [...current, value];
      return { ...prev, [field]: next };
    });
  };

  // 자세히 모드로 넘어왔다가 다시 빠르게 모드로 돌아가기 - 입력해둔 공통 4개는 쿼리로 들고 감.
  // 지금 세션을 이어보던 흐름은 아니므로 마지막 세션 기억도 같이 지워서, 빠르게 모드가
  // 다시 이쪽으로 자동 복귀시키지 않게 한다.
  const goQuickMode = () => {
    localStorage.removeItem(LAST_SESSION_KEY);
    const params = new URLSearchParams({
      budgetManwon: form.budgetManwon || "",
      region: form.region || "",
      groomName: form.groomName || "",
      brideName: form.brideName || "",
      weddingDate: form.weddingDate || "",
    });
    navigate(`/aiplan/quick?${params.toString()}`);
  };

  const buildPayload = (overrideBudgetManwon) => ({
    budget: Number(overrideBudgetManwon ?? form.budgetManwon) * 10000,
    region: form.region,
    groomName: form.groomName || null,
    brideName: form.brideName || null,
    weddingDate: form.weddingDate || null,
    guestCount: form.guestCount ? Number(form.guestCount) : null,
    hallType: form.hallTypes.length ? form.hallTypes.join(",") : null,
    studioMood: form.studioMoods.length ? form.studioMoods.join(",") : null,
    dressStyle: form.dressStyles.length ? form.dressStyles.join(",") : null,
    makeupStyle: form.makeupStyle || null,
    freeText: form.freeText || null,
  });

  // submitAi는 "다시 시도" 버튼에서도 그대로 재호출할 수 있게 이벤트 객체 없이 독립시켰다.
  // overrideBudgetManwon은 "예산 늘려서 다시 찾기" 전용 - form 상태 업데이트를 기다리지 않고 바로 그
  // 값으로 요청하기 위해 받는다.
  const submitAi = (overrideBudgetManwon) => {
    const budgetManwon = overrideBudgetManwon ?? form.budgetManwon;
    if (!budgetManwon || !form.region) {
      setError("총 예산과 지역은 필수로 입력해주세요.");
      return;
    }

    setError(null);
    setRetryAction(null);
    setLoading(true);

    getAiRecommendations(buildPayload(overrideBudgetManwon))
      .then((data) => applyResult(data, { budgetManwon, region: form.region }))
      .catch((err) => {
        console.error(err);
        setError(
          "AI 추천을 불러오는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
        );
        setRetryAction(() => submitAi);
      })
      .finally(() => setLoading(false));
  };

  const handleSubmitAi = (e) => {
    e.preventDefault();
    submitAi();
  };

  // 결과 화면의 "예산 늘려서 다시 찾기" 버튼 - suggestedBudget(원)을 만원 단위로 바꿔 폼에도
  // 반영해두고 그 예산으로 재요청한다.
  const handleBumpBudget = (suggestedBudgetWon) => {
    const manwon = String(Math.ceil(suggestedBudgetWon / 10000));
    setForm((prev) => ({ ...prev, budgetManwon: manwon }));
    submitAi(manwon);
  };

  const handleReset = () => {
    setResult(null);
    setError(null);
    setRetryAction(null);
    setRefineOpen(false);
    setRefineText("");
    localStorage.removeItem(LAST_SESSION_KEY);
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.delete("sessionId");
        return next;
      },
      { replace: true },
    );
  };

  const submitRefine = () => {
    if (!refineText.trim() || !result?.sessionId) {
      return;
    }

    setError(null);
    setRetryAction(null);
    setRefineLoading(true);

    refineRecommendation({
      sessionId: result.sessionId,
      message: refineText.trim(),
    })
      .then((data) => {
        applyResult(data);
        setRefineText("");
      })
      .catch((err) => {
        console.error(err);
        setError(
          "다듬기 요청 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
        );
        setRetryAction(() => submitRefine);
      })
      .finally(() => setRefineLoading(false));
  };

  const handleRefineSubmit = (e) => {
    e.preventDefault();
    submitRefine();
  };

  // 다듬기 텍스트를 처음부터 다 안 써도 되게, 자주 쓸 법한 문구를 눌러서 채워넣을 수 있게 함
  const appendRefineSuggestion = (suggestion) => {
    setRefineText((prev) =>
      prev.trim() ? `${prev.trim()} ${suggestion}` : suggestion,
    );
  };

  const handleSlotAction = (category, action) => {
    if (!result?.sessionId) return Promise.resolve();

    return updateSlotStatus({ sessionId: result.sessionId, category, action })
      .then((data) => applyResult(data))
      .catch((err) => {
        console.error(err);
        setError("확정 처리 중 문제가 발생했어요.");
        setRetryAction(null);
      });
  };

  // "이 결과 마이페이지에 담기" - 추천 도중 자동 반영이 아니라 사용자가 명시적으로 눌렀을 때만
  // 웨딩플랜/예산관리/체크리스트에 반영한다. 기존에 저장해둔 예식일·총예산·예식장이 있으면
  // 덮어써지므로, 실행 전에 한 번 더 확인받는다.
  const handleApplyToPlan = async () => {
    if (!result?.sessionId) return;

    if (
      !(await showConfirm(
        "기존에 저장된 예식일 · 총예산 · 예식장 정보가 있다면 덮어씌워져요.",
      ))
    ) {
      return;
    }

    applySessionToPlan(result.sessionId).catch((err) => {
      console.error(err);
      const msg = err?.response?.data?.msg;
      showAlert(
        msg || "반영 중 문제가 발생했어요. 로그인 상태를 확인해주세요.",
      );
    });
  };

  // 사이드바에서 예전 회차를 클릭하면 그 세션을 다시 불러옴
  const handleSelectHistory = (sessionId) => {
    if (result?.sessionId && String(result.sessionId) === sessionId) {
      return;
    }

    setError(null);
    setRetryAction(null);
    setRefineOpen(false);
    setLoading(true);

    getSessionResult(sessionId)
      .then((data) => applyResult(data))
      .catch((err) => {
        console.error(err);
        setError("그 회차 결과를 불러오지 못했어요.");
      })
      .finally(() => setLoading(false));
  };

  // 지금 보고 있는 조합의 링크를 복사 - sessionId가 URL에 있어서, 이 링크를 받은 사람도
  // 같은 조합을 그대로 보고(+ 다듬기/확정도 함께) 이어볼 수 있다.
  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.href).then(() => {
      setLinkCopied(true);
      setTimeout(() => setLinkCopied(false), 1500);
    });
  };

  const handleRenameHistory = (sessionId, label) => {
    setHistory(renameSessionHistoryEntry(sessionId, label));
  };

  // 기록에서만 지움 - 서버 세션은 그대로 둔다. 지금 보고 있던 회차를 지웠으면 폼으로 돌아간다.
  const handleRemoveHistory = (sessionId) => {
    setHistory(removeSessionHistoryEntry(sessionId));
    if (result?.sessionId && String(result.sessionId) === sessionId) {
      handleReset();
    }
  };

  const inputClass =
    "w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark";
  const labelClass = "mb-1 block text-sm font-medium text-ink-soft";

  return (
    <div className="-mx-5 -mb-10 min-h-[calc(100vh-6rem)] bg-cream px-5">
      {/* 답례품(GIFT SHOP) 페이지와 같은 구조의 상단 히어로 배너 - 배경 이미지는
          public/aiplan-hero.jpg 자리에 넣으면 바로 반영됨 (지금은 빈 자리만 잡아둠). */}
      <section
        className="relative -mx-5 -mt-12 bg-cover bg-center pb-10 pt-16 text-center md:pb-12"
        style={{ backgroundImage: "url('/aiplan-hero.jpg')" }}
      >
        <div className="absolute inset-0 bg-black/45" />
        <div className="relative z-10 mx-auto max-w-[720px] px-5">
          <ShopTapeLabel tone="white" className="mb-5">
            01 — AI WEDDING PLAN
          </ShopTapeLabel>
          <h1 className="mb-2.5 font-['Gowun_Batang'] text-2xl leading-snug text-white md:mb-3.5 md:text-4xl">
            예산부터 취향까지, 한 번에 맞추는 웨딩플랜
          </h1>
          <p className="whitespace-pre-line text-sm leading-relaxed text-white/85 md:text-[15px]">
            홀·스튜디오·드레스·메이크업까지{"\n"}조건에 맞게 골라 조합해드려요
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-[1160px] px-4 py-10">
        {loading && (
          <AiPlanLoadingModal
            message={
              result ? "불러오는 중이에요" : "조건에 맞는 곳을 찾고 있어요"
            }
          />
        )}

        <div className="flex flex-col gap-6 md:flex-row">
          {history.length > 0 && (
            <aside className="w-full shrink-0 md:w-56 md:sticky md:top-6 md:self-start">
              <button
                type="button"
                onClick={() => setHistoryOpen((prev) => !prev)}
                className="mb-2 flex w-full items-center justify-between rounded-xl border border-line bg-white px-4 py-2.5 text-sm font-medium text-ink-soft md:hidden"
              >
                <span>추천 기록 ({history.length})</span>
                <span>{historyOpen ? "숨기기 ▲" : "펼치기 ▼"}</span>
              </button>
              <div className={`${historyOpen ? "block" : "hidden"} md:block`}>
                <SessionHistoryPanel
                  history={history}
                  activeSessionId={result?.sessionId}
                  onSelect={handleSelectHistory}
                  onRename={handleRenameHistory}
                  onRemove={handleRemoveHistory}
                  onStartNew={handleReset}
                  disabled={loading}
                />
              </div>
            </aside>
          )}

          <div className="mx-auto w-full max-w-[900px]">
            {showReservedBanner && (
              <div className="mb-4 flex items-center justify-between gap-3 rounded-xl border border-brand-dark bg-blush-50 px-4 py-3 text-sm text-brand-deep">
                <span>
                  이 조합으로 예약 신청을 모두 마쳤어요. 업체 확인 후 결제를
                  진행하실 수 있어요.
                </span>
                <button
                  type="button"
                  onClick={() => setShowReservedBanner(false)}
                  aria-label="닫기"
                  className="shrink-0 text-brand-deep hover:opacity-70"
                >
                  ×
                </button>
              </div>
            )}

            {!result && (
              <div className="mb-8 text-center">
                <p className="text-sm text-ink-muted">
                  비워두면 취향 없이, 입력한 항목만 반영해서 추천해드려요
                </p>
              </div>
            )}

            {!result ? (
              <form
                onSubmit={handleSubmitAi}
                className="rounded-2xl border border-line bg-white p-6 shadow-sm md:p-8"
              >
                <p className="mb-3 text-xs font-medium text-ink-muted">
                  공통 정보
                </p>
                <div className="mb-6 grid grid-cols-1 gap-5 border-b border-line pb-6 md:grid-cols-2">
                  <div>
                    <label className={labelClass}>총 예산 (만원) *</label>
                    <input
                      type="number"
                      min="0"
                      value={form.budgetManwon}
                      onChange={handleChange("budgetManwon")}
                      placeholder="예: 3000"
                      className={inputClass}
                    />
                  </div>

                  <div>
                    <label className={labelClass}>희망 지역 *</label>
                    <input
                      type="text"
                      value={form.region}
                      onChange={handleChange("region")}
                      placeholder="예: 강남"
                      className={inputClass}
                    />
                  </div>

                  <div>
                    <label className={labelClass}>신랑 이름</label>
                    <input
                      type="text"
                      value={form.groomName}
                      onChange={handleChange("groomName")}
                      className={inputClass}
                    />
                  </div>

                  <div>
                    <label className={labelClass}>신부 이름</label>
                    <input
                      type="text"
                      value={form.brideName}
                      onChange={handleChange("brideName")}
                      className={inputClass}
                    />
                  </div>

                  <div>
                    <label className={labelClass}>결혼 날짜</label>
                    <input
                      type="date"
                      min={MIN_WEDDING_DATE}
                      value={form.weddingDate}
                      onChange={handleChange("weddingDate")}
                      className={inputClass}
                    />
                  </div>

                  <div>
                    <label className={labelClass}>하객수 (명)</label>
                    <input
                      type="number"
                      min="0"
                      value={form.guestCount}
                      onChange={handleChange("guestCount")}
                      placeholder="예: 200"
                      className={inputClass}
                    />
                  </div>
                </div>

                <p className="mb-3 text-xs font-medium text-ink-muted">
                  카테고리별 취향 (전부 선택 사항)
                </p>
                <div className="space-y-4">
                  <div className="rounded-xl border border-line bg-white px-4 py-3.5">
                    <label className={labelClass}>홀 분위기</label>
                    <p className="mb-2 text-xs text-ink-muted">
                      여러 개 선택하면 그 중 하나라도 맞는 홀을 찾아요
                    </p>
                    <div className="flex flex-wrap gap-1.5">
                      {HALL_TYPE_OPTIONS.map((opt) => (
                        <button
                          key={opt.value || "none"}
                          type="button"
                          onClick={() =>
                            handleChipMultiSelect("hallTypes", opt.value)
                          }
                          className={
                            (
                              opt.value === ""
                                ? form.hallTypes.length === 0
                                : form.hallTypes.includes(opt.value)
                            )
                              ? "rounded-full border border-brand-dark bg-brand px-3 py-1 text-xs text-white"
                              : "rounded-full border border-line bg-white px-3 py-1 text-xs text-ink-soft hover:bg-blush-100"
                          }
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-xl border border-line bg-white px-4 py-3.5">
                    <label className={labelClass}>스튜디오 분위기</label>
                    <p className="mb-2 text-xs text-ink-muted">
                      여러 개 선택하면 전부 가진 곳을 찾아요
                    </p>
                    <div className="flex flex-wrap gap-1.5">
                      {STUDIO_MOOD_OPTIONS.map((opt) => (
                        <button
                          key={opt.value || "none"}
                          type="button"
                          onClick={() =>
                            handleChipMultiSelect("studioMoods", opt.value)
                          }
                          className={
                            (
                              opt.value === ""
                                ? form.studioMoods.length === 0
                                : form.studioMoods.includes(opt.value)
                            )
                              ? "rounded-full border border-brand-dark bg-brand px-3 py-1 text-xs text-white"
                              : "rounded-full border border-line bg-white px-3 py-1 text-xs text-ink-soft hover:bg-blush-100"
                          }
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-xl border border-line bg-white px-4 py-3.5">
                    <label className={labelClass}>드레스 스타일</label>
                    <p className="mb-2 text-xs text-ink-muted">
                      여러 개 선택하면 전부 가진 아이템을 찾아요
                    </p>
                    <div className="flex flex-wrap gap-1.5">
                      {DRESS_STYLE_OPTIONS.map((opt) => (
                        <button
                          key={opt.value || "none"}
                          type="button"
                          onClick={() =>
                            handleChipMultiSelect("dressStyles", opt.value)
                          }
                          className={
                            (
                              opt.value === ""
                                ? form.dressStyles.length === 0
                                : form.dressStyles.includes(opt.value)
                            )
                              ? "rounded-full border border-brand-dark bg-brand px-3 py-1 text-xs text-white"
                              : "rounded-full border border-line bg-white px-3 py-1 text-xs text-ink-soft hover:bg-blush-100"
                          }
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-xl border border-line bg-white px-4 py-3.5">
                    <label className={labelClass}>메이크업 패키지</label>
                    <p className="mb-2 text-xs text-ink-muted">
                      하나만 선택할 수 있어요
                    </p>
                    <div className="flex flex-wrap gap-1.5">
                      {MAKEUP_TYPE_OPTIONS.map((opt) => (
                        <button
                          key={opt.value || "none"}
                          type="button"
                          onClick={() =>
                            handleChipSelect("makeupStyle", opt.value)
                          }
                          className={
                            form.makeupStyle === opt.value
                              ? "rounded-full border border-brand-dark bg-brand px-3 py-1 text-xs text-white"
                              : "rounded-full border border-line bg-white px-3 py-1 text-xs text-ink-soft hover:bg-blush-100"
                          }
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                <div className="mt-5 rounded-xl border border-dashed border-line bg-surface px-4 py-3.5">
                  <label className={labelClass}>
                    AI에게 추가로 요청하고 싶은 말
                  </label>
                  <p className="mb-2 text-xs text-ink-muted">
                    위 칩으로 고르기 애매한 요청은 여기 자유롭게 적어주세요.
                    'AI에게 맡기기'로 추천받을 때만 반영돼요.
                  </p>
                  <textarea
                    value={form.freeText}
                    onChange={handleChange("freeText")}
                    rows={3}
                    placeholder="예: 하객이 많아서 넓은 홀이었으면 좋겠어요"
                    className={inputClass}
                  />
                </div>

                {error && (
                  <div className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-[#F0C4C4] bg-[#FDEEEE] px-4 py-2.5 text-sm text-[#B23B3B]">
                    <span>{error}</span>
                    {retryAction && (
                      <button
                        type="button"
                        onClick={() => retryAction()}
                        className="shrink-0 font-medium underline underline-offset-2"
                      >
                        다시 시도
                      </button>
                    )}
                  </div>
                )}

                <div className="mt-6 flex flex-col gap-3 md:flex-row">
                  <button
                    type="button"
                    onClick={goQuickMode}
                    className="h-12 flex-1 rounded-full border border-line text-sm font-medium text-ink-soft hover:bg-surface"
                  >
                    빠르게 추천받기
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="h-12 flex-1 rounded-full bg-brand text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-60"
                  >
                    AI에게 맡기기
                  </button>
                </div>
              </form>
            ) : (
              <div>
                <ResultCards
                  result={result}
                  onSlotAction={handleSlotAction}
                  onBumpBudget={handleBumpBudget}
                  onApplyToPlan={handleApplyToPlan}
                  turns={turns}
                  activeTurnNo={activeTurnNo}
                  onSelectTurn={handleSelectTurn}
                />

                {error && (
                  <div className="mt-4 flex flex-wrap items-center justify-center gap-2 rounded-xl border border-[#F0C4C4] bg-[#FDEEEE] px-4 py-2.5 text-sm text-[#B23B3B]">
                    <span>{error}</span>
                    {retryAction && (
                      <button
                        type="button"
                        onClick={() => retryAction()}
                        className="shrink-0 font-medium underline underline-offset-2"
                      >
                        다시 시도
                      </button>
                    )}
                  </div>
                )}

                {/* 조합이 마음에 안 들면 여기서 바로 - 버튼을 누르는 자리 바로 아래에서 폼이
                  펼쳐지게 해서, 예전처럼 페이지 맨 아래 버튼을 누르면 훨씬 위쪽에 폼이
                  나타나는 어색함을 없앴다. */}
                {result.sessionId && (
                  <div className="mt-6 rounded-2xl border border-line bg-white p-5">
                    <button
                      type="button"
                      onClick={() => setRefineOpen((prev) => !prev)}
                      className="flex w-full items-center justify-between text-left"
                    >
                      <span>
                        <span className="block text-sm font-semibold text-ink">
                          마음에 안 드는 부분이 있나요?
                        </span>
                        <span className="block text-xs text-ink-faint">
                          자유롭게 말씀해주시면 그 부분만 다시 찾아드려요
                        </span>
                      </span>
                      <span className="shrink-0 rounded-full bg-brand px-5 py-2 text-sm font-medium text-white">
                        {refineOpen ? "닫기" : "플랜 수정하기"}
                      </span>
                    </button>

                    {refineOpen && (
                      <form
                        onSubmit={handleRefineSubmit}
                        className="mt-4 border-t border-line pt-4"
                      >
                        <div className="mb-2 flex flex-wrap gap-1.5">
                          {REFINE_SUGGESTIONS.map((suggestion) => (
                            <button
                              key={suggestion}
                              type="button"
                              onClick={() => appendRefineSuggestion(suggestion)}
                              className="rounded-full border border-line bg-white px-3 py-1 text-xs text-ink-soft hover:bg-blush-100"
                            >
                              {suggestion}
                            </button>
                          ))}
                        </div>
                        <textarea
                          value={refineText}
                          onChange={(e) => setRefineText(e.target.value)}
                          rows={3}
                          placeholder="예: 스튜디오는 예산 초과라서 빼고 나머지로 추천해줘. 홀이랑 메이크업은 마음에 들어서 확정, 드레스만 다른 스타일로 다시 찾아줘"
                          className="w-full rounded-xl border border-line bg-surface px-4 py-2.5 text-sm text-ink outline-none focus:border-brand-dark"
                        />
                        <div className="mt-3 flex flex-wrap gap-2">
                          <button
                            type="submit"
                            disabled={refineLoading || !refineText.trim()}
                            className="h-10 rounded-full bg-brand px-5 text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-60"
                          >
                            {refineLoading ? "반영하는 중..." : "보내기"}
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                )}

                {/* 새로 시작하거나 공유하는 건 부가 기능이라, 위 핵심 액션들보다 눈에 덜 띄게
                  아래쪽에 작은 텍스트 링크 느낌으로 둔다. */}
                <div className="mt-6 flex flex-col items-center gap-3 text-sm text-ink-muted md:flex-row md:justify-center md:gap-6">
                  <button
                    type="button"
                    onClick={handleReset}
                    className="hover:text-ink-soft hover:underline"
                  >
                    다른 조건으로 새로 찾기
                  </button>
                  {result.sessionId && (
                    <button
                      type="button"
                      onClick={handleCopyLink}
                      className="hover:text-ink-soft hover:underline"
                    >
                      {linkCopied ? "복사됐어요!" : "링크로 공유하기"}
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DetailPlanPage;
