import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
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
import { getCompanyImageUrl, getList } from "../../api/companyApi";
import { getDashboardSummary } from "../../api/adminDashboardApi";

const categoryLabel = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

const categoryColors = {
  HALL: "bg-emerald-500",
  DRESS: "bg-pink-500",
  MAKEUP: "bg-amber-500",
  STUDIO: "bg-blue-500",
};

const ORDER_STATUS_COLORS = ["#3b82f6", "#f59e0b", "#a855f7"];

// 오늘 발생량에 따른 카드 그라데이션 - 1건 이상이면 빨강, 0건이면 노랑
const KPI_GRADIENTS = {
  up: "from-[#f87171] to-[#dc2626]",
  flat: "from-[#fbbf24] to-[#eab308]",
};

const trendGradient = (todayValue) => (todayValue > 0 ? "up" : "flat");

const toneStyle = {
  info: "border-blue-400 bg-blue-50 text-blue-700",
  warning: "border-amber-400 bg-amber-50 text-amber-700",
  danger: "border-red-400 bg-red-50 text-red-700",
};

const initCompanyState = { dtoList: [], totalCount: 0 };

const AdminDashboardComponent = () => {
  const navigate = useNavigate();

  const [companyData, setCompanyData] = useState(initCompanyState);
  const [companyFetching, setCompanyFetching] = useState(true);
  const [companyError, setCompanyError] = useState("");

  const [summary, setSummary] = useState(null);
  const [summaryFetching, setSummaryFetching] = useState(true);
  const [summaryError, setSummaryError] = useState("");

  useEffect(() => {
    setCompanyFetching(true);
    setCompanyError("");

    getList({ page: 1, size: 100, sort: "latest" })
      .then((data) => {
        setCompanyData({
          ...initCompanyState,
          ...data,
          dtoList: data?.dtoList || [],
          totalCount: data?.totalCount || data?.dtoList?.length || 0,
        });
      })
      .catch((err) => {
        console.error(err);
        setCompanyError("등록 업체 데이터를 불러오지 못했습니다.");
      })
      .finally(() => setCompanyFetching(false));
  }, []);

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

  const companyStats = useMemo(() => {
    const companies = companyData.dtoList;
    const activeCompanies = companies.filter((company) => !company.delFlag);
    const totalPrice = activeCompanies.reduce((sum, company) => sum + Number(company.priceAvg || 0), 0);
    const pricedCompanies = activeCompanies.filter((company) => Number(company.priceAvg || 0) > 0);
    const averagePrice = pricedCompanies.length ? Math.round(totalPrice / pricedCompanies.length) : 0;

    const categoryCounts = activeCompanies.reduce((acc, company) => {
      const category = company.category || "ETC";
      acc[category] = (acc[category] || 0) + 1;
      return acc;
    }, {});

    const topCategory = Object.entries(categoryCounts).sort((a, b) => b[1] - a[1])[0];
    const highestPriceCompany = [...activeCompanies].sort(
      (a, b) => Number(b.priceAvg || 0) - Number(a.priceAvg || 0),
    )[0];
    const newestCompanies = [...activeCompanies].slice(0, 5);

    return {
      total: companyData.totalCount || companies.length,
      active: activeCompanies.length,
      averagePrice,
      categoryCounts,
      topCategory,
      highestPriceCompany,
      newestCompanies,
    };
  }, [companyData]);

  const maxCategoryCount = Math.max(1, ...Object.values(companyStats.categoryCounts));

  const categoryChartData = Object.entries(categoryLabel).map(([category, label]) => ({
    name: label,
    개수: companyStats.categoryCounts[category] || 0,
  }));

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
          gradient={summary ? trendGradient(summary.memberStats.newToday) : "flat"}
          icon={<UsersIcon />}
          value={summary ? `${summary.memberStats.total}명` : "-"}
          label="전체 회원"
          desc={`오늘 신규 ${summary?.memberStats.newToday ?? 0}명`}
        />
        <GradientKpi
          gradient={summary ? trendGradient(summary.orderStats.todayRevenue) : "flat"}
          icon={<WalletIcon />}
          value={summary ? `${Number(summary.orderStats.totalRevenue).toLocaleString()}원` : "-"}
          label="누적 매출"
          desc={summary ? <RevenueChangeBadge orderStats={summary.orderStats} light /> : "-"}
        />
        <GradientKpi
          gradient={summary ? trendGradient(summary.boardStats.todayCount) : "flat"}
          icon={<DocumentIcon />}
          value={summary ? `${summary.boardStats.total}건` : "-"}
          label="전체 게시글"
          desc={`오늘 ${summary?.boardStats.todayCount ?? 0}건 작성`}
        />
        <GradientKpi
          gradient={summary ? trendGradient(summary.orderStats.todayCount) : "flat"}
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
                <div className="text-xl font-semibold">{todo.count}건</div>
                <div className="mt-1 text-sm font-medium">{todo.label}</div>
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

      {/* ===== 예약 / 재고 요약 ===== */}
      <div className="mb-5 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Panel title="예약 현황" badge="전체 예약">
          <div className="space-y-3">
            <MiniStat label="전체 예약" value={summary ? `${summary.reservationStats.total}건` : "-"} />
            <MiniStat label="확정 대기" value={summary ? `${summary.reservationStats.pending}건` : "-"} tone="warning" />
            <MiniStat label="결제 완료" value={summary ? `${summary.reservationStats.paidCount}건` : "-"} tone="success" />
            <MiniStat label="이번 주 예식 예정" value={summary ? `${summary.reservationStats.weddingThisWeek}건` : "-"} tone="accent" />
          </div>
        </Panel>

        <Panel title="게시판 현황" badge="자유 · 후기">
          <div className="space-y-3">
            <MiniStat label="자유게시판" value={summary ? `${summary.boardStats.freeCount}건` : "-"} />
            <MiniStat label="후기게시판" value={summary ? `${summary.boardStats.reviewCount}건` : "-"} />
            <MiniStat label="오늘 작성" value={summary ? `${summary.boardStats.todayCount}건` : "-"} tone="accent" />
          </div>
        </Panel>

        <Panel title="재고 부족 상품" badge={`${summary?.productStats.lowStockCount ?? 0}개`}>
          {summary?.productStats.lowStockProducts?.length ? (
            <div className="space-y-2">
              {summary.productStats.lowStockProducts.map((product) => (
                <div key={product.pno} className="flex items-center justify-between rounded-md bg-red-50 px-3 py-2 text-sm">
                  <span className="truncate text-red-700">{product.pname}</span>
                  <span className="font-semibold text-red-700">{product.stockQty}개</span>
                </div>
              ))}
            </div>
          ) : (
            <EmptyText>{summaryFetching ? "불러오는 중..." : "재고 부족 상품이 없습니다."}</EmptyText>
          )}
        </Panel>
      </div>

      {/* ===== 업체 현황 (기존) ===== */}
      <div className="mb-2 mt-8 text-sm font-semibold text-slate-500">업체 현황</div>

      {companyError ? (
        <div className="mb-5 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {companyError}
        </div>
      ) : null}

      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="전체 업체" value={`${companyStats.total}개`} desc="등록된 전체 업체" />
        <Kpi label="활성 업체" value={`${companyStats.active}개`} desc="삭제 처리 제외" tone="success" />
        <Kpi label="평균 가격" value={`${companyStats.averagePrice.toLocaleString()}원`} desc="가격 등록 업체 기준" tone="accent" />
        <Kpi
          label="최다 유형"
          value={companyStats.topCategory ? categoryLabel[companyStats.topCategory[0]] || companyStats.topCategory[0] : "-"}
          desc={companyStats.topCategory ? `${companyStats.topCategory[1]}개 등록` : "데이터 없음"}
          tone="warning"
        />
      </div>

      <div className="mb-5 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Panel title="업체 유형 분포" badge="카테고리별">
          {companyFetching ? (
            <EmptyText>불러오는 중...</EmptyText>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={categoryChartData} layout="vertical" margin={{ left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" allowDecimals={false} fontSize={12} />
                <YAxis type="category" dataKey="name" fontSize={12} width={60} />
                <Tooltip />
                <Bar dataKey="개수" fill="#ec4899" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Panel>

        <Panel title="가격 요약" badge="평균가 기준">
          <div className="flex h-full flex-col justify-between gap-4">
            <div>
              <div className="text-sm text-slate-500">평균 가격</div>
              <div className="mt-2 text-3xl font-semibold text-blue-700">{companyStats.averagePrice.toLocaleString()}원</div>
            </div>
            <div className="rounded-md bg-slate-50 p-4">
              <div className="text-xs text-slate-500">최고가 업체</div>
              <div className="mt-1 truncate font-semibold">{companyStats.highestPriceCompany?.name || "-"}</div>
              <div className="mt-1 text-sm text-slate-600">
                {companyStats.highestPriceCompany?.priceAvg
                  ? `${Number(companyStats.highestPriceCompany.priceAvg).toLocaleString()}원`
                  : "가격 정보 없음"}
              </div>
            </div>
          </div>
        </Panel>

        <Panel title="운영 체크" badge="업체 데이터">
          <div className="space-y-3">
            <MiniStat label="노출 가능 업체" value={`${companyStats.active}개`} tone="accent" />
            <MiniStat label="비활성 업체" value={`${companyStats.total - companyStats.active}개`} tone="warning" />
          </div>
        </Panel>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Panel title="최근 등록 업체" badge="최신순 5개">
          <div className="space-y-3">
            {companyStats.newestCompanies.length ? (
              companyStats.newestCompanies.map((company, index) => (
                <CompanyRow key={company.cmno || company.name} rank={index + 1} company={company} />
              ))
            ) : (
              <EmptyText>등록된 업체가 없습니다.</EmptyText>
            )}
          </div>
        </Panel>

        <Panel title="카테고리별 요약" badge="등록 현황">
          <div className="grid grid-cols-2 gap-3">
            {Object.entries(categoryLabel).map(([category, label]) => (
              <div key={category} className="rounded-md bg-slate-50 p-4">
                <div className={`mb-3 h-2 w-10 rounded-full ${categoryColors[category]}`} />
                <div className="text-sm text-slate-500">{label}</div>
                <div className="mt-1 text-2xl font-semibold">{companyStats.categoryCounts[category] || 0}개</div>
              </div>
            ))}
          </div>
        </Panel>
      </div>
    </section>
  );
};

const Kpi = ({ label, value, desc, tone = "default" }) => {
  const toneClass = {
    default: "text-slate-900",
    success: "text-emerald-600",
    warning: "text-amber-600",
    accent: "text-blue-600",
  }[tone];

  return (
    <div className="rounded-lg bg-slate-50 p-4">
      <div className="text-xs text-slate-500">{label}</div>
      <div className={`mt-2 text-2xl font-semibold ${toneClass}`}>{value}</div>
      <div className="mt-2 text-xs text-slate-500">{desc}</div>
    </div>
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
const GradientKpi = ({ gradient, icon, value, label, desc }) => (
  <div className={`rounded-2xl bg-gradient-to-br ${KPI_GRADIENTS[gradient]} p-5 text-white shadow-md shadow-slate-200`}>
    <div className="flex items-start justify-between">
      <div>
        <div className="text-2xl font-bold">{value}</div>
        <div className="mt-1 text-sm text-white/85">{label}</div>
      </div>
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white/20">
        {icon}
      </div>
    </div>
    {desc ? <div className="mt-3 text-xs text-white/75">{desc}</div> : null}
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

const MiniStat = ({ label, value, tone = "default" }) => {
  const toneClass = {
    default: "text-slate-900",
    success: "text-emerald-600",
    warning: "text-amber-600",
    accent: "text-blue-600",
  }[tone];

  return (
    <div className="flex items-center justify-between rounded-md bg-slate-50 px-4 py-3">
      <span className="text-sm text-slate-500">{label}</span>
      <span className={`text-sm font-semibold ${toneClass}`}>{value}</span>
    </div>
  );
};

const CompanyRow = ({ rank, company }) => {
  const imageUrl = getCompanyImageUrl(company.mainImage || company.uploadFileNames?.[0], true);

  return (
    <div className="flex items-center gap-3 border-b border-slate-100 pb-3 last:border-b-0 last:pb-0">
      <div className="w-6 text-center text-sm font-semibold text-slate-400">{rank}</div>
      <div className="h-12 w-16 overflow-hidden rounded-xl bg-slate-100">
        {imageUrl ? (
          <img className="h-full w-full object-cover" src={imageUrl} alt={company.name} />
        ) : (
          <div className="flex h-full items-center justify-center text-xs text-slate-400">No Img</div>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-medium text-slate-900">{company.name || "업체명 없음"}</div>
        <div className="text-xs text-slate-500">{categoryLabel[company.category] || company.category || "기타"}</div>
      </div>
      <div className="text-right text-sm font-semibold text-blue-700">
        {company.priceAvg ? `${Number(company.priceAvg).toLocaleString()}원` : "-"}
      </div>
    </div>
  );
};

const EmptyText = ({ children }) => (
  <div className="rounded-md bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">{children}</div>
);

export default AdminDashboardComponent;
