import { useEffect, useState } from "react";

export const STAGE_LABELS = {
  1: "기본 계획",
  2: "업체 계약",
  3: "청첩장·답례품",
};

const suggestNextOrder = (checklist, stage, excludeId) => {
  const inStage = checklist.filter(
    (i) => i.stage === Number(stage) && i.checklistId !== excludeId,
  );
  if (inStage.length === 0) return 1;
  return Math.max(...inStage.map((i) => i.sortOrder)) + 1;
};

// mode: "add" | "edit". checklist: 전체 목록(같은 단계 내 다음 순번 계산용)
const ChecklistFormModal = ({
  mode = "add",
  editTarget,
  checklist,
  onSubmit,
  onClose,
}) => {
  const [form, setForm] = useState(
    editTarget
      ? {
          title: editTarget.title,
          stage: editTarget.stage,
          sortOrder: editTarget.sortOrder,
        }
      : {
          title: "",
          stage: 1,
          sortOrder: suggestNextOrder(checklist, 1, null),
        },
  );

  // 사용자가 순서를 직접 건드리면, 단계를 바꿔도 더는 자동 제안하지 않음
  const [sortOrderTouched, setSortOrderTouched] = useState(false);

  useEffect(() => {
    if (sortOrderTouched) return;

    const excludeId = mode === "edit" ? editTarget.checklistId : null;
    const suggested = suggestNextOrder(checklist, form.stage, excludeId);

    setForm((f) => ({ ...f, sortOrder: suggested }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.stage]);

  const handleChange = (e) => {
    const { name, value } = e.target;

    if (name === "sortOrder") {
      setSortOrderTouched(true);
    }

    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = () => {
    if (!form.title.trim()) {
      alert("항목명을 입력해주세요.");
      return;
    }

    onSubmit({
      ...form,
      stage: Number(form.stage),
      sortOrder: Number(form.sortOrder) || 1,
    });
  };

  const currentStageCount = checklist.filter(
    (i) =>
      i.stage === Number(form.stage) &&
      (mode !== "edit" || i.checklistId !== editTarget.checklistId),
  ).length;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl w-full max-w-md p-6">
        <p className="text-lg font-medium text-ink mb-5">
          {mode === "add" ? "체크리스트 항목 추가" : "체크리스트 항목 수정"}
        </p>

        <div className="flex flex-col gap-4">
          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">항목명</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="title"
              type="text"
              placeholder="예: 웨딩홀 계약금 입금"
              value={form.title}
              onChange={handleChange}
              autoFocus
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">단계</span>
            <select
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="stage"
              value={form.stage}
              onChange={handleChange}
            >
              {Object.entries(STAGE_LABELS).map(([num, label]) => (
                <option key={num} value={num}>
                  {num}단계 — {label}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">
              단계 내 순서
              <span className="text-ink-faint font-normal ml-1">
                (현재 {currentStageCount}개 항목 중 배치)
              </span>
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
            </span>
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

export default ChecklistFormModal;
