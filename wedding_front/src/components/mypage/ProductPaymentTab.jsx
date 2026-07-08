import { useEffect, useState } from "react";
import { getMyOrders } from "../../api/checkoutApi";

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

  useEffect(() => {
    getMyOrders()
      .then((data) => setOrders(data))
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

            <div className="flex flex-col gap-1.5 mb-3">
              {order.items?.map((item, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between text-sm"
                >
                  <span className="text-ink truncate">
                    {item.pname}{" "}
                    <span className="text-ink-faint">x {item.qty}</span>
                  </span>
                  <span className="text-ink-muted shrink-0">
                    {(item.price * item.qty).toLocaleString()}원
                  </span>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-between pt-3 border-t border-line-soft">
              <span className="text-xs text-ink-faint">
                배송비 {order.shippingFee?.toLocaleString() || 0}원
              </span>
              <span className="text-sm font-medium text-ink">
                총 {order.totalPrice?.toLocaleString()}원
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default ProductPaymentTab;
