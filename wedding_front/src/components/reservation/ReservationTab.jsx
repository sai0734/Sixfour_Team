import { useEffect, useState } from "react";
import {
  getListByMember,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/reservationApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import ReservationFormModal from "./ReservationFormModal";

const STATUS_STYLE = {
  대기: "bg-surface text-ink-muted",
  확정: "bg-green-50 text-green-700",
  취소: "bg-red-50 text-red-600",
};

const ReservationTab = () => {
  const { loginState } = useCustomLogin();

  const [reservations, setReservations] = useState([]);
  const [refresh, setRefresh] = useState(false);

  const [modalMode, setModalMode] = useState(null); // null | "add" | "edit"
  const [editTarget, setEditTarget] = useState(null);

  useEffect(() => {
    if (!loginState.email) return;

    getListByMember(loginState.email).then((data) => setReservations(data));
  }, [loginState.email, refresh]);

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

  const handleDelete = (reservationId) => {
    if (!window.confirm("예약을 삭제하시겠습니까?")) return;

    deleteOne(reservationId)
      .then(() => setRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <p className="text-sm text-ink-muted">예약 {reservations.length}건</p>
        <button
          type="button"
          onClick={openAdd}
          className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
        >
          + 예약 등록
        </button>
      </div>

      {reservations.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          등록된 예약이 없습니다.
        </div>
      )}

      <div className="flex flex-col gap-3">
        {reservations.map((r) => (
          <div
            key={r.reservationId}
            className="bg-white rounded-2xl border border-line px-5 py-4 flex items-center gap-4"
          >
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-sm font-medium text-ink">
                  업체 번호 #{r.cmno}
                </span>
                <span
                  className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[r.status] || STATUS_STYLE["대기"]}`}
                >
                  {r.status}
                </span>
              </div>
              <p className="text-xs text-ink-faint">
                {r.weddingDate || "날짜 미정"}
                {r.memo && ` · ${r.memo}`}
              </p>
            </div>

            <button
              type="button"
              onClick={() => openEdit(r)}
              className="h-8 px-4 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream shrink-0"
            >
              수정
            </button>
            <button
              type="button"
              onClick={() => handleDelete(r.reservationId)}
              className="h-8 px-4 rounded-full border border-line-soft text-xs text-red-600 hover:bg-cream shrink-0"
            >
              삭제
            </button>
          </div>
        ))}
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
