import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import TapeLabel from "../../components/common/TapeLabel";
import FetchingModal from "../../components/common/FetchingModal";
import ResultCards from "../../components/aiplan/ResultCards";
import { getDetailRecommendations } from "../../api/aiPlanApi";

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

    getDetailRecommendations({
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

  const handleReset = () => {
    setResult(null);
    setError(null);
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
                placeholder="예: 하객이 많아서 넓은 홀이었으면 좋겠어요 (아직 추천에는 반영되지 않고, AI 연동 이후 단계에서 사용될 예정이에요)"
                className={inputClass}
              />
            </div>
          </div>

          {error && <p className="mt-4 text-sm text-red-500">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="mt-6 h-12 w-full rounded-full bg-brand text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-60"
          >
            취향 반영해서 추천받기
          </button>
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

export default DetailPlanPage;
