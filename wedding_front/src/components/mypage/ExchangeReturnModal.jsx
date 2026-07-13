import { useState } from "react";
import { requestExchangeReturn } from "../../api/checkoutApi";

const REASON_OPTIONS = {
  EXCHANGE: [
    "상품 불량/파손",
    "다른 상품이 왔어요(오배송)",
    "옵션/수량이 달라요",
    "상품 정보와 달라요",
    "기타",
  ],
  RETURN: [
    "단순 변심",
    "상품 불량/파손",
    "다른 상품이 왔어요(오배송)",
    "상품 정보와 달라요",
    "기타",
  ],
};

const ExchangeReturnModal = ({ order, onClose, onSubmitted }) => {
  const [type, setType] = useState("EXCHANGE");
  const [reason, setReason] = useState(REASON_OPTIONS.EXCHANGE[0]);
  const [detail, setDetail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const changeType = (nextType) => {
    setType(nextType);
    setReason(REASON_OPTIONS[nextType][0]);
    setError("");
  };

  const handleSubmit = async () => {
    if (!detail.trim()) {
      setError("상세 내용을 입력해주세요.");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      await requestExchangeReturn(order.orderNumber, {
        type,
        reason,
        detail: detail.trim(),
      });
      onSubmitted?.({
        type,
        reason,
        detail: detail.trim(),
        requestedAt: new Date().toISOString(),
      });
      onClose();
    } catch (e) {
      console.error(e);
      setError(
        e?.response?.data?.message ||
          e?.response?.data?.error ||
          "신청에 실패했어요. 잠시 후 다시 시도해주세요.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4"
      onClick={(e) => e.target === e.currentTarget && !submitting && onClose()}
    >
      <div className="bg-white rounded-2xl w-full max-w-md p-6 max-h-[90vh] overflow-y-auto">
        <p className="text-lg font-medium text-ink mb-1">교환/환불 신청</p>
        <p className="text-xs text-ink-faint mb-5">{order.orderNumber}</p>

        <div className="flex gap-2 mb-4">
          <button
            type="button"
            onClick={() => changeType("EXCHANGE")}
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
            onClick={() => changeType("RETURN")}
            className={`flex-1 h-10 rounded-full text-sm font-medium ${
              type === "RETURN"
                ? "bg-brand text-white"
                : "bg-surface text-ink-muted"
            }`}
          >
            환불
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
          {REASON_OPTIONS[type].map((item) => (
            <option key={item} value={item}>
              {item}
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
          maxLength={500}
          placeholder="상품 상태와 요청 내용을 자세히 적어주세요"
          className="w-full px-4 py-3 rounded-lg border border-line-soft text-sm outline-none focus:border-brand resize-none"
        />
        <div className="text-right text-[11px] text-ink-faint mt-1">
          {detail.length}/500
        </div>

        {error && <p className="text-xs text-red-600 mt-2">{error}</p>}

        <p className="text-[11px] text-ink-faint mt-4 mb-4">
          배송 완료 주문만 신청할 수 있으며, 신청 후 관리자가 내용을 확인해
          처리합니다.
        </p>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="flex-1 h-11 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream disabled:opacity-60"
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
