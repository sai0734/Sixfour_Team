import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import TapeLabel from "../../components/common/TapeLabel";
import FetchingModal from "../../components/common/FetchingModal";
import ResultCards from "../../components/aiplan/ResultCards";
import {
  getDetailRecommendations,
  getAiRecommendations,
  refineRecommendation,
  rollbackRecommendation,
} from "../../api/aiPlanApi";

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

const DetailPlanPage = () => {
  const [searchParams] = useSearchParams();

  const [form, setForm] = useState({
    budgetManwon: searchParams.get("budgetManwon") || "",
    region: searchParams.get("region") || "",
    groomName: searchParams.get("groomName") || "",
    brideName: searchParams.get("brideName") || "",
    weddingDate: searchParams.get("weddingDate") || "",
    hallType: "",
    studioMood: "",
    dressStyle: "",
    makeupStyle: "",
    freeText: "",
  });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [refineOpen, setRefineOpen] = useState(false);
  const [refineText, setRefineText] = useState("");
  const [refineLoading, setRefineLoading] = useState(false);

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const buildPayload = () => ({
    budget: Number(form.budgetManwon) * 10000,
    region: form.region,
    groomName: form.groomName || null,
    brideName: form.brideName || null,
    weddingDate: form.weddingDate || null,
    hallType: form.hallType || null,
    studioMood: form.studioMood || null,
    dressStyle: form.dressStyle || null,
    makeupStyle: form.makeupStyle || null,
    freeText: form.freeText || null,
  });

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!form.budgetManwon || !form.region) {
      setError("총 예산과 지역은 필수로 입력해주세요.");
      return;
    }

    setError(null);
    setLoading(true);

    getDetailRecommendations(buildPayload())
      .then((data) => setResult(data))
      .catch((err) => {
        console.error(err);
        setError(
          "추천을 불러오는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
        );
      })
      .finally(() => setLoading(false));
  };

  const handleSubmitAi = (e) => {
    e.preventDefault();

    if (!form.budgetManwon || !form.region) {
      setError("총 예산과 지역은 필수로 입력해주세요.");
      return;
    }

    setError(null);
    setLoading(true);

    getAiRecommendations(buildPayload())
      .then((data) => setResult(data))
      .catch((err) => {
        console.error(err);
        setError(
          "AI 추천을 불러오는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
        );
      })
      .finally(() => setLoading(false));
  };

  const handleReset = () => {
    setResult(null);
    setError(null);
    setRefineOpen(false);
    setRefineText("");
  };

  const handleRefineSubmit = (e) => {
    e.preventDefault();

    if (!refineText.trim() || !result?.sessionId) {
      return;
    }

    setRefineLoading(true);

    refineRecommendation({
      sessionId: result.sessionId,
      message: refineText.trim(),
    })
      .then((data) => {
        setResult(data);
        setRefineText("");
      })
      .catch((err) => {
        console.error(err);
        setError(
          "다듬기 요청 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.",
        );
      })
      .finally(() => setRefineLoading(false));
  };

  const handleRollback = () => {
    if (!result?.sessionId) {
      return;
    }

    setRefineLoading(true);

    rollbackRecommendation(result.sessionId)
      .then((data) => setResult(data))
      .catch((err) => {
        console.error(err);
        setError("되돌리기 중 문제가 발생했어요.");
      })
      .finally(() => setRefineLoading(false));
  };

  const inputClass =
    "w-full rounded-xl border border-line px-4 py-2.5 text-ink outline-none focus:border-brand-dark";
  const labelClass = "mb-1 block text-sm font-medium text-ink-soft";

  return (
    <div className="mx-auto max-w-[900px] px-4 py-10">
      {loading && <FetchingModal />}

      <div className="mb-8 text-center">
        <TapeLabel className="mb-4">AI WEDDING PLAN · 자세히 모드</TapeLabel>
        <h1 className="mb-2 font-display text-2xl text-ink md:text-3xl">
          카테고리별 취향까지 알려주세요
        </h1>
        <p className="text-sm text-ink-muted">
          비워두면 취향 없이, 입력한 항목만 반영해서 추천해드려요
        </p>
      </div>

      {!result ? (
        <form
          onSubmit={handleSubmit}
          className="rounded-2xl border border-line bg-white p-6 shadow-sm md:p-8"
        >
          <p className="mb-3 text-xs font-medium text-ink-muted">공통 정보</p>
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

            <div className="md:col-span-2">
              <label className={labelClass}>결혼 날짜</label>
              <input
                type="date"
                value={form.weddingDate}
                onChange={handleChange("weddingDate")}
                className={`${inputClass} md:w-1/2`}
              />
            </div>
          </div>

          <p className="mb-3 text-xs font-medium text-ink-muted">
            카테고리별 취향 (전부 선택 사항)
          </p>
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            <div>
              <label className={labelClass}>홀 분위기</label>
              <select
                value={form.hallType}
                onChange={handleChange("hallType")}
                className={inputClass}
              >
                {HALL_TYPE_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className={labelClass}>스튜디오 분위기</label>
              <input
                type="text"
                value={form.studioMood}
                onChange={handleChange("studioMood")}
                placeholder="예: 내추럴, 클래식, 시네마틱"
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>드레스 스타일</label>
              <input
                type="text"
                value={form.dressStyle}
                onChange={handleChange("dressStyle")}
                placeholder="예: 머메이드, 미니, 볼륨"
                className={inputClass}
              />
            </div>

            <div>
              <label className={labelClass}>메이크업 스타일</label>
              <input
                type="text"
                value={form.makeupStyle}
                onChange={handleChange("makeupStyle")}
                placeholder="예: 내추럴, 화사, 시크"
                className={inputClass}
              />
            </div>

            <div className="md:col-span-2">
              <label className={labelClass}>추가로 하고 싶은 말</label>
              <textarea
                value={form.freeText}
                onChange={handleChange("freeText")}
                rows={3}
                placeholder="예: 하객이 많아서 넓은 홀이었으면 좋겠어요 (아래 'AI에게 맡기기'로 추천받을 때만 반영돼요)"
                className={inputClass}
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
              규칙 기반으로 추천받기
            </button>
            <button
              type="button"
              onClick={handleSubmitAi}
              disabled={loading}
              className="h-12 flex-1 rounded-full border border-brand-dark text-sm font-medium text-brand-deep hover:bg-surface disabled:opacity-60"
            >
              AI에게 맡기기
            </button>
          </div>
        </form>
      ) : (
        <div>
          <ResultCards result={result} />

          {error && (
            <p className="mt-4 text-center text-sm text-red-500">{error}</p>
          )}

          {result.sessionId && refineOpen && (
            <form
              onSubmit={handleRefineSubmit}
              className="mt-6 rounded-2xl border border-line bg-surface p-5"
            >
              <label className="mb-2 block text-xs font-medium text-ink-muted">
                이 조합을 어떻게 다듬을까요?
              </label>
              <textarea
                value={refineText}
                onChange={(e) => setRefineText(e.target.value)}
                rows={3}
                placeholder="예: 스튜디오는 예산 초과라서 빼고 나머지로 추천해줘. 홀이랑 메이크업은 마음에 들어서 확정, 드레스만 다른 스타일로 다시 찾아줘"
                className="w-full rounded-xl border border-line bg-white px-4 py-2.5 text-sm text-ink outline-none focus:border-brand-dark"
              />
              <div className="mt-3 flex flex-wrap gap-2">
                <button
                  type="submit"
                  disabled={refineLoading || !refineText.trim()}
                  className="h-10 rounded-full bg-brand px-5 text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-60"
                >
                  {refineLoading ? "반영하는 중..." : "보내기"}
                </button>
                <button
                  type="button"
                  onClick={handleRollback}
                  disabled={refineLoading}
                  className="h-10 rounded-full border border-line px-5 text-sm font-medium text-ink-soft hover:bg-white disabled:opacity-60"
                >
                  직전으로 되돌리기
                </button>
              </div>
            </form>
          )}

          <div className="mt-8 flex flex-col items-center gap-3 md:flex-row md:justify-center">
            <button
              type="button"
              onClick={handleReset}
              className="h-11 rounded-full border border-line px-6 text-sm font-medium text-ink-soft hover:bg-surface"
            >
              다른 조건으로 다시 찾기
            </button>
            {result.sessionId && (
              <button
                type="button"
                onClick={() => setRefineOpen((prev) => !prev)}
                className="h-11 rounded-full border border-brand-dark px-6 text-sm font-medium text-brand-deep hover:bg-surface"
              >
                {refineOpen ? "다듬기 닫기" : "이 결과 다듬기"}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default DetailPlanPage;
