import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyOrders } from "../../api/checkoutApi";
import { getOne as getProduct } from "../../api/productApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import OrderTrackingModal from "./OrderTrackingModal";
import ExchangeReturnModal from "./ExchangeReturnModal";

// 주문 상태값 -> 표시 라벨/색상. MailServiceImpl의 한글 매핑과 동일한 값 기준.
const STATUS_LABELS = {
  PENDING: { label: "주문 접수", className: "bg-surface text-ink-muted" },
  PAID: { label: "결제 완료", className: "bg-brand-light text-brand-accent" },
  SHIPPING: { label: "배송중", className: "bg-blue-50 text-blue-600" },
  DELIVERED: { label: "배송 완료", className: "bg-green-50 text-green-600" },
  CANCELLED: { label: "주문 취소", className: "bg-surface text-ink-faint" },
  REFUNDED: { label: "환불 완료", className: "bg-red-50 text-red-600" },
};

const getStatusMeta = (status) =>
  STATUS_LABELS[status] || {
    label: status,
    className: "bg-surface text-ink-muted",
  };

const ProductPaymentTab = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  // pno -> thumbnail 파일명 캐시 (OrderItemDTO에 썸네일이 없어서 상품 상세를 따로 조회)
  const [thumbnails, setThumbnails] = useState({});
  const [trackingOrder, setTrackingOrder] = useState(null);
  const [exchangeOrder, setExchangeOrder] = useState(null);

  useEffect(() => {
    getMyOrders()
      .then((data) => {
        setOrders(data);

        // 주문에 포함된 상품들의 pno를 모아서 중복 없이 썸네일 조회
        const pnoSet = new Set();
        data.forEach((order) =>
          order.items?.forEach((item) => pnoSet.add(item.pno)),
        );

        pnoSet.forEach((pno) => {
          getProduct(pno)
            .then((product) => {
              // getOne()이 반환하는 상세 DTO엔 thumbnail 필드가 없고
              // uploadFileNames 배열만 있음 (목록 조회용 DTO에만 thumbnail이 따로 있음)
              const firstImage = product.uploadFileNames?.[0];
              setThumbnails((prev) => ({ ...prev, [pno]: firstImage }));
            })
            .catch(() => {
              // 상품이 삭제됐거나 조회 실패해도 결제내역 자체는 그대로 보여줘야 하니 무시
            });
        });
      })
      .catch((e) => console.error(e))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="text-center text-ink-faint py-16">불러오는 중...</div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
        주문 내역이 없습니다.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {orders.map((order) => {
        const statusMeta = getStatusMeta(order.orderStatus);

        return (
          <div
            key={order.ono}
            className="bg-white rounded-2xl border border-line p-5"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="text-xs text-ink-faint">
                  {order.orderNumber}
                </span>
                <span className="text-xs text-ink-faint">
                  {order.regDate?.toString().slice(0, 10)}
                </span>
              </div>
              <span
                className={`text-[11px] px-2.5 py-1 rounded-full font-medium ${statusMeta.className}`}
              >
                {statusMeta.label}
              </span>
            </div>

            <div className="flex flex-col gap-3 mb-3">
              {order.items?.map((item, i) => {
                const thumbnail = thumbnails[item.pno];

                return (
                  <Link
                    key={i}
                    to={`/product/read/${item.pno}`}
                    className="flex items-center gap-3 group"
                  >
                    <div className="w-12 h-12 rounded-lg bg-surface overflow-hidden shrink-0">
                      {thumbnail && (
                        <img
                          src={`${API_SERVER_HOST}/api/product/view/s_${thumbnail}`}
                          alt={item.pname}
                          className="w-full h-full object-cover"
                        />
                      )}
                    </div>

                    <div className="flex items-center justify-between flex-1 min-w-0">
                      <span className="text-sm text-ink truncate group-hover:text-brand group-hover:underline">
                        {item.pname}{" "}
                        <span className="text-ink-faint">x {item.qty}</span>
                      </span>
                      <span className="text-ink-muted shrink-0 text-sm ml-2">
                        {(item.price * item.qty).toLocaleString()}원
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>

            {(() => {
              // 배송비는 서버 값(shippingFee/totalPrice)을 안 믿고, 상품 금액
              // 합계를 기준으로 여기서 직접 계산해서 보여줌 (3만원 미만이면 3천원)
              const itemsSubtotal =
                order.items?.reduce(
                  (sum, item) => sum + item.price * item.qty,
                  0,
                ) || 0;
              const shippingFee = itemsSubtotal < 30000 ? 3000 : 0;
              const finalTotal = itemsSubtotal + shippingFee;

              return (
                <div className="flex flex-col gap-1 pt-3 border-t border-line-soft">
                  <div className="flex items-center justify-between text-xs text-ink-faint">
                    <span>상품 금액</span>
                    <span>{itemsSubtotal.toLocaleString()}원</span>
                  </div>
                  <div className="flex items-center justify-between text-xs text-ink-faint">
                    <span>배송비</span>
                    <span>
                      {shippingFee > 0
                        ? `${shippingFee.toLocaleString()}원`
                        : "무료배송"}
                    </span>
                  </div>
                  <div className="flex items-center justify-between mt-1">
                    <span className="text-xs text-ink-faint">
                      {shippingFee > 0
                        ? "3만원 미만 주문은 배송비가 붙어요"
                        : "3만원 이상 주문은 무료배송이에요"}
                    </span>
                    <span className="text-sm font-medium text-ink">
                      총 {finalTotal.toLocaleString()}원
                    </span>
                  </div>
                </div>
              );
            })()}

            <div className="flex gap-2 mt-3 pt-3 border-t border-line-soft">
              {(order.orderStatus === "SHIPPING" ||
                order.orderStatus === "DELIVERED") && (
                <button
                  type="button"
                  onClick={() => setTrackingOrder(order)}
                  className="flex-1 h-9 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
                >
                  배송조회
                </button>
              )}
              {order.orderStatus === "DELIVERED" && (
                <button
                  type="button"
                  onClick={() => setExchangeOrder(order)}
                  className="flex-1 h-9 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
                >
                  교환/반품 신청
                </button>
              )}
            </div>
          </div>
        );
      })}

      {trackingOrder && (
        <OrderTrackingModal
          order={trackingOrder}
          onClose={() => setTrackingOrder(null)}
        />
      )}

      {exchangeOrder && (
        <ExchangeReturnModal
          order={exchangeOrder}
          onClose={() => setExchangeOrder(null)}
          onSubmitted={() =>
            alert("교환/반품 신청이 접수됐어요. 처리 결과를 안내드릴게요.")
          }
        />
      )}
    </div>
  );
};

export default ProductPaymentTab;
