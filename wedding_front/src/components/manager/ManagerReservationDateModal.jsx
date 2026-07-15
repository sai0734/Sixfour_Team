const STATUS_LABEL = {
  대기: "예약대기",
  결제대기: "결제대기",
  확정: "확정",
  취소: "취소",
};

const STATUS_STYLE = {
  대기: "bg-amber-50 text-amber-700",
  결제대기: "bg-blue-50 text-blue-700",
  확정: "bg-green-50 text-green-700",
  취소: "bg-red-50 text-red-600",
};

const FILTER_LABEL = {
  pending: "예약대기",
  payment: "결제대기",
  closed: "마감",
  any: "예약",
};

const ManagerReservationDateModal = ({
  dateKey,
  dateLabel,
  reservations,
  isClosed,
  filterType,
  navIndex,
  navTotal,
  hasPrev,
  hasNext,
  onPrev,
  onNext,
  onClose,
  onConfirm,
  confirmingId,
}) => {
  const pendingCount = reservations.filter((r) => r.status === "대기").length;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto overscroll-contain bg-black/30 p-4"
      onClick={onClose}
    >
      <div
        className="my-auto max-h-[85vh] w-full max-w-lg overflow-y-auto overscroll-contain rounded-2xl bg-white p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-lg font-medium text-ink">{dateLabel}</p>
            <p className="mt-1 text-xs text-ink-muted">
              {filterType && FILTER_LABEL[filterType] && (
                <span className="mr-2 font-medium text-brand-deep">
                  {FILTER_LABEL[filterType]}
                </span>
              )}
              예약 {reservations.length}건
              {pendingCount > 0 && (
                <span className="ml-2 font-medium text-amber-600">
                  예약대기 {pendingCount}건
                </span>
              )}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="shrink-0 text-ink-faint transition hover:text-ink"
            aria-label="닫기"
          >
            ✕
          </button>
        </div>

        {navTotal > 1 && (
          <div className="mb-4 flex items-center justify-between gap-2 rounded-xl border border-line bg-surface px-3 py-2">
            <button
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={(e) => {
                e.preventDefault();
                onPrev();
              }}
              disabled={!hasPrev}
              className="h-8 shrink-0 rounded-full border border-line-soft px-3 text-xs text-ink-soft transition hover:border-brand hover:text-brand disabled:cursor-not-allowed disabled:opacity-40"
            >
              ← 이전 날짜
            </button>
            <span className="shrink-0 text-xs font-medium text-ink-muted">
              {navIndex + 1} / {navTotal}
            </span>
            <button
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={(e) => {
                e.preventDefault();
                onNext();
              }}
              disabled={!hasNext}
              className="h-8 shrink-0 rounded-full border border-line-soft px-3 text-xs text-ink-soft transition hover:border-brand hover:text-brand disabled:cursor-not-allowed disabled:opacity-40"
            >
              다음 날짜 →
            </button>
          </div>
        )}

        {isClosed && (
          <div className="mb-4 rounded-xl border border-zinc-200 bg-zinc-100 px-4 py-3 text-sm text-zinc-600">
            이 날짜는 결제 완료된 예약이 있어 마감되었습니다.
          </div>
        )}

        <div className="flex flex-col gap-3">
          {reservations.map((r) => (
            <div
              key={r.reservationId}
              className="rounded-xl border border-line p-4"
            >
              <div className="mb-2 flex flex-wrap items-center gap-2">
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${STATUS_STYLE[r.status] || STATUS_STYLE["대기"]}`}
                >
                  {STATUS_LABEL[r.status] || r.status}
                </span>
                {r.amount > 0 && (
                  <span className="text-xs font-semibold text-ink">
                    {Number(r.amount).toLocaleString()}원
                  </span>
                )}
              </div>

              <p className="text-sm font-medium text-ink">{r.memberEmail}</p>
              <div className="mt-2 space-y-1 text-xs text-ink-muted">
                <p>옵션: {r.optionName || "미정"}</p>
                {r.memo && <p>요청: {r.memo}</p>}
              </div>

              {r.status === "대기" && (
                <button
                  type="button"
                  onClick={() => onConfirm(r.reservationId)}
                  disabled={confirmingId === r.reservationId}
                  className="mt-3 h-9 w-full rounded-full bg-brand text-xs font-medium text-white transition hover:bg-brand-deep disabled:opacity-50"
                >
                  {confirmingId === r.reservationId ? "처리 중..." : "예약 확인"}
                </button>
              )}
            </div>
          ))}
        </div>

        <button
          type="button"
          onClick={onClose}
          className="mt-5 h-10 w-full rounded-full border border-line-soft text-sm text-ink-soft transition hover:border-brand hover:text-brand"
        >
          닫기
        </button>
      </div>
    </div>
  );
};

export default ManagerReservationDateModal;
