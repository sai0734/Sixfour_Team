// 승진 코드 추가
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getListByMember,
  postAdd,
  putOne,
  deleteOne,
  prepareBulkPayment,
  cancelBulkPayment,
} from "../../api/reservationApi";
import { getOne as getCompanyOne } from "../../api/companyApi";
import { TOSS_CLIENT_KEY } from "../../api/tossConfig";
import useCustomLogin from "../../hooks/useCustomLogin";
import ReservationFormModal from "./ReservationFormModal";

const STATUS_STYLE = {
  대기: "bg-surface text-ink-muted",
  확정: "bg-green-50 text-green-700",
  취소: "bg-red-50 text-red-600",
};

const PAY_STATUS_STYLE = {
  NONE: "bg-amber-50 text-amber-700",
  PAID: "bg-green-50 text-green-700",
  CANCELLED: "bg-red-50 text-red-600",
};
const PAY_STATUS_LABEL = { NONE: "미결제", PAID: "결제완료", CANCELLED: "취소" };

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

// 업체별로 예약을 그룹화 { cmno: { company, reservations[] } }
const groupByCompany = (reservations, companyMap) => {
  const groups = {};
  reservations.forEach((r) => {
    if (!groups[r.cmno]) {
      groups[r.cmno] = { company: companyMap[r.cmno] || null, reservations: [] };
    }
    groups[r.cmno].reservations.push(r);
  });
  return groups;
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

  // 묶음 결제 선택
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [paying, setPaying] = useState(false);

  const tossRef = useRef(null);

  // 예약 목록 로드
  useEffect(() => {
    if (!loginState.email) return;
    setLoading(true);
    getListByMember(loginState.email)
      .then(async (data) => {
        setReservations(data);

        // 업체 정보 일괄 조회 (중복 제거)
        const uniqueCmnos = [...new Set(data.map((r) => r.cmno).filter(Boolean))];
        const results = await Promise.allSettled(
          uniqueCmnos.map((cmno) => getCompanyOne(cmno)),
        );
        const map = {};
        results.forEach((res, i) => {
          if (res.status === "fulfilled") {
            map[uniqueCmnos[i]] = res.value;
          }
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
        if (window.TossPayments) {
          tossRef.current = window.TossPayments(TOSS_CLIENT_KEY);
        }
      })
      .catch((e) => console.error("Toss 스크립트 로드 실패:", e));
  }, []);

  // ── 모달 ──
  const openAdd = () => {
    setEditTarget(null);
    setModalMode("add");
  };
  const openEdit = (r) => {
    setEditTarget(r);
    setModalMode("edit");
  };
  const closeModal = () => {
    setModalMode(null);
    setEditTarget(null);
  };

  const handleSubmit = (formValues) => {
    if (modalMode === "add") {
      postAdd({ ...formValues, memberEmail: loginState.email })
        .then(() => { closeModal(); setRefresh((r) => !r); })
        .catch((e) => console.error(e));
    } else {
      putOne({ ...editTarget, ...formValues })
        .then(() => { closeModal(); setRefresh((r) => !r); })
        .catch((e) => console.error(e));
    }
  };

  const handleDelete = (reservationId) => {
    if (!window.confirm("예약을 삭제하시겠습니까?")) return;
    deleteOne(reservationId)
      .then(() => setRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  // ── 체크박스 ──
  // 결제 가능한 예약: payStatus !== 'PAID', amount > 0
  const payableIds = reservations
    .filter((r) => r.payStatus !== "PAID" && r.amount > 0)
    .map((r) => r.reservationId);

  const toggleSelect = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };
  const toggleSelectAll = () => {
    if (selectedIds.size === payableIds.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(payableIds));
    }
  };

  // ── 묶음 결제 ──
  const handleBulkPay = async () => {
    if (selectedIds.size === 0) {
      alert("결제할 예약을 선택해주세요.");
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
      const prepared = await prepareBulkPayment(ids);
      const { orderNumber, totalAmount } = prepared;

      const selectedList = reservations.filter((r) => ids.includes(r.reservationId));
      const orderName =
        selectedList.length === 1
          ? `업체 #${selectedList[0].cmno} 예약`
          : `업체 예약 묶음 결제 (${selectedList.length}건)`;

      const idsParam = ids.join(",");
      const successUrl = `${window.location.origin}/companies/reserve/bulk/success?reservationIds=${idsParam}`;
      const failUrl = `${window.location.origin}/companies/reserve/bulk/fail?reservationIds=${idsParam}`;

      await tossRef.current.requestPayment("카드", {
        amount: totalAmount,
        orderId: orderNumber,
        orderName,
        customerName: loginState.nickname || loginState.email,
        successUrl,
        failUrl,
      });
    } catch (err) {
      if (err?.code === "USER_CANCEL") {
        const ids = [...selectedIds];
        await cancelBulkPayment(ids).catch(() => {});
      } else {
        console.error("묶음 결제 오류:", err);
        alert("결제 처리 중 오류가 발생했습니다.");
      }
    } finally {
      setPaying(false);
    }
  };

  const groups = groupByCompany(reservations, companyMap);
  const unpaidCount = reservations.filter((r) => r.payStatus !== "PAID" && r.amount > 0).length;

  return (
    <div>
      {/* ── 헤더 ── */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div className="flex items-center gap-3">
          <p className="text-sm text-ink-muted">
            예약 {reservations.length}건
            {unpaidCount > 0 && (
              <span className="ml-2 text-amber-600 font-medium">미결제 {unpaidCount}건</span>
            )}
          </p>
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
          {selectedIds.size > 0 && (
            <button
              type="button"
              onClick={handleBulkPay}
              disabled={paying}
              className="h-10 px-5 rounded-full bg-rose-500 text-white text-sm font-medium hover:bg-rose-600 transition disabled:opacity-60"
            >
              {paying ? "결제 중..." : `선택 결제 (${selectedIds.size}건)`}
            </button>
          )}
          <button
            type="button"
            onClick={openAdd}
            className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
          >
            + 예약 등록
          </button>
        </div>
      </div>

      {/* ── 예약 없음 ── */}
      {!loading && reservations.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          등록된 예약이 없습니다.
        </div>
      )}

      {/* ── 업체별 그룹 ── */}
      <div className="flex flex-col gap-6">
        {Object.entries(groups).map(([cmno, group]) => {
          const company = group.company;
          return (
            <div key={cmno} className="bg-white rounded-2xl border border-line overflow-hidden">
              {/* 업체 헤더 */}
              <div
                className="flex items-center gap-3 px-5 py-3 bg-blush-50 border-b border-line cursor-pointer hover:bg-blush-100 transition"
                onClick={() => navigate(`/companies/read/${cmno}`)}
              >
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-ink truncate">
                    {company?.name || `업체 #${cmno}`}
                  </p>
                  {company?.address && (
                    <p className="text-xs text-ink-faint truncate mt-0.5">{company.address}</p>
                  )}
                </div>
                <span className="text-xs text-brand shrink-0">상세 보기 →</span>
              </div>

              {/* 해당 업체의 예약 목록 */}
              <div className="divide-y divide-line">
                {group.reservations.map((r) => {
                  const canPay = r.payStatus !== "PAID" && r.amount > 0;
                  return (
                    <div
                      key={r.reservationId}
                      className="flex items-center gap-3 px-5 py-4"
                    >
                      {/* 체크박스 */}
                      <input
                        type="checkbox"
                        checked={selectedIds.has(r.reservationId)}
                        onChange={() => canPay && toggleSelect(r.reservationId)}
                        disabled={!canPay}
                        className="w-4 h-4 accent-brand shrink-0 disabled:opacity-30"
                      />

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1 flex-wrap">
                          <span className="text-sm font-medium text-ink">
                            {r.optionName || "옵션 미정"}
                          </span>
                          <span
                            className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[r.status] || STATUS_STYLE["대기"]}`}
                          >
                            {r.status}
                          </span>
                          <span
                            className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${PAY_STATUS_STYLE[r.payStatus] || PAY_STATUS_STYLE["NONE"]}`}
                          >
                            {PAY_STATUS_LABEL[r.payStatus] || "미결제"}
                          </span>
                        </div>
                        <p className="text-xs text-ink-faint">
                          {r.weddingDate || "날짜 미정"}
                          {r.amount > 0 && (
                            <span className="ml-2 font-medium text-ink">
                              {Number(r.amount).toLocaleString()}원
                            </span>
                          )}
                          {r.memo && ` · ${r.memo}`}
                        </p>
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0">
                        <button
                          type="button"
                          onClick={() => openEdit(r)}
                          className="h-8 px-3 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(r.reservationId)}
                          className="h-8 px-3 rounded-full border border-line-soft text-xs text-red-500 hover:bg-cream"
                        >
                          삭제
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* 업체별 소계 (결제 완료/미결제) */}
              {group.reservations.some((r) => r.amount > 0) && (
                <div className="px-5 py-3 bg-surface flex justify-end gap-4 text-xs text-ink-muted border-t border-line">
                  <span>
                    결제완료:{" "}
                    <strong className="text-green-700">
                      {group.reservations
                        .filter((r) => r.payStatus === "PAID")
                        .reduce((s, r) => s + (r.amount || 0), 0)
                        .toLocaleString()}
                      원
                    </strong>
                  </span>
                  <span>
                    미결제:{" "}
                    <strong className="text-amber-600">
                      {group.reservations
                        .filter((r) => r.payStatus !== "PAID")
                        .reduce((s, r) => s + (r.amount || 0), 0)
                        .toLocaleString()}
                      원
                    </strong>
                  </span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {modalMode && (
        <ReservationFormModal
          mode={modalMode}
          editTarget={editTarget}
          onSubmit={handleSubmit}
          onClose={closeModal}
        />
      )}
    </div>
  );
};

export default ReservationTab;
// 승진 코드 추가 끝
