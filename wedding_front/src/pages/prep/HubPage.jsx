import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import PrepLayout from "../../layouts/PrepLayout";
import TapeLabel from "../../components/common/TapeLabel";
import DdayEventFormModal from "../../components/ddayevent/DdayEventFormModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getListByMember as getChecklist } from "../../api/checklistApi";
import { getListByMember as getBudget } from "../../api/budgetApi";
import { getByMember as getWeddingPlan } from "../../api/weddingplanApi";
import { getListByMember as getReservations } from "../../api/reservationApi";
import {
  getListByMember as getDdayEvents,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/ddayEventApi";

// 예전엔 준비관리 허브 / D-day 관리가 별도 탭이었는데, 마이페이지(플랜의 예식일, 예약 현황의
// 결제 마감일)와 계속 겹쳐서 "이 탭이 따로 필요한가"라는 문제가 있었음 - 그래서 D-day 관리
// 탭을 없애고, 그 안에서만 가능했던 진짜 고유 기능(체크리스트 마감일 + 예식일 + 직접 추가한
// 일정을 한 타임라인으로 합쳐 보여주기, 커스텀 일정 CRUD)을 이 허브 페이지 안으로 옮겼다.
// 단순 D-day 카운트다운은 메인페이지/마이페이지 플랜 카드에도 이미 있어서 중복이었지만,
// 타임라인 자체는 다른 곳에 없는 기능이라 그대로 유지.
const TYPE_STYLE = {
  예식일: "bg-brand text-white",
  체크리스트: "bg-blue-50 text-blue-700",
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

// 0~100 사이로 클램프한 진행률 바 - 체크리스트 완료율/예산 소진율 둘 다 재사용
const ProgressBar = ({ percent, tone = "brand" }) => {
  const clamped = Math.max(0, Math.min(100, percent));
  const barColor = tone === "danger" ? "bg-red-500" : "bg-brand";
  return (
    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-blush-100">
      <div
        className={`h-full rounded-full ${barColor} transition-all`}
        style={{ width: `${clamped}%` }}
      />
    </div>
  );
};

const HubPage = () => {
  const { loginState } = useCustomLogin();
  const navigate = useNavigate();

  const [checklist, setChecklist] = useState([]);
  const [budgetList, setBudgetList] = useState([]);
  const [plan, setPlan] = useState(null);
  const [reservations, setReservations] = useState([]);
  const [ddayEvents, setDdayEvents] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const [refresh, setRefresh] = useState(false);

  const [modalMode, setModalMode] = useState(null);
  const [editTarget, setEditTarget] = useState(null);

  useEffect(() => {
    if (!loginState.email) return;

    Promise.all([
      getChecklist(loginState.email),
      getBudget(loginState.email),
      getWeddingPlan(loginState.email).catch(() => null),
      getReservations(loginState.email).catch(() => []),
      getDdayEvents(loginState.email).catch(() => []),
    ]).then(([checklistData, budgetData, planData, reservationData, ddayData]) => {
      setChecklist(checklistData);
      setBudgetList(budgetData);
      setPlan(planData);
      setReservations(reservationData);
      setDdayEvents(ddayData);
      setLoaded(true);
    });
  }, [loginState.email, refresh]);

  // D-day 타임라인 - 예식일 + 체크리스트 마감일(미완료) + 직접 추가한 일정을 한 목록으로 합침
  const timeline = useMemo(() => {
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
          reservationId: c.reservationId,
        });
      });

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
    return combined;
  }, [plan, checklist, ddayEvents]);

  const upcoming = useMemo(
    () =>
      timeline.filter(
        (i) => new Date(i.date) >= new Date(new Date().toDateString()),
      ),
    [timeline],
  );
  const past = useMemo(
    () =>
      timeline.filter(
        (i) => new Date(i.date) < new Date(new Date().toDateString()),
      ),
    [timeline],
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

  if (!loginState.email) {
    return (
      <PrepLayout
        eyebrow="03 — PREP HUB"
        title="준비 관리"
        subtitle="한눈에 보는 우리 결혼 준비 현황"
      >
        <div className="p-10 text-center text-ink-faint">
          로그인 후 이용해주세요.
        </div>
      </PrepLayout>
    );
  }

  if (!loaded) {
    return (
      <PrepLayout
        eyebrow="03 — PREP HUB"
        title="준비 관리"
        subtitle="한눈에 보는 우리 결혼 준비 현황"
      >
        <div className="p-10 text-center text-ink-faint">불러오는 중...</div>
      </PrepLayout>
    );
  }

  const doneCount = checklist.filter((i) => i.done).length;
  const total = checklist.length;
  const percent = total === 0 ? 0 : Math.round((doneCount / total) * 100);

  const itemizedBudget = budgetList.reduce(
    (s, i) => s + (i.budgetAmount || 0),
    0,
  );
  const totalBudget = plan?.totalBudget || itemizedBudget;
  const totalActual = budgetList.reduce((s, i) => s + (i.actualAmount || 0), 0);
  const remaining = totalBudget - totalActual;
  const spentPercent = totalBudget > 0 ? Math.round((totalActual / totalBudget) * 100) : 0;

  const dday = plan?.weddingDate
    ? Math.ceil(
        (new Date(plan.weddingDate) - new Date(new Date().toDateString())) /
          (1000 * 60 * 60 * 24),
      )
    : null;

  const pendingPayments = reservations.filter(
    (r) =>
      r.amount > 0 &&
      r.status === "결제대기" &&
      r.payStatus !== "PAID" &&
      r.paymentDeadline,
  );
  const overduePayments = pendingPayments.filter(
    (r) => new Date(r.paymentDeadline) < new Date(new Date().toDateString()),
  );
  const nearestDeadline = pendingPayments
    .map((r) => r.paymentDeadline)
    .sort()[0];

  const renderTimelineItem = (item, idx) => (
    <div
      key={idx}
      className="flex items-center gap-4 rounded-2xl border border-line bg-white px-5 py-4"
    >
      <div className="w-16 shrink-0 text-center">
        <p className="text-sm font-medium text-brand">{calcDday(item.date)}</p>
        <p className="text-[11px] text-ink-faint">{item.date}</p>
      </div>

      <div className="min-w-0 flex-1 border-l border-line pl-4">
        <div className="mb-1 flex items-center gap-2">
          <span
            className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${TYPE_STYLE[item.type]}`}
          >
            {item.type}
          </span>
          {item.reservationId ? (
            <button
              type="button"
              onClick={() => navigate("/mypage?tab=reservation")}
              title="마이페이지 예약 현황에서 보기"
              className="truncate text-sm text-ink hover:underline"
            >
              {item.title}
            </button>
          ) : (
            <span className="truncate text-sm text-ink">{item.title}</span>
          )}
        </div>
        {item.memo && <p className="text-xs text-ink-faint">{item.memo}</p>}
      </div>

      {item.editable && (
        <div className="flex shrink-0 gap-2">
          <button
            type="button"
            onClick={() => openEdit(item)}
            className="h-8 rounded-full border border-line-soft px-4 text-xs text-ink-soft hover:bg-cream"
          >
            수정
          </button>
          <button
            type="button"
            onClick={() => handleDelete(item.raw.ddayId)}
            className="h-8 rounded-full border border-line-soft px-4 text-xs text-red-600 hover:bg-cream"
          >
            삭제
          </button>
        </div>
      )}
    </div>
  );

  return (
    <PrepLayout
      eyebrow="03 — PREP HUB"
      title="준비 관리"
      subtitle="한눈에 보는 우리 결혼 준비 현황"
    >
      <TapeLabel className="mb-4">오늘의 준비 현황</TapeLabel>

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-line bg-white p-6">
          <p className="mb-2 text-xs text-ink-muted">예식일</p>
          {plan?.weddingDate ? (
            <>
              <p className="mb-1 text-2xl font-medium text-brand">
                {dday === 0 ? "D-day" : dday > 0 ? `D-${dday}` : `D+${-dday}`}
              </p>
              <p className="text-xs text-ink-faint">{plan.weddingDate}</p>
            </>
          ) : (
            <p className="text-sm text-ink-faint">
              마이페이지 플랜에서 예식일을 등록해보세요
            </p>
          )}
        </div>

        <Link
          to="/checklist/list"
          className="rounded-2xl border border-line bg-white p-6 transition-colors hover:border-brand"
        >
          <p className="mb-2 text-xs text-ink-muted">체크리스트</p>
          <p className="mb-1 text-2xl font-medium text-ink">{percent}%</p>
          <p className="text-xs text-ink-faint">
            완료 {doneCount} / 전체 {total}
          </p>
          <ProgressBar percent={percent} />
        </Link>

        <Link
          to="/budget/list"
          className="rounded-2xl border border-line bg-white p-6 transition-colors hover:border-brand"
        >
          <p className="mb-2 text-xs text-ink-muted">예산</p>
          <p
            className={`mb-1 text-2xl font-medium ${remaining < 0 ? "text-red-600" : "text-ink"}`}
          >
            {remaining < 0
              ? `${Math.abs(remaining).toLocaleString()}원 초과`
              : `${remaining.toLocaleString()}원`}
          </p>
          <p className="text-xs text-ink-faint">
            지출 {totalActual.toLocaleString()} / {totalBudget.toLocaleString()}
            원
          </p>
          {!plan?.totalBudget && (
            <p className="mt-1 text-[10px] text-ink-faint">
              마이페이지에서 총 예산을 설정하면 여기에 반영돼요
            </p>
          )}
          <ProgressBar percent={spentPercent} tone={remaining < 0 ? "danger" : "brand"} />
        </Link>
      </div>

      {/* 타임라인(넓게)과 더 챙겨볼 것들(좁게)을 좌우로 나란히 배치 - 예전엔 전부 세로로
          쌓여있어서 스크롤이 길었음 */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_320px] lg:items-start">
        <div>
          <div className="mb-4 flex items-center justify-between">
            <TapeLabel>다가오는 일정</TapeLabel>
            <button
              type="button"
              onClick={openAdd}
              className="h-9 shrink-0 rounded-full bg-brand px-4 text-xs font-medium text-white hover:bg-brand-dark"
            >
              + 일정 추가
            </button>
          </div>
          <p className="mb-4 rounded-xl bg-blush-50 px-4 py-3 text-xs text-brand-deep">
            체크리스트 마감일·결제 마감일·예식일을 자동으로 모아 보여줘요. 직접
            일정도 추가할 수 있어요.
          </p>

          {upcoming.length === 0 ? (
            <div className="rounded-2xl border border-line bg-white py-10 text-center text-ink-faint">
              다가오는 일정이 없습니다.
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {upcoming.map((item, idx) => renderTimelineItem(item, idx))}
            </div>
          )}

          {past.length > 0 && (
            <details className="mt-6">
              <summary className="cursor-pointer text-sm font-medium text-ink-faint">
                지난 일정 {past.length}건
              </summary>
              <div className="mt-3 flex flex-col gap-3 opacity-60">
                {past.map((item, idx) => renderTimelineItem(item, `past-${idx}`))}
              </div>
            </details>
          )}
        </div>

        <div className="flex flex-col gap-4">
          <TapeLabel rotate={3}>더 챙겨볼 것들</TapeLabel>

          {pendingPayments.length > 0 && (
            <Link
              to="/mypage?tab=reservation"
              className="rounded-2xl border border-line bg-white p-6 transition-colors hover:border-brand"
            >
              <p className="mb-2 text-xs text-ink-muted">결제 예정</p>
              <p
                className={`mb-1 text-2xl font-medium ${
                  overduePayments.length > 0 ? "text-red-600" : "text-ink"
                }`}
              >
                {pendingPayments.length}건
              </p>
              <p className="text-xs text-ink-faint">
                {overduePayments.length > 0
                  ? `기한 지남 ${overduePayments.length}건 포함`
                  : nearestDeadline
                    ? `가장 가까운 마감 ${nearestDeadline}`
                    : ""}
              </p>
            </Link>
          )}

          <Link
            to="/prep/ai-dress"
            className="rounded-2xl border border-line bg-white p-6 transition-colors hover:border-brand"
          >
            <p className="mb-2 text-xs text-ink-muted">AI 드레스</p>
            <p className="mb-1 text-sm font-medium text-ink">가상 피팅 체험</p>
            <p className="text-xs text-ink-faint">
              내 사진으로 드레스를 미리 입어보세요
            </p>
          </Link>
        </div>
      </div>

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

export default HubPage;
