// 승진 코드 추가
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { confirmBulkPayment } from "../../api/reservationApi";
import BasicLayout from "../../layouts/BasicLayout";
import FetchingModal from "../../components/common/FetchingModal";
import ShopTapeLabel from "../../components/product/ShopTapeLabel";

const ReservationBulkPaymentSuccessPage = () => {
  const navigate = useNavigate();
  const [reservations, setReservations] = useState([]);
  const [fetching, setFetching] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentKey = params.get("paymentKey");
    const orderId = params.get("orderId");
    const amount = params.get("amount");
    const idsParam = params.get("reservationIds");

    if (!orderId || !idsParam) {
      setErrorMsg("잘못된 접근입니다.");
      setFetching(false);
      return;
    }

    const reservationIds = idsParam.split(",").map(Number);

    confirmBulkPayment({
      paymentKey,
      orderNumber: orderId,
      amount: Number(amount),
      reservationIds,
    })
      .then((data) => setReservations(data))
      .catch((err) => {
        console.error(err);
        setErrorMsg("결제 승인 처리 중 문제가 발생했습니다.");
      })
      .finally(() => setFetching(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (fetching) {
    return (
      <BasicLayout showCart={false}>
        <FetchingModal />
      </BasicLayout>
    );
  }

  if (errorMsg) {
    return (
      <BasicLayout showCart={false}>
        <div className="max-w-[600px] mx-auto px-6 py-24 text-center">
          <p className="text-sm text-ink-soft mb-6">{errorMsg}</p>
          <button
            onClick={() => navigate("/mypage?tab=plan")}
            className="h-11 px-6 rounded-full border border-line-soft text-sm"
          >
            마이페이지로
          </button>
        </div>
      </BasicLayout>
    );
  }

  const totalAmount = reservations.reduce((sum, r) => sum + (r.amount || 0), 0);
  const orderNumber = reservations[0]?.orderNumber || "";

  return (
    <BasicLayout showCart={false}>
      <div className="max-w-[600px] mx-auto px-6 py-16 text-center">
        <ShopTapeLabel className="mb-4">묶음 예약 결제 완료</ShopTapeLabel>
        <p className="font-['Gowun_Batang'] text-2xl mb-8 text-ink">
          예약이 확정되었습니다
        </p>

        <div className="bg-white rounded-2xl p-6 text-left mb-8 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          <p className="text-xs text-ink-faint mb-4">주문번호 {orderNumber}</p>

          {reservations.map((r) => (
            <div
              key={r.reservationId}
              className="flex justify-between text-sm mb-2 pb-2 border-b border-line last:border-0 last:mb-0 last:pb-0"
            >
              <span className="text-ink-soft">업체 #{r.cmno} · {r.optionName || "옵션 미정"}</span>
              <span className="font-medium">{Number(r.amount || 0).toLocaleString()}원</span>
            </div>
          ))}

          <div className="flex justify-between text-sm font-semibold text-ink mt-4 pt-3 border-t border-line">
            <span>총 결제금액</span>
            <span>{totalAmount.toLocaleString()}원</span>
          </div>
        </div>

        <div className="flex gap-2 justify-center flex-wrap">
          <button
            onClick={() => navigate("/mypage?tab=payment&psub=vendor")}
            className="h-11 px-8 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark transition"
          >
            결제내역 보기
          </button>
          <button
            onClick={() => navigate("/companies/list")}
            className="h-11 px-8 rounded-full border border-line-soft text-sm hover:border-brand hover:text-brand transition"
          >
            업체 목록으로
          </button>
        </div>
      </div>
    </BasicLayout>
  );
};

export default ReservationBulkPaymentSuccessPage;
// 승진 코드 추가 끝
