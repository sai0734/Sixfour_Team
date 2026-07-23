// 브라우저 기본 confirm() 대체용 공용 확인 모달 (AlertModal과 동일한 카드 스타일 + 취소/확인 두 버튼)
const ConfirmModal = ({ message, onCancel, onConfirm }) => {
  if (!message) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-[0_20px_60px_-15px_rgba(58,54,47,0.35)]">
        <p className="mb-5 text-sm text-ink whitespace-pre-wrap leading-relaxed">
          {message}
        </p>
        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-full px-4 py-2 text-sm font-medium text-ink-muted hover:bg-surface transition"
          >
            취소
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="rounded-full bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark transition"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;
