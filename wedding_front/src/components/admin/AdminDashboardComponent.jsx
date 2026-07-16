import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { getDashboardSummary } from "../../api/adminDashboardApi";

const ORDER_STATUS_COLORS = ["#3b82f6", "#f59e0b", "#a855f7"];

// 상단 KPI 카드 고정 색상 (빨강/파랑은 "오늘의 할 일" 톤과 겹치지 않도록 제외)
const KPI_GRADIENTS = {
  pink: "from-[#f472b6] to-[#ec4899]",
  purple: "from-[#a855f7] to-[#8b5cf6]",
  green: "from-[#34d399] to-[#10b981]",
  amber: "from-[#fbbf24] to-[#f97316]",
};

// 업체 현황 카드 4개(웨딩홀/드레스/스튜디오/메이크업)에 순서대로 매길 색상
const CATEGORY_CARD_GRADIENTS = ["pink", "purple", "green", "amber"];

const toneStyle = {
  info: "border-blue-400 bg-blue-50 text-blue-700",
  warning: "border-amber-400 bg-amber-50 text-amber-700",
  danger: "border-red-400 bg-red-50 text-red-700",
};

const AdminDashboardComponent = () => {
  const navigate = useNavigate();

  const [summary, setSummary] = useState(null);
  const [summaryFetching, setSummaryFetching] = useState(true);
  const [summaryError, setSummaryError] = useState("");

  useEffect(() => {
    setSummaryFetching(true);
    setSummaryError("");

    getDashboardSummary()
      .then((data) => setSummary(data))
      .catch((err) => {
        console.error(err);
        setSummaryError("대시보드 요약 데이터를 불러오지 못했습니다.");
      })
      .finally(() => setSummaryFetching(false));
  }, []);

  const today = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, "0")}.${String(now.getDate()).padStart(2, "0")}`;
  }, []);

  const orderChartData = summary
    ? [
        { name: "결제완료", 건수: summary.orderStats.paid },
        { name: "배송중", 건수: summary.orderStats.shipping },
        { name: "배송완료", 건수: summary.orderStats.delivered },
      ]
    : [];

  const handleTodoClick = (todo) => {
    if (todo.link) navigate(todo.link);
  };

  return (
    <section className="mx-auto max-w-6xl p-4 text-slate-800">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">관리자 대시보드</h2>
          <p className="mt-1 text-sm text-slate-500">{today} 기준 전체 운영 현황입니다.</p>
        </div>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
          {summaryFetching ? "데이터 불러오는 중" : "실시간 요약"}
        </span>
      </div>

      {summaryError ? (
        <div className="mb-5 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {summaryError}
        </div>
      ) : null}

      {/* ===== 전체 핵심 지표 (전체 회원이 맨 위) ===== */}
      <div className="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <GradientKpi
          gradient="pink"
          icon={<UsersIcon />}
          value={summary ? `${summary.memberStats.total}명` : "-"}
          label="전체 회원"
          desc={`오늘 신규 ${summary?.memberStats.newToday ?? 0}명`}
        />
        <GradientKpi
          gradient="purple"
          icon={<WalletIcon />}
          value={summary ? `${Number(summary.orderStats.totalRevenue).toLocaleString()}원` : "-"}
          label="누적 매출"
          desc={summary ? <RevenueChangeBadge orderStats={summary.orderStats} light /> : "-"}
        />
        <GradientKpi
          gradient="green"
          icon={<DocumentIcon />}
          value={summary ? `${summary.boardStats.total}건` : "-"}
          label="전체 게시글"
          desc={`오늘 ${summary?.boardStats.todayCount ?? 0}건 작성`}
        />
        <GradientKpi
          gradient="amber"
          icon={<OrderIcon />}
          value={
            summary
              ? `${summary.orderStats.paid + summary.orderStats.shipping + summary.orderStats.delivered}건`
              : "-"
          }
          label="전체 주문"
          desc={summary ? `오늘 ${summary.orderStats.todayCount}건` : "-"}
        />
      </div>

      {/* ===== 월별 매출 추이 ===== */}
      <Panel title="월별 매출 추이" badge="최근 6개월">
        {summary?.monthlyRevenue?.length ? (
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={summary.monthlyRevenue.map((row) => ({ ...row, 매출: row.revenue }))}>
              <defs>
                <linearGradient id="revenueFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.35} />
                  <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#eef2f7" />
              <XAxis dataKey="month" fontSize={12} axisLine={false} tickLine={false} />
              <YAxis fontSize={12} axisLine={false} tickLine={false} tickFormatter={(v) => `${Math.round(v / 10000)}만`} />
              <Tooltip formatter={(value) => `${Number(value).toLocaleString()}원`} />
              <Area
                type="monotone"
                dataKey="매출"
                stroke="#3b82f6"
                strokeWidth={3}
                fill="url(#revenueFill)"
                dot={{ r: 4, fill: "#3b82f6", strokeWidth: 0 }}
                activeDot={{ r: 6 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <EmptyText>{summaryFetching ? "불러오는 중..." : "매출 데이터가 없습니다."}</EmptyText>
        )}
      </Panel>

      {/* ===== 오늘의 할 일 ===== */}
      <Panel title="오늘의 할 일" badge="관리자 액션 필요">
        {summaryFetching ? (
          <EmptyText>불러오는 중...</EmptyText>
        ) : summary?.todos?.length ? (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {summary.todos.map((todo, index) => (
              <button
                key={index}
                type="button"
                onClick={() => handleTodoClick(todo)}
                disabled={!todo.link}
                className={`rounded-lg border-l-4 p-4 text-left transition ${toneStyle[todo.tone] || toneStyle.info} ${
                  todo.link ? "cursor-pointer hover:opacity-80" : "cursor-default"
                }`}
              >
                {todo.breakdown?.length ? (
                  <>
                    <div className="mb-2 text-sm font-medium">{todo.label}</div>
                    <div className="grid grid-cols-2 gap-x-3 gap-y-1">
                      {todo.breakdown.map((row) => (
                        <div key={row.label} className="flex items-baseline justify-between gap-1">
                          <span className="text-xs opacity-80">{row.label}</span>
                          <span className="text-sm font-semibold">{row.count}건</span>
                        </div>
                      ))}
                    </div>
                  </>
                ) : (
                  <>
                    <div className="text-xl font-semibold">{todo.count}건</div>
                    <div className="mt-1 text-sm font-medium">{todo.label}</div>
                  </>
                )}
              </button>
            ))}
          </div>
        ) : (
          <EmptyText>오늘 처리할 항목이 없습니다. 🎉</EmptyText>
        )}
      </Panel>

      {/* ===== 주문 차트 ===== */}
      <Panel title="주문 상태 현황" badge="PENDING 제외">
        {orderChartData.some((row) => row["건수"] > 0) ? (
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie
                data={orderChartData}
                dataKey="건수"
                nameKey="name"
                innerRadius={55}
                outerRadius={80}
                paddingAngle={3}
              >
                {orderChartData.map((entry, index) => (
                  <Cell key={entry.name} fill={ORDER_STATUS_COLORS[index % ORDER_STATUS_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        ) : (
          <EmptyText>{summaryFetching ? "불러오는 중..." : "주문 데이터가 없습니다."}</EmptyText>
        )}
      </Panel>

      {/* ===== 업체 현황 ===== */}
      <div className="mb-2 mt-8 text-sm font-semibold text-slate-500">업체 현황</div>

      <div className="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {CATEGORY_CARD_GRADIENTS.map((gradient, index) => {
          const card = summary?.categoryRevenueCards?.[index];
          return (
            <GradientKpi
              key={gradient}
              gradient={gradient}
              icon={<WalletIcon />}
              label={card ? card.categoryLabel : "-"}
              breakdown={[
                {
                  label: "매출최고",
                  value: card ? `${card.topCompanyName} · ${Number(card.topAmount).toLocaleString()}원` : "-",
                },
                {
                  label: "평균매출",
                  value: card ? `${Number(card.averageAmount).toLocaleString()}원` : "-",
                },
                {
                  label: "매출최저",
                  value: card ? `${card.bottomCompanyName} · ${Number(card.bottomAmount).toLocaleString()}원` : "-",
                },
              ]}
            />
          );
        })}
      </div>

      <Panel title="업체 매출 전체 순위" badge="TOP 10">
        {summaryFetching ? (
          <EmptyText>불러오는 중...</EmptyText>
        ) : summary?.topCompaniesOverall?.length ? (
          <div className="space-y-2">
            {summary.topCompaniesOverall.map((item) => (
              <div
                key={item.rank}
                className="flex items-center gap-3 rounded-md bg-slate-50 px-4 py-2.5"
              >
                <div className="w-6 text-center text-sm font-semibold text-slate-400">{item.rank}</div>
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium text-slate-900">{item.companyName}</div>
                  <div className="text-xs text-slate-500">{item.categoryLabel}</div>
                </div>
                <div className="text-right text-sm font-semibold text-blue-700">
                  {Number(item.amount).toLocaleString()}원
                </div>
              </div>
            ))}
          </div>
        ) : (
          <EmptyText>매출 데이터가 없습니다.</EmptyText>
        )}
      </Panel>
    </section>
  );
};

const Panel = ({ title, badge, children }) => (
  <div className="mb-5 rounded-lg border border-slate-200 bg-white p-5">
    <div className="mb-4 flex items-center justify-between">
      <h3 className="font-semibold">{title}</h3>
      <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs text-blue-700">{badge}</span>
    </div>
    {children}
  </div>
);

// 이번 달 매출을 지난달과 비교해서 증감률/증감액을 뱃지로 보여줌
const RevenueChangeBadge = ({ orderStats, light = false }) => {
  const { currentMonthRevenue, lastMonthRevenue, revenueChangeAmount, revenueChangeRate } = orderStats;

  if (lastMonthRevenue === 0) {
    return (
      <span className={light ? "text-white/80" : "text-slate-500"}>
        이번 달 {Number(currentMonthRevenue).toLocaleString()}원 (지난달 데이터 없음)
      </span>
    );
  }

  const isUp = revenueChangeAmount > 0;
  const isFlat = revenueChangeAmount === 0;
  const colorClass = light
    ? "text-white/85"
    : isFlat
      ? "text-slate-500"
      : isUp
        ? "text-red-600"
        : "text-blue-600";
  const arrow = isFlat ? "" : isUp ? "▲" : "▼";

  return (
    <span className={colorClass}>
      전월대비 {arrow} {Math.abs(revenueChangeRate ?? 0).toFixed(1)}%
      {" "}({isUp ? "+" : isFlat ? "" : "-"}{Math.abs(revenueChangeAmount).toLocaleString()}원)
    </span>
  );
};

// 상단 핵심 지표용 그라데이션 카드 (레퍼런스 UI의 컬러풀한 KPI 카드 스타일)
const GradientKpi = ({ gradient, icon, value, label, desc, breakdown }) => (
  <div className={`rounded-2xl bg-gradient-to-br ${KPI_GRADIENTS[gradient]} p-5 text-white shadow-md shadow-slate-200`}>
    <div className="flex items-start justify-between">
      <div className="min-w-0 flex-1">
        {breakdown?.length ? (
          <div className="text-sm font-semibold">{label}</div>
        ) : (
          <>
            <div className="text-2xl font-bold">{value}</div>
            <div className="mt-1 text-sm text-white/85">{label}</div>
          </>
        )}
      </div>
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white/20">
        {icon}
      </div>
    </div>

    {breakdown?.length ? (
      <div className="mt-3 space-y-2">
        {breakdown.map((row) => (
          <div key={row.label} className="text-xs">
            <div className="text-white/80">{row.label}</div>
            <div className="mt-0.5 break-words font-semibold">{row.value}</div>
          </div>
        ))}
      </div>
    ) : desc ? (
      <div className="mt-3 text-xs text-white/75">{desc}</div>
    ) : null}
  </div>
);

const UsersIcon = () => (
  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m6-1.13a4 4 0 10-4-4 4 4 0 004 4zm6 1a4 4 0 10-4-4" />
  </svg>
);

const WalletIcon = () => (
  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M21 12V7H5a2 2 0 010-4h14v4M3 5v14a2 2 0 002 2h16v-5" />
    <path strokeLinecap="round" strokeLinejoin="round" d="M18 12a2 2 0 100 4 2 2 0 000-4z" />
  </svg>
);

const DocumentIcon = () => (
  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
  </svg>
);

const OrderIcon = () => (
  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
    <path strokeLinecap="round" strokeLinejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
  </svg>
);

const EmptyText = ({ children }) => (
  <div className="rounded-md bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">{children}</div>
);

export default AdminDashboardComponent;
