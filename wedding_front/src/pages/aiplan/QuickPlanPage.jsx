import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import ShopTapeLabel from "../../components/product/ShopTapeLabel";
import AiPlanLoadingModal from "../../components/aiplan/AiPlanLoadingModal";
import ResultCards from "../../components/aiplan/ResultCards";
import { getQuickRecommendations } from "../../api/aiPlanApi";

// DetailPlanPage.jsx와 동일한 키 - 자세히/AI 모드 결과를 이어서 볼 수 있게 저장해둔 마지막 세션 id
const LAST_SESSION_KEY = "aiplan_last_session_id";

// 오늘부터 14일 뒤 날짜 - 결혼 날짜 입력 최소값 (준비 기간 최소 2주 확보)
const MIN_WEDDING_DATE = (() => {
  const d = new Date();
  d.setDate(d.getDate() + 14);
  return d.toISOString().slice(0, 10);
})();

const QuickPlanPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // 자세히 모드에서 "빠르게 모드로 돌아가기"를 누르면 공통 필수 4개를 쿼리로 들고 와서 이어 쓴다
  const [form, setForm] = useState({
    budgetManwon: searchParams.get("budgetManwon") || "",
    region: searchParams.get("region") || "",
    groomName: searchParams.get("groomName") || "",
    brideName: searchParams.get("brideName") || "",
    weddingDate: searchParams.get("weddingDate") || "",
    guestCount: "",
  });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [retryAction, setRetryAction] = useState(null);

  // 마지막 세션이 있으면 아래 useEffect가 곧장 다른 페이지(자세히 모드)로 튕겨낸다. 그 판단이
  // 끝날 때까지는 빈 폼이 잠깐 번쩍이지 않도록 로딩 오버레이로 가려둔다.
  const [redirecting] = useState(() => Boolean(localStorage.getItem(LAST_SESSION_KEY)));

  // "AI 웨딩플랜" 메뉴는 항상 여기(/aiplan/quick)로 먼저 들어오게 라우팅돼 있다. 다른 페이지 갔다가
  // 돌아왔을 때도 마지막으로 보던 자세히/AI 모드 결과가 있으면 빈 폼 대신 그걸로 바로 이어준다.
  useEffect(() => {
    const lastSessionId = localStorage.getItem(LAST_SESSION_KEY);
    if (lastSessionId) {
      navigate(`/aiplan/detail?sessionId=${lastSessionId}`, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  // submitQuick은 재사용 가능한 "다시 시도" 대상이라 이벤트 객체 없이 독립적으로 동작하게 뺐다.
  // overrideBudgetManwon은 "예산 늘려서 다시 찾기" 전용 - form 상태 업데이트를 기다리지 않고 바로
  // 그 값으로 요청하기 위해 받는다.
  const submitQuick = (overrideBudgetManwon) => {
    const budgetManwon = overrideBudgetManwon ?? form.budgetManwon;
    if (!budgetManwon || !form.region) {
      setError("총 예산과 지역은 필수로 입력해주세요.");
      return;
    }

    setError(null);
    setRetryAction(null);
    setLoading(true);

    getQuickRecommendations({
      budget: Number(budgetManwon) * 10000,
      region: form.region,
      groomName: form.groomName || null,
      brideName: form.brideName || null,
      weddingDate: form.weddingDate || null,
      guestCount: form.guestCount ? Number(form.guestCount) : null,
    })
      .then((data) => setResult(data))
      .catch((err) => {
        console.error(err);
        setError(
          "추천을 불러오는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
        );
        setRetryAction(() => submitQuick);
      })
      .finally(() => setLoading(false));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    submitQuick();
  };

  // 결과 화면의 "예산 늘려서 다시 찾기" 버튼 - suggestedBudget(원)을 만원 단위로 바꿔 폼에도
  // 반영해두고 그 예산으로 재요청한다.
  const handleBumpBudget = (suggestedBudgetWon) => {
    const manwon = String(Math.ceil(suggestedBudgetWon / 10000));
    setForm((prev) => ({ ...prev, budgetManwon: manwon }));
    submitQuick(manwon);
  };

  const goDetailMode = () => {
    // 공통 필수 4개는 자세히 모드 화면에서 이어서 쓸 수 있게 쿼리로 넘겨줌
    const params = new URLSearchParams({
      budgetManwon: form.budgetManwon || "",
      region: form.region || "",
      groomName: form.groomName || "",
      brideName: form.brideName || "",
      weddingDate: form.weddingDate || "",
    });
    navigate(`/aiplan/detail?${params.toString()}`);
  };

  const handleReset = () => {
    setResult(null);
    setError(null);
  };

  return (
    <div className="-mx-5 -mb-10 min-h-[calc(100vh-6rem)] bg-cream px-5">
      {/* DetailPlanPage.jsx와 동일한 히어로 배너 - 배경 이미지는 public/aiplan-hero.jpg
          자리에 넣으면 바로 반영됨 (지금은 빈 자리만 잡아둠). */}
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

      <div className="mx-auto max-w-[900px] px-4 py-10">
      {(loading || redirecting) && (
        <AiPlanLoadingModal
          message={redirecting ? "이어서 보여드릴게요" : "지금 등록된 업체 중에서 찾고 있어요"}
        />
      )}

      {!result && (
        <div className="mb-8 text-center">
          <p className="text-sm text-ink-muted">
            AI 호출 없이, 지금 등록된 업체/패키지 중에서 바로 추천해드려요
          </p>
        </div>
      )}

      {!result ? (
        <form
          onSubmit={handleSubmit}
          className="rounded-2xl border border-line bg-white p-6 shadow-sm md:p-8"
        >
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft">
                총 예산 (만원) *
              </label>
              <input
                type="number"
                min="0"
                value={form.budgetManwon}
                onChange={handleChange("budgetManwon")}
                placeholder="예: 3000"
                className="w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft">
                희망 지역 *
              </label>
              <input
                type="text"
                value={form.region}
                onChange={handleChange("region")}
                placeholder="예: 강남"
                className="w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft">
                신랑 이름
              </label>
              <input
                type="text"
                value={form.groomName}
                onChange={handleChange("groomName")}
                className="w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft">
                신부 이름
              </label>
              <input
                type="text"
                value={form.brideName}
                onChange={handleChange("brideName")}
                className="w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft">
                결혼 날짜
              </label>
              <input
                type="date"
                min={MIN_WEDDING_DATE}
                value={form.weddingDate}
                onChange={handleChange("weddingDate")}
                className="w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-ink-soft">
                하객수 (명)
              </label>
              <input
                type="number"
                min="0"
                value={form.guestCount}
                onChange={handleChange("guestCount")}
                placeholder="예: 200"
                className="w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark"
              />
            </div>
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
              type="submit"
              disabled={loading}
              className="h-12 flex-1 rounded-full bg-brand text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-60"
            >
              빠르게 추천받기
            </button>
            <button
              type="button"
              onClick={goDetailMode}
              className="h-12 flex-1 rounded-full border border-line text-sm font-medium text-ink-soft hover:bg-surface"
            >
              자세히 설정하기 →
            </button>
          </div>
        </form>
      ) : (
        <div>
          <ResultCards result={result} onBumpBudget={handleBumpBudget} />

          <div className="mt-8 text-center">
            <button
              type="button"
              onClick={handleReset}
              className="h-11 rounded-full border border-line px-6 text-sm font-medium text-ink-soft hover:bg-surface"
            >
              조건 다시 입력하기
            </button>
          </div>
        </div>
      )}
      </div>
    </div>
  );
};

export default QuickPlanPage;
