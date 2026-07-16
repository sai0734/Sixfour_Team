import { useState } from "react";
import { useNavigate } from "react-router-dom";
import TapeLabel from "../../components/common/TapeLabel";
import FetchingModal from "../../components/common/FetchingModal";
import ResultCards from "../../components/aiplan/ResultCards";
import { getQuickRecommendations } from "../../api/aiPlanApi";

const initForm = {
  budgetManwon: "", // 화면 입력은 "만원" 단위, API로 보낼 땐 *10000
  region: "",
  groomName: "",
  brideName: "",
  weddingDate: "",
};

const QuickPlanPage = () => {
  const navigate = useNavigate();

  const [form, setForm] = useState(initForm);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!form.budgetManwon || !form.region) {
      setError("총 예산과 지역은 필수로 입력해주세요.");
      return;
    }

    setError(null);
    setLoading(true);

    getQuickRecommendations({
      budget: Number(form.budgetManwon) * 10000,
      region: form.region,
      groomName: form.groomName || null,
      brideName: form.brideName || null,
      weddingDate: form.weddingDate || null,
    })
      .then((data) => setResult(data))
      .catch((err) => {
        console.error(err);
        setError(
          "추천을 불러오는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
        );
      })
      .finally(() => setLoading(false));
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
    <div className="mx-auto max-w-[900px] px-4 py-10">
      {loading && <FetchingModal />}

      <div className="mb-8 text-center">
        <TapeLabel className="mb-4">AI WEDDING PLAN · 빠르게 모드</TapeLabel>
        <h1 className="mb-2 font-display text-2xl text-ink md:text-3xl">
          예산과 지역만 알려주세요
        </h1>
        <p className="text-sm text-ink-muted">
          AI 호출 없이, 지금 등록된 업체/패키지 중에서 바로 추천해드려요
        </p>
      </div>

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

            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium text-ink-soft">
                결혼 날짜
              </label>
              <input
                type="date"
                value={form.weddingDate}
                onChange={handleChange("weddingDate")}
                className="w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark md:w-1/2"
              />
            </div>
          </div>

          {error && <p className="mt-4 text-sm text-red-500">{error}</p>}

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
              자세히 설정하기
            </button>
          </div>
        </form>
      ) : (
        <div>
          <ResultCards result={result} />

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
  );
};

export default QuickPlanPage;
