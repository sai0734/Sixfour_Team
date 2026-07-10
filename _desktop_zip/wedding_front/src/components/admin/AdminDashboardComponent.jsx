import { useEffect, useMemo, useState } from "react";
import { getCompanyImageUrl, getList } from "../../api/companyApi";

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

const initState = {
  dtoList: [],
  totalCount: 0,
};

const AdminDashboardComponent = () => {
  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setFetching(true);
    setError("");

    getList({ page: 1, size: 100, sort: "latest" })
      .then((data) => {
        setServerData({
          ...initState,
          ...data,
          dtoList: data?.dtoList || [],
          totalCount: data?.totalCount || data?.dtoList?.length || 0,
        });
      })
      .catch((err) => {
        console.error(err);
        setError("등록 업체 데이터를 불러오지 못했습니다.");
      })
      .finally(() => {
        setFetching(false);
      });
  }, []);

  const today = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, "0")}.${String(now.getDate()).padStart(2, "0")}`;
  }, []);

  const stats = useMemo(() => {
    const companies = serverData.dtoList;
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
    const highestPriceCompany = [...activeCompanies].sort((a, b) => Number(b.priceAvg || 0) - Number(a.priceAvg || 0))[0];
    const newestCompanies = [...activeCompanies].slice(0, 5);

    return {
      total: serverData.totalCount || companies.length,
      active: activeCompanies.length,
      averagePrice,
      categoryCounts,
      topCategory,
      highestPriceCompany,
      newestCompanies,
    };
  }, [serverData]);

  const maxCategoryCount = Math.max(1, ...Object.values(stats.categoryCounts));

  return (
    <section className="mx-auto max-w-6xl p-4 text-slate-800">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">관리자 대시보드</h2>
          <p className="mt-1 text-sm text-slate-500">{today} 기준 등록 업체 현황입니다.</p>
        </div>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
          {fetching ? "데이터 불러오는 중" : `업체 ${stats.total}개 분석`}
        </span>
      </div>

      {error ? (
        <div className="mb-5 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      ) : null}

      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="전체 업체" value={`${stats.total}개`} desc="등록된 전체 업체" />
        <Kpi label="활성 업체" value={`${stats.active}개`} desc="삭제 처리 제외" tone="success" />
        <Kpi label="평균 가격" value={`${stats.averagePrice.toLocaleString()}원`} desc="가격 등록 업체 기준" tone="accent" />
        <Kpi
          label="최다 유형"
          value={stats.topCategory ? categoryLabel[stats.topCategory[0]] || stats.topCategory[0] : "-"}
          desc={stats.topCategory ? `${stats.topCategory[1]}개 등록` : "데이터 없음"}
          tone="warning"
        />
      </div>

      <div className="mb-5 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Panel title="업체 유형 분포" badge="카테고리별">
          <div className="space-y-4">
            {Object.entries(categoryLabel).map(([category, label]) => {
              const count = stats.categoryCounts[category] || 0;
              const percent = Math.round((count / maxCategoryCount) * 100);

              return (
                <div key={category}>
                  <div className="mb-1 flex justify-between text-sm">
                    <span>{label}</span>
                    <span className="font-medium">{count}개</span>
                  </div>
                  <div className="h-2 rounded-full bg-slate-100">
                    <div className={`h-2 rounded-full ${categoryColors[category]}`} style={{ width: `${percent}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </Panel>

        <Panel title="가격 요약" badge="평균가 기준">
          <div className="flex h-full flex-col justify-between gap-4">
            <div>
              <div className="text-sm text-slate-500">평균 가격</div>
              <div className="mt-2 text-3xl font-semibold text-blue-700">{stats.averagePrice.toLocaleString()}원</div>
            </div>
            <div className="rounded-md bg-slate-50 p-4">
              <div className="text-xs text-slate-500">최고가 업체</div>
              <div className="mt-1 truncate font-semibold">{stats.highestPriceCompany?.name || "-"}</div>
              <div className="mt-1 text-sm text-slate-600">
                {stats.highestPriceCompany?.priceAvg ? `${Number(stats.highestPriceCompany.priceAvg).toLocaleString()}원` : "가격 정보 없음"}
              </div>
            </div>
          </div>
        </Panel>

        <Panel title="운영 체크" badge="업체 데이터">
          <div className="space-y-3">
            <Todo tone="info" count={stats.active} title="노출 가능 업체" desc="사용자에게 보여줄 수 있는 업체입니다." />
            <Todo tone="warning" count={stats.total - stats.active} title="비활성 업체" desc="삭제 처리되었거나 숨김 상태입니다." />
          </div>
        </Panel>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Panel title="최근 등록 업체" badge="최신순 5개">
          <div className="space-y-3">
            {stats.newestCompanies.length ? (
              stats.newestCompanies.map((company, index) => (
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
                <div className="mt-1 text-2xl font-semibold">{stats.categoryCounts[category] || 0}개</div>
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
  <div className="rounded-lg border border-slate-200 bg-white p-5">
    <div className="mb-4 flex items-center justify-between">
      <h3 className="font-semibold">{title}</h3>
      <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs text-blue-700">{badge}</span>
    </div>
    {children}
  </div>
);

const Todo = ({ tone, count, title, desc }) => {
  const toneMap = {
    warning: "border-amber-400 bg-amber-50 text-amber-700",
    info: "border-blue-400 bg-blue-50 text-blue-700",
  };

  return (
    <div className={`rounded-lg border-l-4 p-4 ${toneMap[tone]}`}>
      <div className="mb-2 text-xl font-semibold">{count}개</div>
      <div className="font-medium">{title}</div>
      <div className="mt-1 text-sm opacity-80">{desc}</div>
    </div>
  );
};

const CompanyRow = ({ rank, company }) => {
  const imageUrl = getCompanyImageUrl(company.mainImage || company.uploadFileNames?.[0], true);

  return (
    <div className="flex items-center gap-3 border-b border-slate-100 pb-3 last:border-b-0 last:pb-0">
      <div className="w-6 text-center text-sm font-semibold text-slate-400">{rank}</div>
      <div className="h-12 w-16 overflow-hidden rounded-md bg-slate-100">
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
