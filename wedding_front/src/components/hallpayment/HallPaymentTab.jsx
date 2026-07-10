import { useEffect, useState } from "react";
import {
  getListByMember,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/hallPaymentApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import HallPaymentFormModal from "./HallPaymentFormModal";

const STATUS_STYLE = {
  대기: "bg-surface text-ink-muted",
  완료: "bg-green-50 text-green-700",
  취소: "bg-red-50 text-red-600",
};

const isOverdue = (dueDate, status) => {
  if (!dueDate || status !== "대기") return false;
  return new Date(dueDate) < new Date(new Date().toDateString());
};

const HallPaymentTab = () => {
  const { loginState } = useCustomLogin();

  const [payments, setPayments] = useState([]);
  const [refresh, setRefresh] = useState(false);

  const [modalMode, setModalMode] = useState(null);
  const [editTarget, setEditTarget] = useState(null);

  useEffect(() => {
    if (!loginState.email) return;

    getListByMember(loginState.email).then((data) => setPayments(data));
  }, [loginState.email, refresh]);

  const openAdd = () => {
    setEditTarget(null);
    setModalMode("add");
  };

  const openEdit = (p) => {
    setEditTarget(p);
    setModalMode("edit");
  };

  const closeModal = () => {
    setModalMode(null);
    setEditTarget(null);
  };

  const handleSubmit = (formValues) => {
    if (modalMode === "add") {
      postAdd({ ...formValues, memberEmail: loginState.email })
        .then(() => {
          closeModal();
          setRefresh((r) => !r);
        })
        .catch((e) => console.error(e));
    } else {
      putOne({ ...editTarget, ...formValues })
        .then(() => {
          closeModal();
          setRefresh((r) => !r);
        })
        .catch((e) => console.error(e));
    }
  };

  const handleDelete = (paymentId) => {
    if (!window.confirm("납부 항목을 삭제하시겠습니까?")) return;

    deleteOne(paymentId)
      .then(() => setRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  if (!loginState.email) {
    return (
      <div className="p-10 text-center text-ink-faint">
        로그인 후 이용해주세요.
      </div>
    );
  }

  const totalAmount = payments.reduce((s, p) => s + (p.amount || 0), 0);
  const paidAmount = payments
    .filter((p) => p.status === "완료")
    .reduce((s, p) => s + (p.amount || 0), 0);

  return (
    <div>
      <div className="grid grid-cols-1 gap-4 mb-6 sm:grid-cols-2">
        <div className="bg-white rounded-2xl border border-line p-5">
          <p className="text-xs text-ink-muted mb-1">총 납부 예정액</p>
          <p className="text-xl font-medium text-ink">
            {totalAmount.toLocaleString()}원
          </p>
        </div>
        <div className="bg-white rounded-2xl border border-line p-5">
          <p className="text-xs text-ink-muted mb-1">완료된 납부액</p>
          <p className="text-xl font-medium text-brand">
            {paidAmount.toLocaleString()}원
          </p>
        </div>
      </div>

      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-ink-muted">
          웨딩홀 납부 {payments.length}건
        </p>
        <button
          type="button"
          onClick={openAdd}
          className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
        >
          + 납부 등록
        </button>
      </div>

      {payments.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          등록된 납부 내역이 없습니다.
        </div>
      )}

      <div className="flex flex-col gap-3">
        {payments.map((p) => {
          const overdue = isOverdue(p.dueDate, p.status);

          return (
            <div
              key={p.paymentId}
              className={`bg-white rounded-2xl border px-5 py-4 flex items-center gap-4 ${
                overdue ? "border-red-300" : "border-line"
              }`}
            >
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-medium text-ink">
                    업체 번호 #{p.cmno} · {p.paymentType}
                  </span>
                  <span
                    className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[p.status] || STATUS_STYLE["대기"]}`}
                  >
                    {p.status}
                  </span>
                  {overdue && (
                    <span className="text-[11px] bg-red-50 text-red-600 px-2 py-0.5 rounded-full font-medium">
                      기한 초과
                    </span>
                  )}
                </div>
                <p className="text-xs text-ink-faint">
                  {p.amount?.toLocaleString()}원 · 기한 {p.dueDate || "미정"}
                </p>
              </div>

              <button
                type="button"
                onClick={() => openEdit(p)}
                className="h-8 px-4 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream shrink-0"
              >
                수정
              </button>
              <button
                type="button"
                onClick={() => handleDelete(p.paymentId)}
                className="h-8 px-4 rounded-full border border-line-soft text-xs text-red-600 hover:bg-cream shrink-0"
              >
                삭제
              </button>
            </div>
          );
        })}
      </div>

      {modalMode && (
        <HallPaymentFormModal
          mode={modalMode}
          editTarget={editTarget}
          onSubmit={handleSubmit}
          onClose={closeModal}
        />
      )}
    </div>
  );
};

export default HallPaymentTab;
