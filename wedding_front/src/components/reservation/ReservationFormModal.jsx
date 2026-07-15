import { useMemo, useState } from "react";
import { buildCompanyOptions, categoryLabel } from "../../util/companyOptionBuilder";

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
  company,
  onSubmit,
  onClose,
}) => {
  const options = useMemo(() => buildCompanyOptions(company), [company]);

  const findOptionKey = (optionName) => {
    const found = options.find((o) => o.label === optionName);
    return found?.key || options[0]?.key || "";
  };

  const [selectedKey, setSelectedKey] = useState(
    editTarget ? findOptionKey(editTarget.optionName) : "",
  );

  const [form, setForm] = useState(
    editTarget
      ? {
          cmno: editTarget.cmno,
          weddingDate: editTarget.weddingDate || "",
          memo: editTarget.memo || "",
        }
      : { ...initState },
  );

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const selectedOption = options.find((o) => o.key === selectedKey);

  const handleSubmit = () => {
    if (mode === "add") {
      if (!form.cmno) {
        alert("업체 번호를 입력해주세요.");
        return;
      }
      onSubmit({ ...form, cmno: Number(form.cmno) });
      return;
    }

    if (!form.weddingDate) {
      alert("예약 날짜를 선택해주세요.");
      return;
    }
    if (options.length > 0 && !selectedOption) {
      alert("옵션을 선택해주세요.");
      return;
    }

    onSubmit({
      weddingDate: form.weddingDate,
      memo: form.memo,
      optionName: selectedOption ? selectedOption.label : editTarget.optionName,
      amount: selectedOption ? selectedOption.price : editTarget.amount,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-md p-6 max-h-[85vh] overflow-y-auto">
        <p className="text-lg font-medium text-ink mb-1">
          {mode === "add" ? "예약 등록" : "예약 수정"}
        </p>

        {mode === "add" && (
          <p className="text-xs text-ink-faint mb-5">
            업체탐색 화면 연동 전까지는 업체 번호를 직접 입력해 테스트합니다.
          </p>
        )}

        {mode === "edit" && company && (
          <p className="text-xs text-ink-faint mb-5">
            {categoryLabel[company.category] || company.category} · {company.name}
          </p>
        )}

        <div className="flex flex-col gap-4 mt-2">
          {mode === "add" && (
            <>
              <label className="flex flex-col gap-2">
                <span className="text-xs font-medium text-ink-soft">업체 번호</span>
                <input
                  className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                  name="cmno"
                  type="number"
                  value={form.cmno}
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
            </>
          )}

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

          {/* 승진 코드 추가 - 수정 모드: 옵션 선택 */}
          {mode === "edit" && (
            <div className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">옵션</span>
              {options.length === 0 ? (
                <p className="text-sm text-ink-faint py-2">
                  {editTarget?.optionName || "등록된 옵션 없음"}
                </p>
              ) : (
                <div className="flex flex-col gap-2">
                  {options.map((opt) => (
                    <label
                      key={opt.key}
                      className={`flex items-center justify-between gap-3 rounded-xl border px-4 py-3 cursor-pointer transition ${
                        selectedKey === opt.key
                          ? "border-brand bg-blush-50"
                          : "border-line-soft hover:border-brand"
                      }`}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <input
                          type="radio"
                          name="option"
                          checked={selectedKey === opt.key}
                          onChange={() => setSelectedKey(opt.key)}
                        />
                        <p className="text-sm font-medium text-ink truncate">
                          {opt.label}
                        </p>
                      </div>
                      {opt.price > 0 && (
                        <span className="shrink-0 text-sm text-ink-soft">
                          {opt.price.toLocaleString()}원
                        </span>
                      )}
                    </label>
                  ))}
                </div>
              )}
            </div>
          )}
          {/* 승진 코드 추가 끝 */}

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
