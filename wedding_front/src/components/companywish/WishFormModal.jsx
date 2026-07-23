import { useState } from "react";
import { showAlert } from "../../util/globalAlert";

const WishFormModal = ({ onSubmit, onClose }) => {
  const [cmno, setCmno] = useState("");

  const handleSubmit = () => {
    if (!cmno) {
      showAlert("업체 번호를 입력해주세요.");
      return;
    }
    onSubmit(Number(cmno));
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl w-full max-w-md p-6">
        <p className="text-lg font-medium text-ink mb-5">업체 찜하기</p>

        <p className="text-xs text-ink-faint mb-4">
          업체탐색 화면 연동 전까지는 업체 번호를 직접 입력해 테스트합니다.
        </p>

        <label className="flex flex-col gap-2">
          <span className="text-xs font-medium text-ink-soft">업체 번호</span>
          <input
            className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
            type="number"
            value={cmno}
            onChange={(e) => setCmno(e.target.value)}
            autoFocus
          />
        </label>

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
            찜하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default WishFormModal;
