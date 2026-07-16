import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import PrepLayout from "../../layouts/PrepLayout";
import TapeLabel from "../../components/common/TapeLabel";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getListByMember as getChecklist } from "../../api/checklistApi";
import { getListByMember as getBudget } from "../../api/budgetApi";
import { getByMember as getWeddingPlan } from "../../api/weddingplanApi";
// 재원 추가 - "더 챙겨볼 것들"에 결제 예정(예약 기반) 카드를 보여주기 위해 가져옴
import { getListByMember as getReservations } from "../../api/reservationApi";
// 재원 추가 끝

const HubPage = () => {
  const { loginState } = useCustomLogin();

  const [checklist, setChecklist] = useState([]);
  const [budgetList, setBudgetList] = useState([]);
  const [plan, setPlan] = useState(null);
  const [reservations, setReservations] = useState([]); // 재원 추가
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    if (!loginState.email) return;

    Promise.all([
      getChecklist(loginState.email),
      getBudget(loginState.email),
      getWeddingPlan(loginState.email).catch(() => null),
      getReservations(loginState.email).catch(() => []), // 재원 추가
    ]).then(([checklistData, budgetData, planData, reservationData]) => {
      setChecklist(checklistData);
      setBudgetList(budgetData);
      setPlan(planData);
      setReservations(reservationData); // 재원 추가
      setLoaded(true);
    });
  }, [loginState.email]);

  if (!loginState.email) {
    return (
      <PrepLayout
        eyebrow="PREP HUB"
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
        eyebrow="PREP HUB"
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

  // 마이페이지 웨딩플랜에서 설정한 "총 예산"을 기준으로 삼는다.
  // (전에는 이 값을 아예 안 읽고, 예산관리 항목들의 budgetAmount 합계만 썼기 때문에
  //  마이페이지에서 총 예산을 설정해도 허브에는 반영되지 않았음)
  // 아직 총 예산을 설정 안 했으면(0 또는 미설정) 예산관리 항목 합계로 대체 표시.
  const itemizedBudget = budgetList.reduce(
    (s, i) => s + (i.budgetAmount || 0),
    0,
  );
  const totalBudget = plan?.totalBudget || itemizedBudget;
  const totalActual = budgetList.reduce((s, i) => s + (i.actualAmount || 0), 0);
  const remaining = totalBudget - totalActual;

  const dday = plan?.weddingDate
    ? Math.ceil(
        (new Date(plan.weddingDate) - new Date(new Date().toDateString())) /
          (1000 * 60 * 60 * 24),
      )
    : null;

  // 재원 추가 - "더 챙겨볼 것들"용 결제 예정 요약
  const pendingPayments = reservations.filter(
    (r) => r.amount > 0 && r.payStatus !== "PAID" && r.paymentDeadline,
  );
  const overduePayments = pendingPayments.filter(
    (r) => new Date(r.paymentDeadline) < new Date(new Date().toDateString()),
  );
  const nearestDeadline = pendingPayments
    .map((r) => r.paymentDeadline)
    .sort()[0];
  // 재원 추가 끝

  return (
    <PrepLayout
      eyebrow="PREP HUB"
      title="준비 관리"
      subtitle="한눈에 보는 우리 결혼 준비 현황"
    >
      <TapeLabel className="mb-4">오늘의 준비 현황</TapeLabel>

      <div className="grid grid-cols-1 gap-4 mb-4 sm:grid-cols-3">
        <Link
          to="/prep/dday"
          className="bg-white rounded-2xl border border-line p-6 hover:border-brand transition-colors"
        >
          <p className="text-xs text-ink-muted mb-2">예식일</p>
          {plan?.weddingDate ? (
            <>
              <p className="text-2xl font-medium text-brand mb-1">
                {dday === 0 ? "D-day" : dday > 0 ? `D-${dday}` : `D+${-dday}`}
              </p>
              <p className="text-xs text-ink-faint">{plan.weddingDate}</p>
            </>
          ) : (
            <p className="text-sm text-ink-faint">
              웨딩플랜에서 예식일을 등록해보세요
            </p>
          )}
        </Link>

        <Link
          to="/checklist/list"
          className="bg-white rounded-2xl border border-line p-6 hover:border-brand transition-colors"
        >
          <p className="text-xs text-ink-muted mb-2">체크리스트</p>
          <p className="text-2xl font-medium text-ink mb-1">{percent}%</p>
          <p className="text-xs text-ink-faint">
            완료 {doneCount} / 전체 {total}
          </p>
        </Link>

        <Link
          to="/budget/list"
          className="bg-white rounded-2xl border border-line p-6 hover:border-brand transition-colors"
        >
          <p className="text-xs text-ink-muted mb-2">예산</p>
          <p
            className={`text-2xl font-medium mb-1 ${remaining < 0 ? "text-red-600" : "text-ink"}`}
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
            <p className="text-[10px] text-ink-faint mt-1">
              마이페이지에서 총 예산을 설정하면 여기에 반영돼요
            </p>
          )}
        </Link>
      </div>

      <TapeLabel className="mb-4" rotate={3}>
        더 챙겨볼 것들
      </TapeLabel>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* 재원 추가 - 결제 예정 (예약 기반) */}
        {pendingPayments.length > 0 && (
          <Link
            to="/mypage?tab=reservation"
            className="bg-white rounded-2xl border border-line p-6 hover:border-brand transition-colors"
          >
            <p className="text-xs text-ink-muted mb-2">결제 예정</p>
            <p
              className={`text-2xl font-medium mb-1 ${
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
        {/* 재원 추가 끝 */}
        <div className="bg-white rounded-2xl border border-line p-6 opacity-60">
          <p className="text-sm font-medium text-ink mb-1">AI 드레스</p>
          <p className="text-xs text-ink-faint">준비 중인 기능입니다</p>
        </div>
      </div>
    </PrepLayout>
  );
};

export default HubPage;
