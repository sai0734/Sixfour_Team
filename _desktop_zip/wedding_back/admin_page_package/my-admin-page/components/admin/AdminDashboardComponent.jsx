import { useMemo, useState } from "react";

const dashboardData = {
  day: {
    revenue: "830,000",
    reservations: "8",
    members: "3",
    labels: ["1", "5", "10", "15", "20", "25", "30"],
    bars: [35, 52, 30, 44, 66, 58, 49],
  },
  month: {
    revenue: "9,400,000",
    reservations: "234",
    members: "89",
    labels: ["1월", "2월", "3월", "4월", "5월", "6월"],
    bars: [42, 58, 51, 67, 74, 89],
  },
  year: {
    revenue: "90,000,000",
    reservations: "2,814",
    members: "1,072",
    labels: ["2022", "2023", "2024", "2025"],
    bars: [51, 62, 74, 89],
  },
};

const AdminDashboardComponent = () => {
  const [period, setPeriod] = useState("month");
  const data = dashboardData[period];

  const today = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, "0")}.${String(now.getDate()).padStart(2, "0")}`;
  }, []);

  return (
    <section className="mx-auto max-w-6xl p-4 text-slate-800">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">관리자 대시보드</h2>
          <p className="mt-1 text-sm text-slate-500">{today} 기준 통계와 처리 필요 항목입니다.</p>
        </div>
        <div className="flex rounded-md border border-slate-200 bg-white p-1">
          {[
            ["day", "일"],
            ["month", "월"],
            ["year", "년"],
          ].map(([item, label]) => (
            <button
              key={item}
              className={`h-8 rounded px-3 text-sm capitalize ${period === item ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-50"}`}
              type="button"
              onClick={() => setPeriod(item)}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Kpi label="총 매출" value={`${data.revenue}원`} delta="+20%" />
        <Kpi label="총 예약" value={`${data.reservations}건`} delta="+14%" />
        <Kpi label="신규 회원" value={`${data.members}명`} delta="+8%" />
        <Kpi label="활성 업체" value="128개" delta="+6개" />
      </div>

      <div className="mb-5 grid gap-4 lg:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white p-5 lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-semibold">매출 추이</h3>
            <div className="text-xs text-slate-500">선택 기간</div>
          </div>
          <div className="flex h-56 items-end gap-3">
            {data.bars.map((bar, index) => (
              <div key={data.labels[index]} className="flex flex-1 flex-col items-center gap-2">
                <div className="w-full rounded-t bg-blue-600" style={{ height: `${bar}%` }} />
                <span className="text-xs text-slate-500">{data.labels[index]}</span>
              </div>
            ))}
          </div>
        </div>

        <Panel title="예약 현황" badge="이번 달 234건">
          <div className="space-y-3">
            <Progress label="확정" value={148} total={234} color="bg-blue-600" />
            <Progress label="대기" value={54} total={234} color="bg-amber-500" />
            <Progress label="완료" value={32} total={234} color="bg-emerald-500" />
          </div>
        </Panel>

        <Panel title="회원 증가 추이" badge="+89명">
          <div className="grid grid-cols-6 items-end gap-2 pt-8">
            {[30, 46, 38, 62, 52, 72].map((height, index) => (
              <div key={index} className="rounded-t bg-emerald-500" style={{ height: `${height + 30}px` }} />
            ))}
          </div>
        </Panel>
      </div>

      <div className="mb-5">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="font-semibold">처리 필요 항목</h3>
          <span className="text-xs text-slate-500">최신 데이터 기준</span>
        </div>
        <div className="grid gap-3 md:grid-cols-3">
          <Todo tone="danger" count="9" title="업체 승인 대기" desc="신규 등록 업체 검토가 필요합니다." />
          <Todo tone="warning" count="54" title="예약 확정 대기" desc="업체 응답을 기다리는 예약이 있습니다." />
          <Todo tone="info" count="12" title="미답변 문의" desc="고객 문의에 대한 답변이 필요합니다." />
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Panel title="인기 업체 TOP 5" badge="예약 수 기준">
          {["그랜드 웨딩홀", "더가운 스튜디오", "하루 스튜디오", "세림 드레스", "블룸 메이크업"].map((name, index) => (
            <RankRow key={name} rank={index + 1} name={name} sub={index % 2 === 0 ? "서울" : "경기"} value={`${52 - index * 7}건`} />
          ))}
        </Panel>
        <Panel title="최근 예약" badge="오늘 기준">
          {["김지윤", "이민준", "박수연", "최영호", "정하나"].map((name, index) => (
            <RankRow key={name} rank={index + 1} name={name} sub="웨딩 패키지" value={index % 2 === 0 ? "확정" : "대기"} />
          ))}
        </Panel>
      </div>
    </section>
  );
};

const Kpi = ({ label, value, delta }) => (
  <div className="rounded-lg bg-slate-50 p-4">
    <div className="text-xs text-slate-500">{label}</div>
    <div className="mt-2 text-2xl font-semibold">{value}</div>
    <div className="mt-2 text-xs text-emerald-600">이전 대비 {delta}</div>
  </div>
);

const Panel = ({ title, badge, children }) => (
  <div className="rounded-lg border border-slate-200 bg-white p-5">
    <div className="mb-4 flex items-center justify-between">
      <h3 className="font-semibold">{title}</h3>
      <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs text-blue-700">{badge}</span>
    </div>
    {children}
  </div>
);

const Progress = ({ label, value, total, color }) => (
  <div>
    <div className="mb-1 flex justify-between text-sm">
      <span>{label}</span>
      <span className="font-medium">{value}</span>
    </div>
    <div className="h-2 rounded-full bg-slate-100">
      <div className={`h-2 rounded-full ${color}`} style={{ width: `${(value / total) * 100}%` }} />
    </div>
  </div>
);

const Todo = ({ tone, count, title, desc }) => {
  const toneMap = {
    danger: "border-red-400 bg-red-50 text-red-700",
    warning: "border-amber-400 bg-amber-50 text-amber-700",
    info: "border-blue-400 bg-blue-50 text-blue-700",
  };
  return (
    <div className={`rounded-lg border-l-4 p-4 ${toneMap[tone]}`}>
      <div className="mb-2 text-xl font-semibold">{count}</div>
      <div className="font-medium">{title}</div>
      <div className="mt-1 text-sm opacity-80">{desc}</div>
    </div>
  );
};

const RankRow = ({ rank, name, sub, value }) => (
  <div className="flex items-center gap-3 border-b border-slate-100 py-3 last:border-b-0">
    <div className="w-6 text-center text-sm font-semibold text-slate-400">{rank}</div>
    <div className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-50 text-sm font-semibold text-blue-700">{name[0]}</div>
    <div className="min-w-0 flex-1">
      <div className="truncate text-sm font-medium">{name}</div>
      <div className="text-xs text-slate-500">{sub}</div>
    </div>
    <div className="text-sm font-semibold text-blue-700">{value}</div>
  </div>
);

export default AdminDashboardComponent;
