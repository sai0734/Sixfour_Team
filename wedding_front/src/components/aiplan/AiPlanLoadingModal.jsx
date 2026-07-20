// 공용 FetchingModal(검은 배경 + "Loading.....")은 이 페이지 전체 톤(핑크/세리프/둥근 카드)이랑
// 안 어울려서, AI 웨딩플랜 전용으로 따로 만든 로딩 오버레이. common/ 파일은 안 건드림.
const AiPlanLoadingModal = ({ message = "취향에 맞는 곳을 찾고 있어요" }) => {
  return (
    <div className="fixed inset-0 z-[1055] flex items-center justify-center bg-ink/20 px-4">
      <div className="flex w-full max-w-xs flex-col items-center gap-4 rounded-3xl bg-white px-8 py-10 shadow-[0_20px_50px_-20px_rgba(58,54,47,0.35)]">
        <span className="relative flex h-12 w-12 items-center justify-center">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-blush-200 opacity-75" />
          <span className="relative inline-flex h-8 w-8 rounded-full bg-brand-dark" />
        </span>
        <div className="text-center">
          <p className="font-display text-base text-ink">{message}</p>
          <p className="mt-1 text-xs text-ink-faint">몇 초만 기다려주세요</p>
        </div>
      </div>
    </div>
  );
};

export default AiPlanLoadingModal;
