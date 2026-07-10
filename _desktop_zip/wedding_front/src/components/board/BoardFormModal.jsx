import { useState } from "react";

export const BOARD_TYPE_LABELS = {
  FREE: "자유",
  REVIEW: "후기",
};

const initState = (fixedType) => ({
  boardType: fixedType || "FREE",
  category: "",
  title: "",
  content: "",
  rating: 5,
});

// mode: "add" | "edit"
// fixedType: 지정하면 게시판 종류 select를 숨기고 그 타입으로 고정 (자유게시판 전용 페이지 등에서 사용)
// categoryOptions: 지정하면 그 목록으로 카테고리 select 노출 (예: 자유게시판 전용 카테고리)
const BoardFormModal = ({
  mode = "add",
  editTarget,
  fixedType,
  categoryOptions,
  onSubmit,
  onClose,
}) => {
  const [form, setForm] = useState(
    editTarget
      ? {
          boardType: editTarget.boardType,
          category: editTarget.category || "",
          title: editTarget.title,
          content: editTarget.content,
          rating: editTarget.rating || 5,
        }
      : initState(fixedType),
  );

  const [files, setFiles] = useState([]);

  const handleFileChange = (e) => {
    setFiles(Array.from(e.target.files || []));
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const activeCategoryOptions = categoryOptions || null;

  const handleSubmit = () => {
    if (!form.title.trim()) {
      alert("제목을 입력해주세요.");
      return;
    }
    if (!form.content.trim()) {
      alert("내용을 입력해주세요.");
      return;
    }

    onSubmit({
      ...form,
      category: activeCategoryOptions ? form.category : null,
      rating: form.boardType === "REVIEW" ? Number(form.rating) : null,
      files,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto">
        <p className="text-lg font-medium text-ink mb-5">
          {mode === "add" ? "글쓰기" : "글 수정"}
        </p>

        <div className="flex flex-col gap-4">
          {!fixedType && (
            <label className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">게시판</span>
              <select
                className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                name="boardType"
                value={form.boardType}
                onChange={handleChange}
                disabled={mode === "edit"}
              >
                {Object.entries(BOARD_TYPE_LABELS).map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          )}

          {activeCategoryOptions && (
            <label className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">
                카테고리
              </span>
              <select
                className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                name="category"
                value={form.category}
                onChange={handleChange}
              >
                <option value="">선택 안 함</option>
                {activeCategoryOptions.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>
          )}

          {form.boardType === "REVIEW" && (
            <label className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">별점</span>
              <select
                className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                name="rating"
                value={form.rating}
                onChange={handleChange}
              >
                {[5, 4, 3, 2, 1].map((r) => (
                  <option key={r} value={r}>
                    {"★".repeat(r)}
                    {"☆".repeat(5 - r)}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">제목</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="title"
              type="text"
              value={form.title}
              onChange={handleChange}
              autoFocus
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">내용</span>
            <textarea
              className="min-h-[140px] px-4 py-3 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand resize-none"
              name="content"
              value={form.content}
              onChange={handleChange}
            />
          </label>

          {(mode === "add" || mode === "edit") && (
            <label className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">
                사진/동영상 첨부{mode === "edit" && " (추가)"}
              </span>
              <input
                type="file"
                accept="image/*,video/*"
                multiple
                onChange={handleFileChange}
                className="text-sm text-ink-soft file:mr-3 file:h-9 file:px-4 file:rounded-full file:border-0 file:bg-surface file:text-xs file:font-medium file:text-ink-soft hover:file:bg-cream"
              />
              {files.length > 0 && (
                <span className="text-[11px] text-ink-faint">
                  {files.length}개 선택됨
                </span>
              )}
            </label>
          )}
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

export default BoardFormModal;
