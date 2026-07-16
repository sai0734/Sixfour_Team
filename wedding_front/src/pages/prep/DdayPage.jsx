import { useEffect, useMemo, useState } from "react";
import PrepLayout from "../../layouts/PrepLayout";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getByMember as getWeddingPlan } from "../../api/weddingplanApi";
import { getListByMember as getChecklist } from "../../api/checklistApi";
import {
  getListByMember as getDdayEvents,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/ddayEventApi";
// 재원 추가 - 준비관리 D-day에 예약 결제 마감일도 같이 모아 보여주기 위해 가져옴
import { getListByMember as getReservations } from "../../api/reservationApi";
import { getOne as getCompanyOne } from "../../api/companyApi";
// 재원 추가 끝
import DdayEventFormModal from "../../components/ddayevent/DdayEventFormModal";

const TYPE_STYLE = {
  예식일: "bg-brand text-white",
  체크리스트: "bg-blue-50 text-blue-700",
  결제: "bg-orange-50 text-orange-700", // 재원 추가
  커스텀: "bg-purple-50 text-purple-700",
};

const calcDday = (dateStr) => {
  const diff = Math.ceil(
    (new Date(dateStr) - new Date(new Date().toDateString())) /
      (1000 * 60 * 60 * 24),
  );
  if (diff === 0) return "D-day";
  return diff > 0 ? `D-${diff}` : `D+${-diff}`;
};

const DdayPage = () => {
  const { loginState } = useCustomLogin();

  const [items, setItems] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const [refresh, setRefresh] = useState(false);

  const [modalMode, setModalMode] = useState(null);
  const [editTarget, setEditTarget] = useState(null);

  useEffect(() => {
    if (!loginState.email) return;

    Promise.all([
      getWeddingPlan(loginState.email).catch(() => null),
      getChecklist(loginState.email).catch(() => []),
      getDdayEvents(loginState.email).catch(() => []),
      getReservations(loginState.email).catch(() => []), // 재원 추가
    ]).then(async ([plan, checklist, ddayEvents, reservations]) => {
      const combined = [];

      if (plan?.weddingDate) {
        combined.push({
          date: plan.weddingDate,
          type: "예식일",
          title: "우리의 결혼식",
          editable: false,
        });
      }

      checklist
        .filter((c) => c.dueDate && !c.done)
        .forEach((c) => {
          combined.push({
            date: c.dueDate,
            type: "체크리스트",
            title: c.title,
            editable: false,
          });
        });

      // 재원 추가 - 아직 결제 안 한 예약(금액 있음)의 결제 마감일을 D-day에 표시
      const pendingPayments = reservations.filter(
        (r) => r.amount > 0 && r.payStatus !== "PAID" && r.paymentDeadline,
      );
      const uniqueCmnos = [...new Set(pendingPayments.map((r) => r.cmno))];
      const companyResults = await Promise.allSettled(
        uniqueCmnos.map((cmno) => getCompanyOne(cmno)),
      );
      const companyMap = {};
      companyResults.forEach((res, i) => {
        if (res.status === "fulfilled") companyMap[uniqueCmnos[i]] = res.value;
      });

      pendingPayments.forEach((r) => {
        const companyName = companyMap[r.cmno]?.name || `업체 #${r.cmno}`;
        combined.push({
          date: r.paymentDeadline,
          type: "결제",
          title: `${companyName} 결제 마감`,
          editable: false,
        });
      });
      // 재원 추가 끝

      ddayEvents.forEach((d) => {
        combined.push({
          date: d.eventDate,
          type: "커스텀",
          title: d.title,
          memo: d.memo,
          editable: true,
          raw: d,
        });
      });

      combined.sort((a, b) => new Date(a.date) - new Date(b.date));

      setItems(combined);
      setLoaded(true);
    });
  }, [loginState.email, refresh]);

  const upcoming = useMemo(
    () =>
      items.filter(
        (i) => new Date(i.date) >= new Date(new Date().toDateString()),
      ),
    [items],
  );
  const past = useMemo(
    () =>
      items.filter(
        (i) => new Date(i.date) < new Date(new Date().toDateString()),
      ),
    [items],
  );

  const openAdd = () => {
    setEditTarget(null);
    setModalMode("add");
  };

  const openEdit = (item) => {
    setEditTarget(item.raw);
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

  const handleDelete = (ddayId) => {
    if (!window.confirm("일정을 삭제하시겠습니까?")) return;

    deleteOne(ddayId)
      .then(() => setRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  const renderItem = (item, idx) => (
    <div
      key={idx}
      className="bg-white rounded-2xl border border-line px-5 py-4 flex items-center gap-4"
    >
      <div className="w-16 text-center shrink-0">
        <p className="text-sm font-medium text-brand">{calcDday(item.date)}</p>
        <p className="text-[11px] text-ink-faint">{item.date}</p>
      </div>

      <div className="flex-1 min-w-0 border-l border-line pl-4">
        <div className="flex items-center gap-2 mb-1">
          <span
            className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${TYPE_STYLE[item.type]}`}
          >
            {item.type}
          </span>
          <span className="text-sm text-ink truncate">{item.title}</span>
        </div>
        {item.memo && <p className="text-xs text-ink-faint">{item.memo}</p>}
      </div>

      {item.editable && (
        <div className="flex gap-2 shrink-0">
          <button
            type="button"
            onClick={() => openEdit(item)}
            className="h-8 px-4 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
          >
            수정
          </button>
          <button
            type="button"
            onClick={() => handleDelete(item.raw.ddayId)}
            className="h-8 px-4 rounded-full border border-line-soft text-xs text-red-600 hover:bg-cream"
          >
            삭제
          </button>
        </div>
      )}
    </div>
  );

  return (
    <PrepLayout
      eyebrow="D-DAY TIMELINE"
      title="D-day 관리"
      subtitle="예식일까지 남은 일정을 한눈에"
    >
      {!loginState.email ? (
        <div className="p-10 text-center text-ink-faint">
          로그인 후 이용해주세요.
        </div>
      ) : !loaded ? (
        <div className="p-10 text-center text-ink-faint">불러오는 중...</div>
      ) : (
        <>
          <div className="flex items-center justify-between mb-6">
            <p className="text-sm text-ink-muted">
              체크리스트 마감일·결제 마감일·예식일을 자동으로 모아 보여줘요.
              직접 일정도 추가할 수 있어요.
            </p>
            <button
              type="button"
              onClick={openAdd}
              className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark shrink-0 ml-4"
            >
              + 일정 추가
            </button>
          </div>

          <p className="text-sm font-medium text-ink mb-3">다가오는 일정</p>
          {upcoming.length === 0 ? (
            <div className="text-center text-ink-faint py-10 bg-white rounded-2xl border border-line mb-8">
              다가오는 일정이 없습니다.
            </div>
          ) : (
            <div className="flex flex-col gap-3 mb-8">
              {upcoming.map((item, idx) => renderItem(item, idx))}
            </div>
          )}

          {past.length > 0 && (
            <>
              <p className="text-sm font-medium text-ink-faint mb-3">
                지난 일정
              </p>
              <div className="flex flex-col gap-3 opacity-60">
                {past.map((item, idx) => renderItem(item, `past-${idx}`))}
              </div>
            </>
          )}
        </>
      )}

      {modalMode && (
        <DdayEventFormModal
          mode={modalMode}
          editTarget={editTarget}
          onSubmit={handleSubmit}
          onClose={closeModal}
        />
      )}
    </PrepLayout>
  );
};

export default DdayPage;
