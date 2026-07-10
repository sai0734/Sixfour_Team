import { useState } from "react";
import { requestExchangeReturn } from "../../api/checkoutApi";

const REASON_OPTIONS = [
  "단순 변심",
  "상품 불량/파손",
  "다른 상품이 왔어요(오배송)",
  "상품 정보와 달라요",
  "기타",
];

// 마이페이지 결제내역 "교환/반품 신청" 모달.
// TODO(황용현): 이 모달이 부르는 POST /api/checkout/orders/{orderNumber}/exchange-return
// 엔드포인트가 아직 백엔드에 없음 - 별도로 스펙 전달해뒀음.
const ExchangeReturnModal = ({ order, onClose, onSubmitted }) => {
  const [type, setType] = useState("EXCHANGE");
  const [reason, setReason] = useState(REASON_OPTIONS[0]);
  const [detail, setDetail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = () => {
    if (!detail.trim()) {
      setError("상세 내용을 입력해주세요.");
      return;
    }

    setSubmitting(true);

    requestExchangeReturn(order.orderNumber, { type, reason, detail })
      .then(() => {
        onSubmitted?.();
        onClose();
      })
      .catch((e) => {
        console.error(e);
        setError("신청에 실패했어요. 잠시 후 다시 시도해주세요.");
      })
      .finally(() => setSubmitting(false));
  };

  return (
    <div
      className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl w-full max-w-md p-6 max-h-[90vh] overflow-y-auto">
        <p className="text-lg font-medium text-ink mb-1">교환/반품 신청</p>
        <p className="text-xs text-ink-faint mb-5">{order.orderNumber}</p>

        <div className="flex gap-2 mb-4">
          <button
            type="button"
            onClick={() => setType("EXCHANGE")}
            className={`flex-1 h-10 rounded-full text-sm font-medium ${
              type === "EXCHANGE"
                ? "bg-brand text-white"
                : "bg-surface text-ink-muted"
            }`}
          >
            교환
          </button>
          <button
            type="button"
            onClick={() => setType("RETURN")}
            className={`flex-1 h-10 rounded-full text-sm font-medium ${
              type === "RETURN"
                ? "bg-brand text-white"
                : "bg-surface text-ink-muted"
            }`}
          >
            반품
          </button>
        </div>

        <label className="block text-xs font-medium text-ink-muted mb-1.5">
          사유
        </label>
        <select
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          className="w-full h-11 px-4 rounded-lg border border-line-soft text-sm mb-4 outline-none focus:border-brand"
        >
          {REASON_OPTIONS.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>

        <label className="block text-xs font-medium text-ink-muted mb-1.5">
          상세 내용
        </label>
        <textarea
          value={detail}
          onChange={(e) => {
            setDetail(e.target.value);
            setError("");
          }}
          rows={4}
          placeholder="어떤 문제가 있었는지 자세히 적어주시면 처리가 빨라져요"
          className="w-full px-4 py-3 rounded-lg border border-line-soft text-sm outline-none focus:border-brand resize-none"
        />

        {error && <p className="text-xs text-red-600 mt-2">{error}</p>}

        <p className="text-[11px] text-ink-faint mt-4 mb-4">
          신청 후 상품 상태 확인이 필요할 수 있어요. 처리 결과는 결제내역에서
          확인할 수 있어요.
        </p>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 h-11 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting}
            className="flex-1 h-11 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark disabled:opacity-60"
          >
            {submitting ? "신청 중..." : "신청하기"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExchangeReturnModal;
