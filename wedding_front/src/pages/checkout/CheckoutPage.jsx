import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import useCustomCart from "../../hooks/useCustomCart";
import useCustomLogin from "../../hooks/useCustomLogin";
import { prepareOrder, getLastAddress } from "../../api/checkoutApi";
import { TOSS_CLIENT_KEY } from "../../api/tossConfig";
import { API_SERVER_HOST } from "../../api/reservationApi";
import { calculateShippingFee } from "../../util/shippingPolicy";
import BasicLayout from "../../layouts/BasicLayout";

const host = API_SERVER_HOST;

let tossScriptPromise = null;
const loadTossScript = () => {
  if (tossScriptPromise) return tossScriptPromise;

  tossScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://js.tosspayments.com/v1/payment";
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });

  return tossScriptPromise;
};

const CheckoutPage = () => {
  const { cartItems } = useCustomCart();
  const { loginState } = useCustomLogin();
  const location = useLocation();

  const directItem = location.state?.directItem;
  const selectedCinos = location.state?.selectedCinos;

  const safeCartItems = directItem
    ? [directItem]
    : selectedCinos
      ? (Array.isArray(cartItems) ? cartItems : []).filter((item) =>
          selectedCinos.includes(item.cino),
        )
      : Array.isArray(cartItems)
        ? cartItems
        : [];

  const [receiverName, setReceiverName] = useState("");
  const [receiverPhone, setReceiverPhone] = useState("");
  const [zipcode, setZipcode] = useState("");
  const [address, setAddress] = useState("");
  const [addressDetail, setAddressDetail] = useState("");
  const [request, setRequest] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const productSubtotal = safeCartItems.reduce(
    (sum, item) => sum + (item.price + (item.extraPrice || 0)) * item.qty,
    0,
  );
  const shippingFee =
    productSubtotal > 0 ? calculateShippingFee(productSubtotal) : 0;
  const totalPrice = productSubtotal + shippingFee;

  const handleClickSearchAddress = () => {
    if (!window.daum || !window.daum.Postcode) {
      alert(
        "주소 검색 기능을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
      return;
    }

    new window.daum.Postcode({
      oncomplete: (data) => {
        setZipcode(data.zonecode);
        setAddress(data.roadAddress || data.jibunAddress);
      },
    }).open();
  };

  // 우편번호/기본주소/상세주소를 각각 정확한 칸에 채우도록 변경
  const handleClickLoadLastAddress = () => {
    getLastAddress().then((data) => {
      if (!data) {
        alert("불러올 수 있는 최근 배송지가 없습니다.");
        return;
      }
      setReceiverName(data.receiverName);
      setReceiverPhone(data.receiverPhone);
      setZipcode(data.zipcode || "");
      setAddress(data.address);
      setAddressDetail(data.addressDetail || "");
    });
  };

  const handleClickPay = async () => {
    if (safeCartItems.length === 0) {
      alert("주문할 상품이 없습니다.");
      return;
    }
    if (!receiverName || !receiverPhone || !address) {
      alert("받으실 분 정보와 주소를 모두 입력해주세요.");
      return;
    }

    setSubmitting(true);

    try {
      const cinos = directItem ? [] : safeCartItems.map((item) => item.cino);

      const order = await prepareOrder({
        directItem: directItem
          ? {
              pno: directItem.pno,
              pono: directItem.pono,
              qty: directItem.qty,
            }
          : null,
        cinos,
        receiverName,
        receiverPhone,
        zipcode,
        address,
        addressDetail,
        request,
      });

      sessionStorage.setItem("checkout_cinos", JSON.stringify(cinos));
      sessionStorage.setItem("checkout_orderNumber", order.orderNumber);
      sessionStorage.setItem("checkout_amount", String(order.totalPrice));

      await loadTossScript();

      const tossPayments = window.TossPayments(TOSS_CLIENT_KEY);

      const orderName =
        safeCartItems.length > 1
          ? `${safeCartItems[0].pname} 외 ${safeCartItems.length - 1}건`
          : safeCartItems[0].pname;

      await tossPayments.requestPayment("카드", {
        amount: order.totalPrice,
        orderId: order.orderNumber,
        orderName,
        customerName: receiverName,
        successUrl: `${window.location.origin}/checkout/success`,
        failUrl: `${window.location.origin}/checkout/fail`,
      });
    } catch (err) {
      console.error(err);
      alert("결제가 취소되었거나 오류가 발생했습니다.");
      window.location.href = directItem
        ? `/product/read/${directItem.pno}`
        : "/cart";
    } finally {
      setSubmitting(false);
    }
  };

  if (!loginState.email) {
    return (
      <BasicLayout showCart={false}>
        <div className="max-w-[700px] mx-auto px-6 py-24 text-center text-ink-soft">
          로그인이 필요한 페이지입니다.
        </div>
      </BasicLayout>
    );
  }

  return (
    <BasicLayout showCart={false}>
      <div className="bg-white min-h-screen">
        <div className="max-w-[700px] mx-auto px-6 pt-10 pb-20">
          <p className="text-xs tracking-[0.15em] text-brand-accent mb-2">
            CHECKOUT
          </p>
          <p className="font-serif text-2xl mb-8">주문/결제</p>

          <div className="mb-8">
            <p className="text-sm font-medium mb-3">주문 상품</p>
            <div className="flex flex-col gap-3">
              {safeCartItems.map((item, i) => (
                <div
                  key={item.cino ?? i}
                  className="flex justify-between text-sm"
                >
                  <span>
                    {item.pname}
                    {item.optionValue && ` (${item.optionValue})`} × {item.qty}
                  </span>
                  <span>
                    {(
                      (item.price + (item.extraPrice || 0)) *
                      item.qty
                    ).toLocaleString()}
                    원
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="mb-8 border-t border-line pt-6">
            <div className="flex justify-between items-center mb-3">
              <p className="text-sm font-medium">배송지 정보</p>
              <button
                onClick={handleClickLoadLastAddress}
                className="text-xs text-brand-accent underline"
              >
                최근 배송지 불러오기
              </button>
            </div>

            <div className="flex flex-col gap-2">
              <input
                type="text"
                value={receiverName}
                onChange={(e) => setReceiverName(e.target.value)}
                placeholder="받으실 분"
                className="h-10 px-4 border border-line-soft rounded-lg text-sm"
              />
              <input
                type="text"
                value={receiverPhone}
                onChange={(e) => setReceiverPhone(e.target.value)}
                placeholder="연락처"
                className="h-10 px-4 border border-line-soft rounded-lg text-sm"
              />
              <div className="flex gap-2">
                <input
                  type="text"
                  value={zipcode}
                  readOnly
                  placeholder="우편번호"
                  className="h-10 px-4 border border-line-soft rounded-lg text-sm w-32 bg-cream"
                />
                <button
                  onClick={handleClickSearchAddress}
                  className="h-10 px-4 border border-line-soft rounded-lg text-xs"
                >
                  우편번호 찾기
                </button>
              </div>
              <input
                type="text"
                value={address}
                readOnly
                placeholder="주소 (우편번호 찾기로 자동 입력)"
                className="h-10 px-4 border border-line-soft rounded-lg text-sm bg-cream"
              />
              <input
                type="text"
                value={addressDetail}
                onChange={(e) => setAddressDetail(e.target.value)}
                placeholder="상세주소"
                className="h-10 px-4 border border-line-soft rounded-lg text-sm"
              />
              <textarea
                value={request}
                onChange={(e) => setRequest(e.target.value)}
                placeholder="배송 요청사항 (선택)"
                rows={2}
                className="p-3 border border-line-soft rounded-lg text-sm resize-none"
              />
            </div>
          </div>

          <div className="border-t border-line pt-6 mb-8">
            <div className="flex justify-between text-sm text-ink-soft mb-2">
              <span>상품금액</span>
              <span>{productSubtotal.toLocaleString()}원</span>
            </div>
            <div className="flex justify-between text-sm text-ink-soft mb-2">
              <span>배송비</span>
              <span>
                {shippingFee === 0
                  ? "무료"
                  : `${shippingFee.toLocaleString()}원`}
              </span>
            </div>
            <div className="flex justify-between items-baseline mt-3 pt-3 border-t border-line">
              <span className="text-sm text-ink-soft">결제 금액</span>
              <span className="text-xl font-medium">
                {totalPrice.toLocaleString()}원
              </span>
            </div>
          </div>

          <button
            onClick={handleClickPay}
            disabled={submitting}
            className="w-full h-12 rounded-full bg-brand text-white text-sm font-medium disabled:opacity-50"
          >
            {submitting ? "처리 중..." : "결제하기"}
          </button>
        </div>
      </div>
    </BasicLayout>
  );
};

export default CheckoutPage;
