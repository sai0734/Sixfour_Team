import { useState } from "react";

const initState = {
  title: "",
  eventDate: "",
  memo: "",
};

// mode: "add" | "edit"
const DdayEventFormModal = ({
  mode = "add",
  editTarget,
  onSubmit,
  onClose,
}) => {
  const [form, setForm] = useState(
    editTarget
      ? {
          title: editTarget.title,
          eventDate: editTarget.eventDate || "",
          memo: editTarget.memo || "",
        }
      : { ...initState },
  );

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleSubmit = () => {
    if (!form.title.trim()) {
      alert("일정명을 입력해주세요.");
      return;
    }
    if (!form.eventDate) {
      alert("날짜를 선택해주세요.");
      return;
    }

    onSubmit(form);
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-md p-6">
        <p className="text-lg font-medium text-ink mb-5">
          {mode === "add" ? "일정 추가" : "일정 수정"}
        </p>

        <div className="flex flex-col gap-4">
          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">일정명</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="title"
              type="text"
              placeholder="예: 상견례, 혼인신고"
              value={form.title}
              onChange={handleChange}
              autoFocus
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">날짜</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="eventDate"
              type="date"
              value={form.eventDate}
              onChange={handleChange}
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="text-xs font-medium text-ink-soft">메모</span>
            <input
              className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
              name="memo"
              type="text"
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

export default DdayEventFormModal;
