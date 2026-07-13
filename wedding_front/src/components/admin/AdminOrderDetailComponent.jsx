import { useEffect, useState } from "react";
import {
  createSearchParams,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import {
  getAdminOrderDetail,
  changeOrderStatus,
  updateOrderShipping,
  updateOrderTracking,
  updateOrderMemo,
  refundOrder,
} from "../../api/adminOrderApi";
import { getReviews, postReply, deleteReview } from "../../api/reviewApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import AdminLayout from "../../layouts/AdminLayout";
import ShopTapeLabel from "../product/ShopTapeLabel";

const host = API_SERVER_HOST;

const TERMINAL_STATUSES = ["REFUNDED", "CANCELLED"];

const STATUS_LABEL = {
  PAID: "결제완료",
  SHIPPING_READY: "배송준비",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  REFUNDED: "환불완료",
  CANCELLED: "주문취소",
  EXCHANGE_REQUESTED: "교환신청",
  REFUND_REQUESTED: "환불신청",
  EXCHANGE: "교환완료",
};

const STATUS_FLOW = ["PAID", "SHIPPING_READY", "SHIPPING", "DELIVERED"];

const SectionCard = ({ title, children }) => (
  <section className="bg-white rounded-2xl p-5 shadow-[0_6px_18px_-10px_rgba(58,54,47,0.18)] mb-5">
    <p className="text-sm font-medium text-brand-deep mb-3">{title}</p>
    {children}
  </section>
);

const AdminReviewMiniSection = ({ pno, pname, thumbnail, memberEmail }) => {
  const [reviews, setReviews] = useState([]);
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState("");

  const fetchReviews = () => {
    getReviews(pno).then((data) => {
      const onlyMine = data.filter((r) => r.memberEmail === memberEmail);
      setReviews(onlyMine);
    });
  };

  useEffect(() => {
    fetchReviews();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pno, memberEmail]);

  const handleReply = (rno) => {
    if (!replyContent.trim()) return;
    postReply(pno, rno, replyContent).then(() => {
      setReplyingTo(null);
      setReplyContent("");
      fetchReviews();
    });
  };

  const handleDelete = (rno) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    deleteReview(pno, rno).then(fetchReviews);
  };

  const ProductHeader = () => (
    <div className="flex items-center gap-2.5 mb-2">
      <div className="w-8 h-8 shrink-0 rounded-lg overflow-hidden bg-surface">
        {thumbnail && (
          <img
            alt={pname}
            className="w-full h-full object-cover"
            src={`${host}/api/product/view/s_${thumbnail}`}
          />
        )}
      </div>
      <p className="text-xs font-medium">{pname}</p>
    </div>
  );

  if (reviews.length === 0) {
    return (
      <div className="pb-3 mb-3 border-b border-line-soft last:border-b-0 last:mb-0 last:pb-0">
        <ProductHeader />
        <p className="text-xs text-ink-faint pl-[42px]">
          이 구매자가 작성한 리뷰가 없습니다.
        </p>
      </div>
    );
  }

  return (
    <div className="mb-4 last:mb-0">
      <ProductHeader />
      {reviews.map((r) => (
        <div key={r.rno} className="bg-cream rounded-xl p-3 mb-2 text-xs">
          <div className="flex justify-between">
            <span>
              {r.nickname} ·{" "}
              <span className="text-[#C9A96A]">
                {"★".repeat(r.rating || 0)}
              </span>
            </span>
            <button
              onClick={() => handleDelete(r.rno)}
              className="text-ink-faint underline"
            >
              삭제
            </button>
          </div>
          <p className="mt-1 text-ink-soft">{r.content}</p>

          {r.replies?.map((reply) => (
            <div
              key={reply.rno}
              className="bg-white rounded-lg p-2 mt-2 flex justify-between"
            >
              <span>답변: {reply.content}</span>
              <button
                onClick={() => handleDelete(reply.rno)}
                className="text-ink-faint underline"
              >
                삭제
              </button>
            </div>
          ))}

          {r.replies?.length === 0 &&
            (replyingTo === r.rno ? (
              <div className="flex gap-2 mt-2">
                <input
                  value={replyContent}
                  onChange={(e) => setReplyContent(e.target.value)}
                  className="flex-1 border border-line-soft rounded-full px-3 py-1 bg-white"
                />
                <button
                  onClick={() => handleReply(r.rno)}
                  className="text-brand-deep underline"
                >
                  등록
                </button>
              </div>
            ) : (
              <button
                onClick={() => setReplyingTo(r.rno)}
                className="text-brand-deep underline mt-2"
              >
                답변 달기
              </button>
            ))}
        </div>
      ))}
    </div>
  );
};

const AdminOrderDetailComponent = ({ ono }) => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [order, setOrder] = useState(null);

  const [shippingForm, setShippingForm] = useState({
    receiverName: "",
    receiverPhone: "",
    zipcode: "",
    address: "",
    addressDetail: "",
  });
  const [trackingNo, setTrackingNo] = useState("");
  const [memo, setMemo] = useState("");

  const handleBackToList = () => {
    const page = searchParams.get("page");
    const size = searchParams.get("size") || "10";

    if (page) {
      navigate({
        pathname: "/admin/orders",
        search: createSearchParams({ page, size }).toString(),
      });
    } else {
      navigate("/admin/orders");
    }
  };

  const fetchDetail = () => {
    getAdminOrderDetail(ono).then((data) => {
      setOrder(data);
      setShippingForm({
        receiverName: data.receiverName ?? "",
        receiverPhone: data.receiverPhone ?? "",
        zipcode: data.zipcode ?? "",
        address: data.address ?? "",
        addressDetail: data.addressDetail ?? "",
      });
      setTrackingNo(data.trackingNo ?? "");
      setMemo(data.adminMemo ?? "");
    });
  };

  useEffect(() => {
    fetchDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ono]);

  if (!order) {
    return (
      <AdminLayout>
        <div className="bg-white rounded-2xl p-10 text-center text-ink-faint text-sm shadow-[0_6px_18px_-10px_rgba(58,54,47,0.18)]">
          불러오는 중...
        </div>
      </AdminLayout>
    );
  }

  const isTerminalOrder = TERMINAL_STATUSES.includes(order.orderStatus);
  const isPaymentCanceled = order.payStatus === "CANCELED";
  const isStatusLocked = isTerminalOrder || isPaymentCanceled;
  const canRefund = !isStatusLocked;

  const handleChangeStatus = (newStatus) => {
    if (
      !window.confirm(
        `주문 상태를 "${STATUS_LABEL[newStatus]}"(으)로 변경하시겠습니까? 회원에게 이메일 알림이 발송됩니다.`,
      )
    )
      return;

    changeOrderStatus(ono, newStatus)
      .then(() => {
        alert("상태가 변경되었습니다.");
        fetchDetail();
      })
      .catch((err) => {
        console.error(err);
        alert(err.response?.data?.msg || "상태 변경에 실패했습니다.");
      });
  };

  const handleRefund = () => {
    const reason = window.prompt(
      "환불 사유를 입력해주세요.",
      order.exchangeReturnReason || "",
    );
    if (!reason) return;
    if (
      !window.confirm(
        "정말 환불 처리하시겠습니까? 이 작업은 되돌릴 수 없습니다.",
      )
    )
      return;

    refundOrder(ono, reason)
      .then(() => {
        alert("환불 처리가 완료되었습니다.");
        fetchDetail();
      })
      .catch((err) => {
        console.error("환불 실패:", err);
        console.error("서버 응답:", err.response?.data);
        alert(
          err.response?.data?.msg ||
            err.response?.data?.error ||
            "환불 처리에 실패했습니다. 콘솔을 확인해주세요.",
        );
      });
  };

  const handleExchangeComplte = () => {
    if (
      !window.confirm(
        "교환 처리하시겠습니까? 주문 상태가 '교환완료'로 변경됩니다.",
      )
    )
      return;

    changeOrderStatus(ono, "EXCHANGE")
      .then(() => {
        alert("교환 처리가 완료되었습니다.");
        fetchDetail();
      })
      .catch((err) => {
        console.error(err);
        alert(err.response?.data?.msg || "교환처리에 실패했습니다.");
      });
  };

  const handleSaveShipping = () => {
    updateOrderShipping(ono, shippingForm)
      .then(() => {
        alert("배송지 정보가 저장되었습니다.");
        fetchDetail();
      })
      .catch((err) => {
        console.error(err);
        alert("배송지 저장에 실패했습니다.");
      });
  };

  const handleSaveTracking = () => {
    updateOrderTracking(ono, trackingNo)
      .then(() => {
        alert("운송장 번호가 저장되었습니다.");
        fetchDetail();
      })
      .catch((err) => {
        console.error(err);
        alert("운송장 저장에 실패했습니다.");
      });
  };

  const handleSaveMemo = () => {
    updateOrderMemo(ono, memo)
      .then(() => {
        alert("메모가 저장되었습니다.");
      })
      .catch((err) => {
        console.error(err);
        alert("메모 저장에 실패했습니다.");
      });
  };

  const uniqueProducts = Array.from(
    new Map(order.items.map((i) => [i.pno, i])).values(),
  );

  return (
    <AdminLayout>
      <div className="flex flex-wrap justify-between items-center gap-3 mb-6">
        <div>
          <ShopTapeLabel className="mb-2.5">관리자</ShopTapeLabel>
          <p className="font-['Gowun_Batang'] text-2xl text-ink">
            주문 상세 - {order.orderNumber}
          </p>
        </div>
        <button
          onClick={handleBackToList}
          className="text-xs text-ink-faint underline"
        >
          목록으로
        </button>
      </div>

      <SectionCard title="주문 정보">
        <p className="text-sm text-ink-soft">주문자: {order.memberEmail}</p>
        <p className="text-sm text-ink-soft">
          받으실 분: {order.receiverName} / {order.receiverPhone}
        </p>
        <p className="text-sm text-ink-soft">
          배송지: [{order.zipcode}] {order.address} {order.addressDetail}
        </p>
        {order.request && (
          <p className="text-sm text-ink-soft">요청사항: {order.request}</p>
        )}

        <div className="mt-3 flex flex-col gap-2 bg-cream rounded-xl p-3">
          {order.items.map((item, i) => (
            <div key={i} className="flex items-center gap-3">
              <div className="w-10 h-10 shrink-0 rounded-lg overflow-hidden bg-surface">
                {item.thumbnail && (
                  <img
                    alt={item.pname}
                    className="w-full h-full object-cover"
                    src={`${host}/api/product/view/s_${item.thumbnail}`}
                  />
                )}
              </div>
              <div className="flex-1 flex justify-between text-sm min-w-0">
                <span className="truncate">
                  {item.pname} × {item.qty}
                </span>
                <span className="shrink-0 ml-2">
                  {(item.price * item.qty).toLocaleString()}원
                </span>
              </div>
            </div>
          ))}
        </div>
      </SectionCard>

      <SectionCard title="결제 내역">
        <p className="text-sm text-ink-soft">
          결제 수단: {order.payMethod ?? "-"}
        </p>
        <p className="text-sm text-ink-soft">PG 거래ID: {order.pgTid ?? "-"}</p>
        <p className="text-sm text-ink-soft">
          결제 상태: {order.payStatus ?? "-"}
        </p>
        <p className="text-sm text-ink-soft">
          상품금액: {(order.totalPrice - order.shippingFee).toLocaleString()}원
          / 배송비: {order.shippingFee.toLocaleString()}원
        </p>
        <p className="text-sm font-medium mt-1">
          총 결제금액: {order.totalPrice.toLocaleString()}원
        </p>
      </SectionCard>

      <SectionCard
        title={`주문 상태 제어 (현재: ${STATUS_LABEL[order.orderStatus] ?? order.orderStatus})`}
      >
        {isStatusLocked && (
          <p className="text-xs text-ink-faint mb-3">
            환불/취소된 주문은 배송 상태를 변경할 수 없습니다.
          </p>
        )}
        <div className="flex gap-2 flex-wrap">
          {STATUS_FLOW.map((s) => (
            <button
              key={s}
              onClick={() => handleChangeStatus(s)}
              disabled={isStatusLocked || order.orderStatus === s}
              className={`h-9 px-4 rounded-full text-xs border transition ${
                order.orderStatus === s
                  ? "bg-brand text-white border-brand"
                  : "border-line-soft hover:border-brand hover:text-brand-deep"
              } ${isStatusLocked ? "opacity-40 cursor-not-allowed" : ""}`}
            >
              {STATUS_LABEL[s]}
            </button>
          ))}
        </div>
      </SectionCard>

      {order.exchangeReturnType && (
        <SectionCard title="교환/반품 신청 내용">
          <p className="text-sm text-ink-soft">
            신청 구분:{" "}
            {order.exchangeReturnType === "EXCHANGE" ? "교환" : "환불"}
          </p>
          <p className="text-sm text-ink-soft">
            사유: {order.exchangeReturnReason}
          </p>
          {order.exchangeReturnDetail && (
            <p className="text-sm text-ink-soft bg-cream rounded-lg p-3 mt-1">
              {order.exchangeReturnDetail}
            </p>
          )}
          {order.exchangeReturnRequestedAt && (
            <p className="text-xs text-ink-faint mt-1">
              신청일: {order.exchangeReturnRequestedAt.toString().slice(0, 10)}
            </p>
          )}

          {order.exchangeReturnType === "EXCHANGE" &&
            order.orderStatus === "EXCHANGE_REQUESTED" && (
              <button
                onClick={handleExchangeComplete}
                className="h-9 px-4 mt-3 rounded-full bg-brand text-white text-xs hover:bg-brand-dark transition"
              >
                교환처리
              </button>
            )}
        </SectionCard>
      )}

      <SectionCard title="환불 처리">
        {canRefund ? (
          <button
            onClick={handleRefund}
            className="h-9 px-4 rounded-full border border-red-300 text-red-600 text-xs hover:bg-red-50 transition"
          >
            환불 승인 (PG 결제취소)
          </button>
        ) : (
          <p className="text-sm text-ink-soft">
            환불 처리가 완료된 주문입니다. (결제 상태: {order.payStatus ?? "-"})
          </p>
        )}
      </SectionCard>

      <SectionCard title="배송 정보 등록/수정">
        <div className="flex flex-col gap-2 mb-3">
          <input
            value={shippingForm.receiverName}
            onChange={(e) =>
              setShippingForm({ ...shippingForm, receiverName: e.target.value })
            }
            placeholder="받으실 분"
            className="h-9 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
          />
          <input
            value={shippingForm.receiverPhone}
            onChange={(e) =>
              setShippingForm({
                ...shippingForm,
                receiverPhone: e.target.value,
              })
            }
            placeholder="연락처"
            className="h-9 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
          />
          <div className="flex gap-2">
            <input
              value={shippingForm.zipcode}
              onChange={(e) =>
                setShippingForm({ ...shippingForm, zipcode: e.target.value })
              }
              placeholder="우편번호"
              className="h-9 px-3 border border-line-soft rounded-lg text-sm w-28 focus:outline-none focus:border-brand"
            />
            <input
              value={shippingForm.address}
              onChange={(e) =>
                setShippingForm({ ...shippingForm, address: e.target.value })
              }
              placeholder="주소"
              className="h-9 px-3 border border-line-soft rounded-lg text-sm flex-1 focus:outline-none focus:border-brand"
            />
          </div>
          <input
            value={shippingForm.addressDetail}
            onChange={(e) =>
              setShippingForm({
                ...shippingForm,
                addressDetail: e.target.value,
              })
            }
            placeholder="상세주소"
            className="h-9 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
          />
        </div>
        <button
          onClick={handleSaveShipping}
          className="h-9 px-4 rounded-full bg-brand text-white text-xs hover:bg-brand-dark transition"
        >
          배송지 저장
        </button>

        <div className="flex gap-2 mt-4">
          <input
            value={trackingNo}
            onChange={(e) => setTrackingNo(e.target.value)}
            placeholder="운송장 번호"
            className="h-9 px-3 border border-line-soft rounded-lg text-sm flex-1 focus:outline-none focus:border-brand"
          />
          <button
            onClick={handleSaveTracking}
            className="h-9 px-4 rounded-full border border-line-soft text-xs hover:border-brand hover:text-brand-deep transition"
          >
            운송장 저장
          </button>
        </div>
      </SectionCard>

      <SectionCard title="이 구매자의 리뷰 모니터링 및 답글 관리">
        {uniqueProducts.map((item) => (
          <AdminReviewMiniSection
            key={item.pno}
            pno={item.pno}
            pname={item.pname}
            thumbnail={item.thumbnail}
            memberEmail={order.memberEmail}
          />
        ))}
      </SectionCard>

      <SectionCard title="관리자 전용 메모 (CS 요청사항 등)">
        <textarea
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          rows={3}
          className="w-full border border-line-soft rounded-lg p-3 text-sm resize-none focus:outline-none focus:border-brand"
        />
        <button
          onClick={handleSaveMemo}
          className="h-9 px-4 mt-2 rounded-full border border-line-soft text-xs hover:border-brand hover:text-brand-deep transition"
        >
          메모 저장
        </button>
      </SectionCard>
    </AdminLayout>
  );
};

export default AdminOrderDetailComponent;
