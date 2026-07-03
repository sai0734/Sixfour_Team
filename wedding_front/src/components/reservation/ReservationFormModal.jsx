import { useState } from "react";

export const STATUS_OPTIONS = ["대기", "확정", "취소"];

const initState = {
  cmno: "",
  weddingDate: "",
  status: "대기",
  memo: "",
};

// mode: "add" | "edit"
const ReservationFormModal = ({
  mode = "add",
  editTarget,
  onSubmit,
  onClose,
}) => {
  const [form, setForm] = useState(
    editTarget
      ? {
          cmno: editTarget.cmno,
          weddingDate: editTarget.weddingDate || "",
          status: editTarget.status,
          memo: editTarget.memo || "",
        }
      : { ...initState },
  );

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = () => {
    if (!form.cmno) {
      alert("업체 번호를 입력해주세요.");
      return;
    }

    onSubmit({ ...form, cmno: Number(form.cmno) });
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-md p-6">
        <p className="text-lg font-medium text-ink mb-1">
          {mode === "add" ? "예약 등록" : "예약 수정"}
        </p>
        {mode === "add" && (
          <p className="text-xs text-ink-faint mb-5">
            업체탐색 화면 연동 전까지는 업체 번호를 직접 입력해 테스트합니다.
          </p>
        )}

        <div className="flex flex-col gap-4 mt-2">
          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">업체 번호</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand disabled:bg-cream disabled:text-ink-faint"
              name="cmno"
              type="number"
              value={form.cmno}
              onChange={handleChange}
              disabled={mode === "edit"}
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">예약 날짜</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="weddingDate"
              type="date"
              value={form.weddingDate}
              onChange={handleChange}
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">상태</span>
            <select
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="status"
              value={form.status}
              onChange={handleChange}
            >
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">요청사항</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="memo"
              type="text"
              placeholder="예: 오후 2시 이후 방문 희망"
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
            {mode === "add" ? "등록" : "저장"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ReservationFormModal;
