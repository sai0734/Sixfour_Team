import { useEffect } from "react";
import {
  //ysj 대시보드 시각화
  Bar,
  BarChart,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

const COLORS = ["#2563eb", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6"];

function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(null);

  useEffect(() => {
    fetch("http://localhost:8080/api/admin.dashboard")
      .then((res) => res.json())
      .then((data) => setDashboard(data));
  }, []);

  if (!dashboard) return <div> Loading...</div>;
  return (
    <main>
      <section classname="summary grid">
        <article>
          <span>총 스튜디오</span>
          <strong>{dashboard.summary.totalStudioCount}개</strong>
        </article>

        <article>
          <span>평균평점</span>
          <strong>{dashboard.summary.average.Rating}</strong>
        </article>

        <article>
          <span>인기 업체</span>
          <strong>{dashboard.summary.topStudio}</strong>
        </article>

        <article>
          <span>최저가 업체</span>
          <strong>{dashboard.summary.lowestpriceStudio}</strong>
        </article>
      </section>

      <section className="chart-grid">
        <article>
          <h3>테마별 업체 수</h3>

          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie
                data={dashboard.themeStats}
                datakey="count"
                nameKey="theme"
                outerRadius={90}
                label
              >
                {dashboard.themeStats.map((_, index) => (
                  <Cell key={index} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </article>

        <article>
          <h3>업체별 방문자 수</h3>

          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={dashboard.visitStats}>
              <XAxis datakey="name" />
              <YAxis />
              <Tooltip />
              <Bar datakey="visits" fill="#2563eb" />
            </BarChart>
          </ResponsiveContainer>
        </article>
      </section>
    </main>
  );
}

export default AdminDashboardPage;
