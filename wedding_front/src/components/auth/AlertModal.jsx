// 로그인/회원가입/로그아웃 등 인증 관련 페이지 공용 알림 모달 (브라우저 기본 alert() 대체)
// ConfirmModal과 동일한 이유로 z-[10000] - 앱에서 가장 높은 모달(z-[9999]) 위에서 띄워도
// 항상 최상단에 와야 한다.
const AlertModal = ({ message, onClose }) => {
  if (!message) return null;

  return (
    <div className="fixed inset-0 z-[10000] flex items-center justify-center bg-black/40 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-[0_20px_60px_-15px_rgba(58,54,47,0.35)]">
        <p className="mb-5 text-sm text-ink whitespace-pre-wrap leading-relaxed">
          {message}
        </p>
        <div className="flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="rounded-full bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark transition"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

export default AlertModal;
