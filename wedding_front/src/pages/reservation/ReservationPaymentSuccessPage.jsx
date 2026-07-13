import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { confirmPayment } from "../../api/reservationApi";
import { getOne as getCompanyOne } from "../../api/companyApi";
import BasicLayout from "../../layouts/BasicLayout";
import FetchingModal from "../../components/common/FetchingModal";
import ShopTapeLabel from "../../components/product/ShopTapeLabel";

// 재원 추가 - 토스 결제 성공 시 successUrl 로 돌아오는 페이지
// CheckoutSuccessPage.jsx 와 동일한 패턴(paymentKey/orderId/amount 쿼리 → 서버 승인 호출)
const ReservationPaymentSuccessPage = () => {
  const { cmno } = useParams();
  const navigate = useNavigate();

  const [reservation, setReservation] = useState(null);
  const [companyName, setCompanyName] = useState("");
  const [fetching, setFetching] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentKey = params.get("paymentKey");
    const orderId = params.get("orderId");
    const amount = params.get("amount");
    const reservationId = params.get("reservationId");

    if (!reservationId) {
      setErrorMsg("잘못된 접근입니다.");
      setFetching(false);
      return;
    }

    confirmPayment(reservationId, {
      paymentKey,
      orderNumber: orderId,
      amount: Number(amount),
    })
      .then((data) => {
        setReservation(data);
        return getCompanyOne(data.cmno).catch(() => null);
      })
      .then((company) => {
        if (company) setCompanyName(company.name);
      })
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
            onClick={() => navigate(`/companies/read/${cmno}`)}
            className="h-11 px-6 rounded-full border border-line-soft text-sm"
          >
            업체 목록으로
          </button>
        </div>
      </BasicLayout>
    );
  }

  return (
    <BasicLayout showCart={false}>
      <div className="max-w-[600px] mx-auto px-6 py-16 text-center">
        <ShopTapeLabel className="mb-4">예약 결제 완료</ShopTapeLabel>
        <p className="font-['Gowun_Batang'] text-2xl mb-8 text-ink">
          예약이 확정되었습니다
        </p>

        <div className="bg-white rounded-2xl p-6 text-left mb-8 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          {companyName && (
            <div className="flex justify-between text-sm mb-2">
              <span className="text-ink-soft">업체</span>
              <span className="font-medium">{companyName}</span>
            </div>
          )}
          <div className="flex justify-between text-sm mb-2">
            <span className="text-ink-soft">옵션</span>
            <span>{reservation.optionName || "-"}</span>
          </div>
          <div className="flex justify-between text-sm mb-2">
            <span className="text-ink-soft">예약 날짜</span>
            <span>{reservation.weddingDate}</span>
          </div>
          <div className="flex justify-between text-sm mb-4">
            <span className="text-ink-soft">주문번호</span>
            <span className="text-right">{reservation.orderNumber}</span>
          </div>
          <div className="flex justify-between text-sm font-medium text-ink border-t border-line pt-3">
            <span>결제금액</span>
            <span>{Number(reservation.amount || 0).toLocaleString()}원</span>
          </div>
        </div>

        <div className="flex gap-2 justify-center">
          <button
            onClick={() => navigate("/companies/list")}
            className="h-11 px-8 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark transition"
          >
            업체 목록으로
          </button>
          <button
            onClick={() => navigate("/mypage")}
            className="h-11 px-8 rounded-full border border-line-soft text-sm hover:border-brand hover:text-brand transition"
          >
            내 예약 보기
          </button>
        </div>
      </div>
    </BasicLayout>
  );
};

export default ReservationPaymentSuccessPage;
