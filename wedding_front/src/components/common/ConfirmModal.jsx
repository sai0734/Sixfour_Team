// 브라우저 기본 confirm() 대체용 공용 확인 모달 (AlertModal과 동일한 카드 스타일 + 취소/확인 두 버튼)
// z-[10000] - 앱에서 가장 높은 모달(z-[9999], 게시글 상세/채팅 모달 등) 위에서도 항상
// 확인창이 떠야 해서, 그보다 한 단계 더 높은 값으로 고정해뒀다. 어떤 모달 위에서 이 확인창을
// 띄우든(게시글 삭제, 채팅방 나가기 등) 뒤로 깔리는 일이 없어야 한다.
const ConfirmModal = ({ message, onCancel, onConfirm }) => {
  if (!message) return null;

  return (
    <div className="fixed inset-0 z-[10000] flex items-center justify-center bg-black/40 px-4">
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
