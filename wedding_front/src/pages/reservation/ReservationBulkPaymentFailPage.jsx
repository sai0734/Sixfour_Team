// 승진 코드 추가
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { cancelBulkPayment } from "../../api/reservationApi";
import BasicLayout from "../../layouts/BasicLayout";
import ShopTapeLabel from "../../components/product/ShopTapeLabel";

const ReservationBulkPaymentFailPage = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const idsParam = params.get("reservationIds");

    if (!idsParam) return;

    const reservationIds = idsParam.split(",").map(Number);

    cancelBulkPayment(reservationIds).catch((err) =>
      console.error("묶음 결제 취소 처리 오류:", err),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const params = new URLSearchParams(window.location.search);
  const code = params.get("code") || "";
  const message = params.get("message") || "결제가 취소되었습니다.";

  return (
    <BasicLayout showCart={false}>
      <div className="max-w-[500px] mx-auto px-6 py-24 text-center">
        <ShopTapeLabel className="mb-4">결제 실패</ShopTapeLabel>
        <p className="font-['Gowun_Batang'] text-2xl mb-4 text-ink">
          결제를 완료하지 못했습니다
        </p>
        <p className="text-sm text-ink-soft mb-2">{message}</p>
        {code && <p className="text-xs text-ink-faint mb-8">오류 코드: {code}</p>}

        <div className="flex gap-2 justify-center">
          <button
            onClick={() => navigate(-1)}
            className="h-11 px-8 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark transition"
          >
            다시 시도
          </button>
          <button
            onClick={() => navigate("/mypage")}
            className="h-11 px-8 rounded-full border border-line-soft text-sm hover:border-brand hover:text-brand transition"
          >
            마이페이지로
          </button>
        </div>
      </div>
    </BasicLayout>
  );
};

export default ReservationBulkPaymentFailPage;
// 승진 코드 추가 끝
