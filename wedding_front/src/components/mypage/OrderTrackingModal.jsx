import { useState } from "react";

// 마이페이지 결제내역 "배송조회" 모달.
// 백엔드에 이미 trackingNo(송장번호) 필드/관리자 입력 기능은 있는데,
// 회원용 주문 조회 API(OrderDTO)에 아직 안 내려오고 있어서 order.trackingNo가
// undefined일 수 있음 -> 그 경우 "아직 등록 안 됨" 안내만 보여줌.
const OrderTrackingModal = ({ order, onClose }) => {
  const [copied, setCopied] = useState(false);
  const trackingNo = order.trackingNo;

  const handleCopy = () => {
    navigator.clipboard.writeText(trackingNo).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  };

  return (
    <div
      className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl w-full max-w-sm p-6">
        <p className="text-lg font-medium text-ink mb-1">배송 조회</p>
        <p className="text-xs text-ink-faint mb-5">{order.orderNumber}</p>

        {!trackingNo ? (
          <div className="text-center text-ink-faint py-10 bg-surface rounded-xl text-sm">
            아직 송장번호가 등록되지 않았어요.
            <br />
            상품 준비가 완료되면 등록될 예정이에요.
          </div>
        ) : (
          <>
            <div className="bg-surface rounded-xl p-4 mb-4">
              <p className="text-xs text-ink-muted mb-1">운송장 번호</p>
              <div className="flex items-center justify-between">
                <p className="text-base font-medium text-ink tracking-wide">
                  {trackingNo}
                </p>
                <button
                  type="button"
                  onClick={handleCopy}
                  className="text-xs text-brand-accent underline shrink-0 ml-3"
                >
                  {copied ? "복사됨" : "복사"}
                </button>
              </div>
            </div>

            <a
              href={`https://www.cjlogistics.com/ko/tool/parcel/tracking?gnbInvcNo=${trackingNo}`}
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center h-11 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
            >
              택배사 조회 페이지에서 확인하기 ↗
            </a>
            <p className="text-[11px] text-ink-faint mt-2 text-center">
              CJ대한통운 기준 링크예요. 다른 택배사로 발송됐다면 위 번호를
              복사해서 해당 택배사 홈페이지에서 조회해주세요.
            </p>
          </>
        )}

        <button
          type="button"
          onClick={onClose}
          className="w-full h-10 mt-4 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream"
        >
          닫기
        </button>
      </div>
    </div>
  );
};

export default OrderTrackingModal;
