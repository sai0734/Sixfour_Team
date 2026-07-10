import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyOrders } from "../../api/checkoutApi";
import { getOne as getProduct } from "../../api/productApi";
import { API_SERVER_HOST } from "../../api/reservationApi";

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

            <div className="flex items-center justify-end pt-3 border-t border-line-soft">
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
