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
import AdminLayout from "../../layouts/AdminLayout";

const STATUS_LABEL = {
  PAID: "결제완료",
  SHIPPING_READY: "배송준비",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  REFUNDED: "환불완료",
  CANCELLED: "주문취소",
};

const STATUS_FLOW = ["PAID", "SHIPPING_READY", "SHIPPING", "DELIVERED"];

const AdminReviewMiniSection = ({ pno, pname }) => {
  const [reviews, setReviews] = useState([]);
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState("");

  const fetchReviews = () => {
    getReviews(pno).then((data) => setReviews(data));
  };

  useEffect(() => {
    fetchReviews();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pno]);

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

  if (reviews.length === 0) {
    return (
      <div className="text-xs text-ink-faint py-2">
        {pname}: 등록된 리뷰가 없습니다.
      </div>
    );
  }

  return (
    <div className="mb-4">
      <p className="text-xs font-medium mb-2">{pname}</p>
      {reviews.map((r) => (
        <div
          key={r.rno}
          className="border border-line rounded-lg p-3 mb-2 text-xs"
        >
          <div className="flex justify-between">
            <span>
              {r.nickname} · {"★".repeat(r.rating || 0)}
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
              className="bg-cream rounded p-2 mt-2 flex justify-between"
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
                  className="flex-1 border border-line-soft rounded px-2 py-1"
                />
                <button
                  onClick={() => handleReply(r.rno)}
                  className="text-brand-accent underline"
                >
                  등록
                </button>
              </div>
            ) : (
              <button
                onClick={() => setReplyingTo(r.rno)}
                className="text-brand-accent underline mt-2"
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
        <div className="p-10 text-center text-ink-faint text-sm">
          불러오는 중...
        </div>
      </AdminLayout>
    );
  }

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
    const reason = window.prompt("환불 사유를 입력해주세요.");
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
      <div className="flex justify-between items-center mb-5">
        <p className="font-serif text-2xl">주문 상세 - {order.orderNumber}</p>
        <button
          onClick={handleBackToList}
          className="text-xs text-ink-faint underline"
        >
          목록으로
        </button>
      </div>

      <section className="border-t border-line pt-4 mb-6">
        <p className="text-sm font-medium mb-3">주문 정보</p>
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

        <div className="mt-3 flex flex-col gap-1">
          {order.items.map((item, i) => (
            <div key={i} className="flex justify-between text-sm">
              <span>
                {item.pname} × {item.qty}
              </span>
              <span>{(item.price * item.qty).toLocaleString()}원</span>
            </div>
          ))}
        </div>
      </section>

      <section className="border-t border-line pt-4 mb-6">
        <p className="text-sm font-medium mb-3">결제 내역</p>
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
      </section>

      <section className="border-t border-line pt-4 mb-6">
        <p className="text-sm font-medium mb-3">
          주문 상태 제어 (현재:{" "}
          {STATUS_LABEL[order.orderStatus] ?? order.orderStatus})
        </p>
        <div className="flex gap-2 flex-wrap">
          {STATUS_FLOW.map((s) => (
            <button
              key={s}
              onClick={() => handleChangeStatus(s)}
              disabled={order.orderStatus === s}
              className={`h-9 px-4 rounded-full text-xs border ${
                order.orderStatus === s
                  ? "bg-brand text-white border-brand"
                  : "border-line-soft"
              }`}
            >
              {STATUS_LABEL[s]}
            </button>
          ))}
        </div>
      </section>

      {order.orderStatus !== "REFUNDED" &&
        order.orderStatus !== "CANCELLED" && (
          <section className="border-t border-line pt-4 mb-6">
            <p className="text-sm font-medium mb-3">환불 처리</p>
            <button
              onClick={handleRefund}
              className="h-9 px-4 rounded-full border border-red-300 text-red-600 text-xs"
            >
              환불 승인 (PG 결제취소)
            </button>
          </section>
        )}

      <section className="border-t border-line pt-4 mb-6">
        <p className="text-sm font-medium mb-3">배송 정보 등록/수정</p>
        <div className="flex flex-col gap-2 mb-3">
          <input
            value={shippingForm.receiverName}
            onChange={(e) =>
              setShippingForm({ ...shippingForm, receiverName: e.target.value })
            }
            placeholder="받으실 분"
            className="h-9 px-3 border border-line-soft rounded-lg text-sm"
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
            className="h-9 px-3 border border-line-soft rounded-lg text-sm"
          />
          <div className="flex gap-2">
            <input
              value={shippingForm.zipcode}
              onChange={(e) =>
                setShippingForm({ ...shippingForm, zipcode: e.target.value })
              }
              placeholder="우편번호"
              className="h-9 px-3 border border-line-soft rounded-lg text-sm w-28"
            />
            <input
              value={shippingForm.address}
              onChange={(e) =>
                setShippingForm({ ...shippingForm, address: e.target.value })
              }
              placeholder="주소"
              className="h-9 px-3 border border-line-soft rounded-lg text-sm flex-1"
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
            className="h-9 px-3 border border-line-soft rounded-lg text-sm"
          />
        </div>
        <button
          onClick={handleSaveShipping}
          className="h-9 px-4 rounded-full bg-brand text-white text-xs"
        >
          배송지 저장
        </button>

        <div className="flex gap-2 mt-4">
          <input
            value={trackingNo}
            onChange={(e) => setTrackingNo(e.target.value)}
            placeholder="운송장 번호"
            className="h-9 px-3 border border-line-soft rounded-lg text-sm flex-1"
          />
          <button
            onClick={handleSaveTracking}
            className="h-9 px-4 rounded-full border border-line-soft text-xs"
          >
            운송장 저장
          </button>
        </div>
      </section>

      <section className="border-t border-line pt-4 mb-6">
        <p className="text-sm font-medium mb-3">
          해당 상품 리뷰 모니터링 및 답글 관리
        </p>
        {uniqueProducts.map((item) => (
          <AdminReviewMiniSection
            key={item.pno}
            pno={item.pno}
            pname={item.pname}
          />
        ))}
      </section>

      <section className="border-t border-line pt-4 mb-10">
        <p className="text-sm font-medium mb-3">
          관리자 전용 메모 (CS 요청사항 등)
        </p>
        <textarea
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          rows={3}
          className="w-full border border-line-soft rounded-lg p-3 text-sm resize-none"
        />
        <button
          onClick={handleSaveMemo}
          className="h-9 px-4 mt-2 rounded-full border border-line-soft text-xs"
        >
          메모 저장
        </button>
      </section>
    </AdminLayout>
  );
};

export default AdminOrderDetailComponent;
