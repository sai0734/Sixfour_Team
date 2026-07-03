import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import PrepLayout from "../../layouts/PrepLayout";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getListByMember as getChecklist } from "../../api/checklistApi";
import { getListByMember as getBudget } from "../../api/budgetApi";
import { getByMember as getWeddingPlan } from "../../api/weddingplanApi";

const HubPage = () => {
  const { loginState } = useCustomLogin();

  const [checklist, setChecklist] = useState([]);
  const [budgetList, setBudgetList] = useState([]);
  const [plan, setPlan] = useState(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    if (!loginState.email) return;

    Promise.all([
      getChecklist(loginState.email),
      getBudget(loginState.email),
      getWeddingPlan(loginState.email).catch(() => null),
    ]).then(([checklistData, budgetData, planData]) => {
      setChecklist(checklistData);
      setBudgetList(budgetData);
      setPlan(planData);
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

  const totalBudget = budgetList.reduce((s, i) => s + (i.budgetAmount || 0), 0);
  const totalActual = budgetList.reduce((s, i) => s + (i.actualAmount || 0), 0);
  const remaining = totalBudget - totalActual;

  const dday = plan?.weddingDate
    ? Math.ceil(
        (new Date(plan.weddingDate) - new Date(new Date().toDateString())) /
          (1000 * 60 * 60 * 24),
      )
    : null;

  return (
    <PrepLayout
      eyebrow="PREP HUB"
      title="준비 관리"
      subtitle="한눈에 보는 우리 결혼 준비 현황"
    >
      <div className="grid grid-cols-3 gap-4 mb-8">
        {/* D-day 카드 */}
        <div className="bg-white rounded-2xl border border-line p-6">
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
        </div>

        {/* 체크리스트 요약 카드 */}
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

        {/* 예산 요약 카드 */}
        <Link
          to="/budget/list"
          className="bg-white rounded-2xl border border-line p-6 hover:border-brand transition-colors"
        >
          <p className="text-xs text-ink-muted mb-2">예산</p>
          <p
            className={`text-2xl font-medium mb-1 ${remaining < 0 ? "text-red-600" : "text-ink"}`}
          >
            {remaining.toLocaleString()}원
          </p>
          <p className="text-xs text-ink-faint">
            지출 {totalActual.toLocaleString()} / {totalBudget.toLocaleString()}
            원
          </p>
        </Link>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white rounded-2xl border border-line p-6 opacity-60">
          <p className="text-sm font-medium text-ink mb-1">납부 관리</p>
          <p className="text-xs text-ink-faint">준비 중인 기능입니다</p>
        </div>
        <div className="bg-white rounded-2xl border border-line p-6 opacity-60">
          <p className="text-sm font-medium text-ink mb-1">AI 드레스</p>
          <p className="text-xs text-ink-faint">준비 중인 기능입니다</p>
        </div>
      </div>
    </PrepLayout>
  );
};

export default HubPage;
