import { useState } from "react";

export const CATEGORY_OPTIONS = ["홀", "스드메", "예복", "예물", "기타"];

// 카테고리별로 화면이 나뉘어 있지 않고 하나의 목록이라, 순서도 전체 기준 하나로 관리
const suggestNextOrder = (budgetList, excludeId) => {
  const others = budgetList.filter((i) => i.budgetId !== excludeId);
  if (others.length === 0) return 1;
  return Math.max(...others.map((i) => i.sortOrder)) + 1;
};

// mode: "add" | "edit". budgetList: 전체 목록(다음 순번 계산 + 스왑 대상 탐색용)
const BudgetFormModal = ({
  mode = "add",
  editTarget,
  budgetList,
  onSubmit,
  onClose,
}) => {
  const [form, setForm] = useState(
    editTarget
      ? {
          category: editTarget.category,
          budgetAmount: editTarget.budgetAmount,
          actualAmount: editTarget.actualAmount,
          memo: editTarget.memo || "",
          sortOrder: editTarget.sortOrder,
        }
      : {
          category: "홀",
          budgetAmount: 0,
          actualAmount: 0,
          memo: "",
          sortOrder: suggestNextOrder(budgetList, null),
        },
  );

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = () => {
    onSubmit({
      ...form,
      budgetAmount: Number(form.budgetAmount) || 0,
      actualAmount: Number(form.actualAmount) || 0,
      sortOrder: Number(form.sortOrder) || 1,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl w-full max-w-md p-6">
        <p className="text-lg font-medium text-ink mb-5">
          {mode === "add" ? "예산 항목 추가" : "예산 항목 수정"}
        </p>

        <div className="flex flex-col gap-4">
          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">카테고리</span>
            <select
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="category"
              value={form.category}
              onChange={handleChange}
            >
              {CATEGORY_OPTIONS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">
              계획 예산 (원)
            </span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="budgetAmount"
              type="number"
              min={0}
              value={form.budgetAmount}
              onChange={handleChange}
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">
              실지출 (원)
            </span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="actualAmount"
              type="number"
              min={0}
              value={form.actualAmount}
              onChange={handleChange}
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">
              전체 목록 순서
            </span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="sortOrder"
              type="number"
              min={1}
              value={form.sortOrder}
              onChange={handleChange}
            />
            <span className="text-[11px] text-ink-faint">
              이미 사용 중인 번호를 넣으면, 그 항목과 순서가 서로 바뀝니다.
              (카테고리 상관없이 목록 전체 기준)
            </span>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">메모</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="memo"
              type="text"
              placeholder="예: 강남 그레이스 웨딩홀・계약 완료"
              value={form.memo}
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
            {mode === "add" ? "추가" : "저장"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default BudgetFormModal;
