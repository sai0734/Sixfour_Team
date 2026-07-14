import { useState } from "react";

// 재원 추가 - 홀/드레스/메이크업처럼 업체 안에 여러 옵션(아이템/패키지)이 있는 경우,
// 어떤 옵션에 관심있는지 골라서 찜할 수 있게 하는 모달.
// 옵션 목록은 util/companyOptionBuilder.js의 buildCompanyOptions로 만들어짐
// (ReservationReserveComponent에서 예약할 때 쓰는 옵션 목록과 동일한 소스).
const CompanyWishOptionModal = ({
  companyName,
  options,
  onSubmit,
  onClose,
}) => {
  const [selectedKey, setSelectedKey] = useState(options[0]?.key || "");

  const handleConfirm = () => {
    const selected = options.find((o) => o.key === selectedKey);
    if (!selected) return;
    // 재원 수정 - 라벨만 넘기던 걸 옵션 객체 전체(가격/이미지 포함)로 변경.
    // 마이페이지 찜 목록에서 이 옵션의 실제 가격/이미지를 보여주려면 필요함.
    onSubmit(selected);
  };

  return (
    <div
      className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl w-full max-w-md p-6 max-h-[85vh] overflow-y-auto">
        <p className="text-lg font-medium text-ink mb-1">찜할 옵션 선택</p>
        <p className="text-xs text-ink-faint mb-5">{companyName}</p>

        <div className="flex flex-col gap-2 mb-6">
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
                  name="wish-option"
                  checked={selectedKey === opt.key}
                  onChange={() => setSelectedKey(opt.key)}
                />
                <div className="min-w-0">
                  <p className="text-sm font-medium text-ink truncate">
                    {opt.label}
                  </p>
                  {opt.detail && (
                    <p className="text-xs text-ink-faint truncate">
                      {opt.detail}
                    </p>
                  )}
                </div>
              </div>
              {opt.price > 0 && (
                <span className="shrink-0 text-sm text-ink-soft">
                  {opt.price.toLocaleString()}원
                </span>
              )}
            </label>
          ))}
        </div>

        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="h-10 px-5 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={!selectedKey}
            className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark disabled:opacity-50"
          >
            이 옵션으로 찜하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default CompanyWishOptionModal;
