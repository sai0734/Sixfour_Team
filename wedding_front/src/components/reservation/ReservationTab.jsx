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
import { getOne as getCompanyOne, getCompanyImageUrl } from "../../api/companyApi";
import { TOSS_CLIENT_KEY } from "../../api/tossConfig";
import { buildCompanyOptions, categoryLabel } from "../../util/companyOptionBuilder";
import useCustomLogin from "../../hooks/useCustomLogin";
import ReservationFormModal from "./ReservationFormModal";
import { showAlert } from "../../util/globalAlert";
import { showConfirm } from "../../util/globalConfirm";

const STATUS_STYLE = {
  예약대기: "bg-amber-50 text-amber-700",
  결제대기: "bg-blue-50 text-blue-700",
  확정: "bg-green-50 text-green-700",
  취소: "bg-red-50 text-red-600",
};

// 재원 추가 - 결제 최소 기한(paymentDeadline)이 지났는지 판별
const isPaymentDeadlinePassed = (r) =>
  Boolean(
    r.paymentDeadline &&
    new Date(r.paymentDeadline) < new Date(new Date().toDateString()),
  );
// 재원 추가 끝

// 승진 코드 추가 - 예약대기/결제대기 구분 (status 기준)
const isPaymentPending = (r) =>
  r.status === "결제대기" &&
  r.amount > 0 &&
  (r.payStatus === "NONE" || r.payStatus === "CANCELLED") &&
  !isPaymentDeadlinePassed(r); // 재원 추가 - 기한 지난 건 결제/일괄결제 대상에서 제외

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
  // 재원 추가 - 결제대기인데 마감일이 지난 경우
  if (r.status === "결제대기" && isPaymentDeadlinePassed(r)) {
    return { label: "결제기한 만료", style: "bg-red-50 text-red-600" };
  }
  // 재원 추가 끝
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

const getReservationImage = (company, optionName) => {
  if (!company) return null;

  if (optionName) {
    const matchedOption = buildCompanyOptions(company).find(
      (option) => option.label === optionName,
    );
    if (matchedOption?.image) {
      return matchedOption.image;
    }
  }

  return company.mainImage || company.uploadFileNames?.[0] || null;
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
        const uniqueCmnos = [
          ...new Set(data.map((r) => r.cmno).filter(Boolean)),
        ];
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
        if (window.TossPayments)
          tossRef.current = window.TossPayments(TOSS_CLIENT_KEY);
      })
      .catch((e) => console.error("Toss 스크립트 로드 실패:", e));
  }, []);

  // 모달
  const openEdit = (r) => {
    if (!canEditReservation(r)) {
      showAlert("결제대기 상태에서는 일정을 변경할 수 없습니다.");
      return;
    }
    setEditTarget(r);
    setModalMode("edit");
  };
  const closeModal = () => {
    setModalMode(null);
    setEditTarget(null);
  };

  const handleSubmit = (formValues) => {
    putOne({ ...editTarget, ...formValues })
      .then(() => {
        closeModal();
        setRefresh((r) => !r);
      })
      .catch((e) => {
        console.error(e);
        showAlert(e?.response?.data?.message || "예약 수정에 실패했습니다.");
      });
  };

  const handleDelete = async (reservationId) => {
    if (!(await showConfirm("예약을 취소하시겠습니까?"))) return;
    deleteOne(reservationId)
      .then(() => setRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  // 승진 코드 추가 - PAID 예약은 결제내역으로 이동, 예약현황에서 제외
  const displayReservations = reservations.filter(
    (r) => r.payStatus !== "PAID",
  );
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
    if (selectedIds.size === 0) {
      showAlert("결제할 예약을 선택해주세요.");
      return;
    }
    if (paying) return;
    try {
      setPaying(true);
      if (!tossRef.current) {
        await loadTossScript();
        tossRef.current = window.TossPayments(TOSS_CLIENT_KEY);
      }
      const ids = [...selectedIds];
      const { orderNumber, totalAmount } = await prepareBulkPayment(ids);
      const selectedList = reservations.filter((r) =>
        ids.includes(r.reservationId),
      );
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
        showAlert(msg);
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
        showAlert("결제 처리 중 오류가 발생했습니다.");
      }
    } finally {
      setPayingId(null);
    }
  };

  const unpaidCount = displayReservations.filter((r) =>
    isPaymentPending(r),
  ).length;
  const totalUnpaid = displayReservations
    .filter((r) => isPaymentPending(r))
    .reduce((s, r) => s + (r.amount || 0), 0);

  return (
    <div>
      {/* ── 상단 요약 + 액션 ── */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div className="flex items-center gap-3 flex-wrap">
          <span className="text-sm text-ink-muted">
            전체 {displayReservations.length}건
          </span>
          {unpaidCount > 0 && (
            <span className="text-sm font-medium text-amber-600">
              미결제 {unpaidCount}건
            </span>
          )}
          {payableIds.length > 0 && (
            <label className="flex items-center gap-1.5 text-xs text-ink-soft cursor-pointer select-none">
              <input
                type="checkbox"
                checked={
                  selectedIds.size === payableIds.length &&
                  payableIds.length > 0
                }
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
            <strong className="text-amber-600 text-sm">
              {totalUnpaid.toLocaleString()}원
            </strong>
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
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {displayReservations.map((r) => {
          const company = companyMap[r.cmno];
          const canPay = isPaymentPending(r);
          const payBadge = getPayBadge(r);
          const catName = categoryLabel[company?.category] || "업체";
          const mainImage = getReservationImage(company, r.optionName);

          return (
            <article
              key={r.reservationId}
              className={`relative overflow-hidden rounded-2xl border bg-white transition ${
                selectedIds.has(r.reservationId)
                  ? "border-brand shadow-sm"
                  : "border-line hover:border-brand hover:shadow-md"
              }`}
            >
              <div
                className="relative cursor-pointer"
                onClick={() => company && navigate(`/companies/read/${r.cmno}`)}
              >
                {canPay && (
                  <input
                    type="checkbox"
                    checked={selectedIds.has(r.reservationId)}
                    onChange={(e) => {
                      e.stopPropagation();
                      toggleSelect(r.reservationId);
                    }}
                    onClick={(e) => e.stopPropagation()}
                    className="absolute left-3 top-3 z-10 h-5 w-5 accent-brand"
                  />
                )}

                {mainImage ? (
                  <img
                    src={getCompanyImageUrl(mainImage)}
                    alt={company?.name || `업체 #${r.cmno}`}
                    className="h-44 w-full object-cover"
                  />
                ) : (
                  <div className="flex h-44 items-center justify-center bg-blush-50 text-sm text-ink-faint">
                    대표 이미지 없음
                  </div>
                )}
              </div>

              <div
                className="cursor-pointer p-4"
                onClick={() => company && navigate(`/companies/read/${r.cmno}`)}
              >
                <div className="mb-2 flex items-center justify-between gap-2">
                  <span className="rounded-full bg-blush-100 px-2.5 py-1 text-xs text-brand-deep">
                    {catName}
                  </span>
                  <span
                    className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-medium ${payBadge.style}`}
                  >
                    {payBadge.label}
                  </span>
                </div>

                <p className="truncate text-base font-semibold text-ink">
                  {company?.name || `업체 #${r.cmno}`}
                </p>

                {r.optionName && (
                  <p className="mt-1 truncate text-xs font-medium text-brand">
                    예약 옵션: {r.optionName}
                  </p>
                )}

                <div className="mt-3 flex flex-col gap-1 text-xs text-ink-muted">
                  <div className="flex justify-between gap-2">
                    <span className="text-ink-soft">예약 날짜</span>
                    <span className="font-medium text-ink">
                      {r.weddingDate || "미정"}
                    </span>
                  </div>
                  {r.amount > 0 && r.paymentDeadline && (
                    <div className="flex justify-between gap-2">
                      <span className="text-ink-soft">결제 마감</span>
                      <span
                        className={
                          isPaymentDeadlinePassed(r)
                            ? "font-medium text-red-600"
                            : "text-ink"
                        }
                      >
                        {r.paymentDeadline}
                        {isPaymentDeadlinePassed(r) ? " (지남)" : "까지"}
                      </span>
                    </div>
                  )}
                  {r.amount > 0 && (
                    <div className="flex justify-between gap-2">
                      <span className="text-ink-soft">금액</span>
                      <span className="text-sm font-semibold text-ink">
                        {Number(r.amount).toLocaleString()}원
                      </span>
                    </div>
                  )}
                </div>
              </div>

              <div
                className="flex items-center justify-between rounded-b-2xl border-t border-line bg-surface px-4 py-2.5"
                onClick={(e) => e.stopPropagation()}
              >
                <span
                  className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[getReservationPhase(r)] || STATUS_STYLE["예약대기"]}`}
                >
                  {getReservationPhase(r)}
                </span>
                <div className="flex gap-1.5">
                  {/* 재원 추가 - 결제대기인데 마감일이 지난 경우: 결제 버튼 대신 안내만 표시 */}
                  {getReservationPhase(r) === "결제대기" &&
                    isPaymentDeadlinePassed(r) && (
                      <span className="text-[11px] text-red-600 font-medium">
                        결제 기한이 지났어요. 업체에 문의해주세요.
                      </span>
                    )}
                  {/* 재원 추가 끝 */}
                  {getReservationPhase(r) === "결제대기" &&
                    !isPaymentDeadlinePassed(r) && (
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
                  {(canEditReservation(r) ||
                    getReservationPhase(r) === "결제대기") && (
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
            </article>
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
