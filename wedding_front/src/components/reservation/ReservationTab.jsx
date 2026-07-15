// 승진 코드 추가
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getListByMember,
  putOne,
  deleteOne,
  prepareBulkPayment,
  cancelBulkPayment,
  preparePayment,
  cancelPayment,
} from "../../api/reservationApi";
import { getOne as getCompanyOne } from "../../api/companyApi";
import { TOSS_CLIENT_KEY } from "../../api/tossConfig";
import { categoryLabel } from "../../util/companyOptionBuilder";
import useCustomLogin from "../../hooks/useCustomLogin";
import ReservationFormModal from "./ReservationFormModal";

const STATUS_STYLE = {
  예약대기: "bg-amber-50 text-amber-700",
  결제대기: "bg-blue-50 text-blue-700",
  확정: "bg-green-50 text-green-700",
  취소: "bg-red-50 text-red-600",
};

// 승진 코드 추가 - 예약대기/결제대기 구분 (status 기준)
const isPaymentPending = (r) =>
  r.status === "결제대기" &&
  r.amount > 0 &&
  (r.payStatus === "NONE" || r.payStatus === "CANCELLED");

const getReservationPhase = (r) => {
  if (isPaymentPending(r)) return "결제대기";
  if (r.status === "대기") return "예약대기";
  if (r.status === "확정" || r.payStatus === "PAID") return "확정";
  if (r.status === "취소") return "취소";
  return "예약대기";
};

const canEditReservation = (r) => r.status === "대기";

const getPayBadge = (r) => {
  if (r.status === "대기") {
    return { label: "업체 확인중", style: "bg-amber-50 text-amber-600" };
  }
  if (isPaymentPending(r)) {
    return { label: "미결제", style: PAY_STYLE.NONE };
  }
  return {
    label: PAY_LABEL[r.payStatus] || "미결제",
    style: PAY_STYLE[r.payStatus] || PAY_STYLE.NONE,
  };
};
// 승진 코드 추가 끝

const PAY_STYLE = {
  NONE: "bg-zinc-100 text-zinc-500",
  PAID: "bg-emerald-50 text-emerald-700",
  CANCELLED: "bg-red-50 text-red-500",
};
const PAY_LABEL = { NONE: "미결제", PAID: "결제완료", CANCELLED: "결제취소" };

// 카테고리별 아이콘
const CATEGORY_ICON = {
  HALL: "🏛",
  DRESS: "👗",
  MAKEUP: "💄",
  STUDIO: "📷",
};

// Toss 스크립트 singleton
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

const ReservationTab = () => {
  const { loginState } = useCustomLogin();
  const navigate = useNavigate();

  const [reservations, setReservations] = useState([]);
  const [companyMap, setCompanyMap] = useState({});
  const [refresh, setRefresh] = useState(false);
  const [loading, setLoading] = useState(false);

  const [modalMode, setModalMode] = useState(null);
  const [editTarget, setEditTarget] = useState(null);

  const [selectedIds, setSelectedIds] = useState(new Set());
  const [paying, setPaying] = useState(false);
  const [payingId, setPayingId] = useState(null);

  const tossRef = useRef(null);

  // 예약 목록 로드
  useEffect(() => {
    if (!loginState.email) return;
    setLoading(true);
    getListByMember(loginState.email)
      .then(async (data) => {
        setReservations(data);
        const uniqueCmnos = [...new Set(data.map((r) => r.cmno).filter(Boolean))];
        const results = await Promise.allSettled(
          uniqueCmnos.map((cmno) => getCompanyOne(cmno)),
        );
        const map = {};
        results.forEach((res, i) => {
          if (res.status === "fulfilled") map[uniqueCmnos[i]] = res.value;
        });
        setCompanyMap(map);
      })
      .catch((e) => console.error(e))
      .finally(() => setLoading(false));
  }, [loginState.email, refresh]);

  // Toss SDK 미리 로드
  useEffect(() => {
    loadTossScript()
      .then(() => {
        if (window.TossPayments) tossRef.current = window.TossPayments(TOSS_CLIENT_KEY);
      })
      .catch((e) => console.error("Toss 스크립트 로드 실패:", e));
  }, []);

  // 모달
  const openEdit = (r) => {
    if (!canEditReservation(r)) {
      alert("결제대기 상태에서는 일정을 변경할 수 없습니다.");
      return;
    }
    setEditTarget(r);
    setModalMode("edit");
  };
  const closeModal = () => { setModalMode(null); setEditTarget(null); };

  const handleSubmit = (formValues) => {
    putOne({ ...editTarget, ...formValues })
      .then(() => { closeModal(); setRefresh((r) => !r); })
      .catch((e) => {
        console.error(e);
        alert(e?.response?.data?.message || "예약 수정에 실패했습니다.");
      });
  };

  const handleDelete = (reservationId) => {
    if (!window.confirm("예약을 취소하시겠습니까?")) return;
    deleteOne(reservationId)
      .then(() => setRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  // 승진 코드 추가 - PAID 예약은 결제내역으로 이동, 예약현황에서 제외
  const displayReservations = reservations.filter((r) => r.payStatus !== "PAID");
  // 승진 코드 추가 끝

  // 체크박스 (결제대기 = 미결제·결제취소 후 재결제 대상)
  const payableIds = displayReservations
    .filter((r) => isPaymentPending(r))
    .map((r) => r.reservationId);

  const toggleSelect = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };
  const toggleSelectAll = () => {
    setSelectedIds(
      selectedIds.size === payableIds.length ? new Set() : new Set(payableIds),
    );
  };

  // 묶음 결제
  const handleBulkPay = async () => {
    if (selectedIds.size === 0) { alert("결제할 예약을 선택해주세요."); return; }
    if (paying) return;
    try {
      setPaying(true);
      if (!tossRef.current) {
        await loadTossScript();
        tossRef.current = window.TossPayments(TOSS_CLIENT_KEY);
      }
      const ids = [...selectedIds];
      const { orderNumber, totalAmount } = await prepareBulkPayment(ids);
      const selectedList = reservations.filter((r) => ids.includes(r.reservationId));
      const orderName =
        selectedList.length === 1
          ? `${companyMap[selectedList[0].cmno]?.name || "업체"} 예약`
          : `업체 예약 묶음 결제 (${selectedList.length}건)`;
      const idsParam = ids.join(",");
      await tossRef.current.requestPayment("카드", {
        amount: totalAmount,
        orderId: orderNumber,
        orderName,
        customerName: loginState.nickname || loginState.email,
        successUrl: `${window.location.origin}/companies/reserve/bulk/success?reservationIds=${idsParam}`,
        failUrl: `${window.location.origin}/companies/reserve/bulk/fail?reservationIds=${idsParam}`,
      });
    } catch (err) {
      if (err?.code === "USER_CANCEL") {
        await cancelBulkPayment([...selectedIds]).catch(() => {});
      } else {
        console.error("묶음 결제 오류:", err);
        const msg =
          err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          "결제 처리 중 오류가 발생했습니다.";
        alert(msg);
      }
    } finally {
      setPaying(false);
    }
  };

  // 단건 결제
  const handleSinglePay = async (r) => {
    if (paying || payingId) return;
    try {
      setPayingId(r.reservationId);
      if (!tossRef.current) {
        await loadTossScript();
        tossRef.current = window.TossPayments(TOSS_CLIENT_KEY);
      }
      const prepared = await preparePayment(r.reservationId);
      const company = companyMap[r.cmno];
      await tossRef.current.requestPayment("카드", {
        amount: prepared.amount || r.amount,
        orderId: prepared.orderNumber,
        orderName: `${company?.name || "업체"} - ${r.optionName || "예약"}`,
        customerName: loginState.nickname || loginState.email,
        successUrl: `${window.location.origin}/companies/reserve/${r.cmno}/success?reservationId=${r.reservationId}`,
        failUrl: `${window.location.origin}/companies/reserve/${r.cmno}/fail?reservationId=${r.reservationId}`,
      });
    } catch (err) {
      if (err?.code === "USER_CANCEL") {
        await cancelPayment(r.reservationId).catch(() => {});
      } else {
        console.error("결제 오류:", err);
        alert("결제 처리 중 오류가 발생했습니다.");
      }
    } finally {
      setPayingId(null);
    }
  };

  const unpaidCount = displayReservations.filter((r) => isPaymentPending(r)).length;
  const totalUnpaid = displayReservations
    .filter((r) => isPaymentPending(r))
    .reduce((s, r) => s + (r.amount || 0), 0);

  return (
    <div>
      {/* ── 상단 요약 + 액션 ── */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div className="flex items-center gap-3 flex-wrap">
          <span className="text-sm text-ink-muted">전체 {displayReservations.length}건</span>
          {unpaidCount > 0 && (
            <span className="text-sm font-medium text-amber-600">미결제 {unpaidCount}건</span>
          )}
          {payableIds.length > 0 && (
            <label className="flex items-center gap-1.5 text-xs text-ink-soft cursor-pointer select-none">
              <input
                type="checkbox"
                checked={selectedIds.size === payableIds.length && payableIds.length > 0}
                onChange={toggleSelectAll}
                className="w-3.5 h-3.5 accent-brand"
              />
              전체선택
            </label>
          )}
        </div>
        <div className="flex gap-2">
          {payableIds.length > 0 && (
            <button
              type="button"
              onClick={handleBulkPay}
              disabled={paying || selectedIds.size === 0}
              className="h-9 px-4 rounded-full bg-rose-500 text-white text-xs font-medium hover:bg-rose-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {paying
                ? "결제 중..."
                : selectedIds.size > 0
                  ? `일괄 결제 (${selectedIds.size}건)`
                  : "일괄 결제"}
            </button>
          )}
        </div>
      </div>

      {/* ── 합계 요약 바 ── */}
      {displayReservations.length > 0 && totalUnpaid > 0 && (
        <div className="flex gap-4 mb-5 px-4 py-3 bg-white rounded-xl border border-line text-xs text-ink-muted">
          <span>
            미결제{" "}
            <strong className="text-amber-600 text-sm">{totalUnpaid.toLocaleString()}원</strong>
          </span>
        </div>
      )}

      {/* ── 예약 없음 ── */}
      {!loading && displayReservations.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          등록된 예약이 없습니다.
        </div>
      )}

      {/* ── 카드 그리드 ── */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {displayReservations.map((r) => {
          const company = companyMap[r.cmno];
          const canPay = isPaymentPending(r);
          const payBadge = getPayBadge(r);
          const catIcon = CATEGORY_ICON[company?.category] || "🏢";
          const catName = categoryLabel[company?.category] || "업체";

          return (
            <div
              key={r.reservationId}
              className={`relative bg-white rounded-2xl border transition ${
                selectedIds.has(r.reservationId)
                  ? "border-brand shadow-sm"
                  : "border-line hover:border-brand/40"
              }`}
            >
              {/* 체크박스 (결제 가능한 예약만) */}
              {canPay && (
                <input
                  type="checkbox"
                  checked={selectedIds.has(r.reservationId)}
                  onChange={() => toggleSelect(r.reservationId)}
                  className="absolute top-3.5 left-3.5 w-4 h-4 accent-brand z-10"
                />
              )}

              {/* 카드 내용 */}
              <div
                className="p-4 cursor-pointer"
                onClick={() => company && navigate(`/companies/read/${r.cmno}`)}
              >
                {/* 상단: 카테고리 + 업체명 */}
                <div className={`flex items-start gap-2 ${canPay ? "pl-6" : ""}`}>
                  <span className="text-xl leading-none mt-0.5">{catIcon}</span>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-medium text-brand-deep">{catName}</p>
                    <p className="text-sm font-semibold text-ink truncate">
                      {company?.name || `업체 #${r.cmno}`}
                    </p>
                  </div>
                  {/* 결제 상태 뱃지 */}
                  <span
                    className={`shrink-0 text-[10px] px-2 py-0.5 rounded-full font-medium ${payBadge.style}`}
                  >
                    {payBadge.label}
                  </span>
                </div>

                {/* 구분선 */}
                <div className="my-3 border-t border-line" />

                {/* 중간: 옵션 + 날짜 */}
                <div className="flex flex-col gap-1 text-xs text-ink-muted">
                  <div className="flex justify-between">
                    <span className="text-ink-soft">옵션</span>
                    <span className="font-medium text-ink truncate max-w-[60%] text-right">
                      {r.optionName || "미정"}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-ink-soft">날짜</span>
                    <span>{r.weddingDate || "미정"}</span>
                  </div>
                  {r.amount > 0 && (
                    <div className="flex justify-between">
                      <span className="text-ink-soft">금액</span>
                      <span className="font-semibold text-ink">
                        {Number(r.amount).toLocaleString()}원
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* 하단: 예약 상태 + 버튼 */}
              <div
                className="flex items-center justify-between px-4 py-2.5 bg-surface rounded-b-2xl border-t border-line"
                onClick={(e) => e.stopPropagation()}
              >
                <span
                  className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[getReservationPhase(r)] || STATUS_STYLE["예약대기"]}`}
                >
                  {getReservationPhase(r)}
                </span>
                <div className="flex gap-1.5">
                  {getReservationPhase(r) === "결제대기" && (
                    <button
                      type="button"
                      onClick={() => handleSinglePay(r)}
                      disabled={payingId === r.reservationId}
                      className="h-7 px-3 rounded-full bg-brand text-[11px] font-medium text-white hover:bg-brand-deep transition disabled:opacity-50"
                    >
                      {payingId === r.reservationId ? "결제 중..." : "결제"}
                    </button>
                  )}
                  {canEditReservation(r) && (
                    <button
                      type="button"
                      onClick={() => openEdit(r)}
                      className="h-7 px-3 rounded-full border border-line-soft text-[11px] text-ink-soft hover:bg-white transition"
                    >
                      일정변경
                    </button>
                  )}
                  {getReservationPhase(r) === "결제대기" && (
                    <button
                      type="button"
                      onClick={() => handleDelete(r.reservationId)}
                      className="h-7 px-3 rounded-full border border-line-soft text-[11px] text-red-400 hover:bg-white transition"
                    >
                      예약 취소
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {modalMode === "edit" && editTarget && (
        <ReservationFormModal
          mode="edit"
          editTarget={editTarget}
          company={companyMap[editTarget.cmno]}
          onSubmit={handleSubmit}
          onClose={closeModal}
        />
      )}
    </div>
  );
};

export default ReservationTab;
// 승진 코드 추가 끝
