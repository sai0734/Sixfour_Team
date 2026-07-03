import { useState } from "react";

export const REGION_OPTIONS = ["서울", "경기", "부산", "인천", "대구", "기타"];
export const STYLE_OPTIONS = ["소규모", "하객형", "야외", "호텔", "스몰웨딩"];

const initState = {
  budgetMin: 2000,
  budgetMax: 4000,
  region: "서울",
  weddingStyle: "소규모",
  weddingDate: "",
  bio: "",
};

// mode: "add" | "edit"
const CoupleProfileFormModal = ({
  mode = "add",
  editTarget,
  onSubmit,
  onClose,
}) => {
  const [form, setForm] = useState(
    editTarget
      ? {
          budgetMin: editTarget.budgetMin,
          budgetMax: editTarget.budgetMax,
          region: editTarget.region,
          weddingStyle: editTarget.weddingStyle,
          weddingDate: editTarget.weddingDate || "",
          bio: editTarget.bio || "",
        }
      : { ...initState },
  );

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = () => {
    onSubmit({
      ...form,
      budgetMin: Number(form.budgetMin) || 0,
      budgetMax: Number(form.budgetMax) || 0,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto">
        <p className="text-lg font-medium text-ink mb-1">
          {mode === "add" ? "선배 부부 프로필 등록" : "프로필 수정"}
        </p>
        <p className="text-xs text-ink-faint mb-5">
          내 웨딩 경험을 공유하고 예비 부부를 도와주세요.
        </p>

        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">
                예산 최소 (만원)
              </span>
              <input
                className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                name="budgetMin"
                type="number"
                min={0}
                value={form.budgetMin}
                onChange={handleChange}
              />
            </label>
            <label className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">
                예산 최대 (만원)
              </span>
              <input
                className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                name="budgetMax"
                type="number"
                min={0}
                value={form.budgetMax}
                onChange={handleChange}
              />
            </label>
          </div>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">지역</span>
            <select
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="region"
              value={form.region}
              onChange={handleChange}
            >
              {REGION_OPTIONS.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">
              웨딩 스타일
            </span>
            <select
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="weddingStyle"
              value={form.weddingStyle}
              onChange={handleChange}
            >
              {STYLE_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">결혼 날짜</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="weddingDate"
              type="date"
              value={form.weddingDate}
              onChange={handleChange}
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">자기소개</span>
            <textarea
              className="min-h-[100px] px-4 py-3 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand resize-none"
              name="bio"
              placeholder="예: 소규모 웨딩을 준비하면서 정말 많은 걸 배웠어요..."
              value={form.bio}
              onChange={handleChange}
            />
          </label>
        </div>

        <div className="flex justify-end gap-2 mt-6">
          <button
            type="button"
            onClick={onClose}
            className="h-10 px-5 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
          >
            {mode === "add" ? "등록" : "저장"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CoupleProfileFormModal;
