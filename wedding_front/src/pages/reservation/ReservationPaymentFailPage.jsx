import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { cancelPayment } from "../../api/reservationApi";

// 재원 추가 - 토스 결제 실패/취소 시 failUrl 로 돌아오는 페이지
// CheckoutFailPage.jsx 와 동일한 패턴
const ReservationPaymentFailPage = () => {
  const { cmno } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const message = params.get("message");
    const reservationId = params.get("reservationId");

    if (reservationId) {
      cancelPayment(reservationId).catch((err) => console.error(err));
    }

    alert(message || "결제가 취소되었거나 실패했습니다.");

    navigate(`/companies/read/${cmno}`, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
};

export default ReservationPaymentFailPage;
