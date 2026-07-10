import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { cancelOrder } from "../../api/checkoutApi";

const CheckoutFailPage = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get("orderId");
    const message = params.get("message");

    // 참고: 장바구니는 결제 승인 전까지 건드리지 않으므로 별도 "복구" 로직이 필요 없음
    if (orderId) {
      cancelOrder(orderId).catch((err) => console.error(err));
    }

    alert(message || "결제가 취소되었거나 실패했습니다.");

    sessionStorage.removeItem("checkout_cinos");
    sessionStorage.removeItem("checkout_orderNumber");
    sessionStorage.removeItem("checkout_amount");

    navigate("/cart", { replace: true });
  }, []);

  return null;
};

export default CheckoutFailPage;
