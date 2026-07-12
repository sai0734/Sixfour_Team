import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { confirmPayment } from "../../api/checkoutApi";
import useCustomCart from "../../hooks/useCustomCart";
import { API_SERVER_HOST } from "../../api/reservationApi";
import FetchingModal from "../../components/common/FetchingModal";
import BasicLayout from "../../layouts/BasicLayout";
import ShopTapeLabel from "../../components/product/ShopTapeLabel";

const host = API_SERVER_HOST;

const CheckoutSuccessPage = () => {
  const navigate = useNavigate();
  const { refreshCart } = useCustomCart();

  const [order, setOrder] = useState(null);
  const [fetching, setFetching] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentKey = params.get("paymentKey");
    const orderId = params.get("orderId");
    const amount = params.get("amount");

    const cinos = JSON.parse(sessionStorage.getItem("checkout_cinos") || "[]");

    confirmPayment({
      paymentKey,
      orderNumber: orderId,
      amount: Number(amount),
      cinos,
    })
      .then((data) => {
        setOrder(data);
        sessionStorage.removeItem("checkout_cinos");
        sessionStorage.removeItem("checkout_orderNumber");
        sessionStorage.removeItem("checkout_amount");
        refreshCart();
      })
      .catch((err) => {
        console.error(err);
        setErrorMsg("결제 승인 처리 중 문제가 발생했습니다.");
      })
      .finally(() => setFetching(false));
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
            onClick={() => navigate("/cart")}
            className="h-11 px-6 rounded-full border border-line-soft text-sm"
          >
            장바구니로 이동
          </button>
        </div>
      </BasicLayout>
    );
  }

  const productSubtotal = order.items.reduce(
    (sum, item) => sum + item.price * item.qty,
    0,
  );
  const shippingFee = order.shippingFee ?? order.totalPrice - productSubtotal;

  return (
    <BasicLayout showCart={false}>
      <div className="-mx-5 -mb-10 -mt-12 min-h-[calc(100vh-6rem)] bg-cream px-5 pt-16">
        <div className="max-w-[600px] mx-auto px-6 pb-20 text-center">
          <ShopTapeLabel className="mb-4">결제 완료</ShopTapeLabel>
          <p className="font-['Gowun_Batang'] text-2xl mb-8 text-ink">
            주문번호 {order.orderNumber}
          </p>

          <div className="bg-white rounded-2xl p-6 text-left mb-8 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
            <div className="flex justify-between text-sm mb-2">
              <span className="text-ink-soft">결제금액</span>
              <span className="font-medium">
                {order.totalPrice.toLocaleString()}원
              </span>
            </div>
            <div className="flex justify-between text-sm mb-2">
              <span className="text-ink-soft">받으실 분</span>
              <span>{order.receiverName}</span>
            </div>
            <div className="flex justify-between text-sm mb-4">
              <span className="text-ink-soft">배송지</span>
              <span className="text-right">{order.address}</span>
            </div>

            {/* 썸네일 추가된 상품 목록 */}
            <div className="border-t border-line pt-4 flex flex-col gap-2.5">
              {order.items.map((item, i) => (
                <div key={i} className="flex items-center gap-2.5">
                  <div className="w-9 h-9 shrink-0 rounded-lg overflow-hidden bg-surface">
                    {item.thumbnail && (
                      <img
                        alt={item.pname}
                        className="w-full h-full object-cover"
                        src={`${host}/api/product/view/s_${item.thumbnail}`}
                      />
                    )}
                  </div>
                  <div className="flex-1 flex justify-between text-xs text-ink-faint min-w-0">
                    <span className="truncate">
                      {item.pname} × {item.qty}
                    </span>
                    <span className="shrink-0 ml-2">
                      {(item.price * item.qty).toLocaleString()}원
                    </span>
                  </div>
                </div>
              ))}

              <div className="flex justify-between text-xs text-ink-faint pt-1">
                <span>배송비</span>
                <span>
                  {shippingFee === 0
                    ? "무료"
                    : `${shippingFee.toLocaleString()}원`}
                </span>
              </div>

              <div className="flex justify-between text-sm font-medium text-ink border-t border-line mt-2 pt-3">
                <span>합계</span>
                <span>{order.totalPrice.toLocaleString()}원</span>
              </div>
            </div>
          </div>

          <button
            onClick={() => navigate("/product/list")}
            className="h-11 px-8 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark transition"
          >
            쇼핑 계속하기
          </button>
        </div>
      </div>
    </BasicLayout>
  );
};

export default CheckoutSuccessPage;
