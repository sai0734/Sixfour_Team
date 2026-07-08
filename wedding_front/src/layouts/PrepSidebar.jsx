import { Link, useLocation } from "react-router-dom";

// 실제로 만들어진 기능만 enabled: true. 나머지는 목업에만 있는 예정 기능이라 클릭해도 이동 안 함.
// 마이페이지 관련 항목은 MyPageSidebar로 분리했음 (구조 분리)
const MENU_GROUPS = [
  {
    label: "준비 관리",
    items: [
      { name: "준비 관리 허브", path: "/prep/hub", enabled: true },
      { name: "체크리스트", path: "/checklist/list", enabled: true },
      { name: "예산 관리", path: "/budget/list", enabled: true },
      { name: "D-day 관리", path: "/prep/dday", enabled: true },
      { name: "납부 관리", path: "/prep/payment", enabled: true },
      { name: "AI 드레스", path: "/prep/ai-dress", enabled: false },
    ],
  },
];

const PrepSidebar = () => {
  const location = useLocation();

  return (
    <aside className="w-56 shrink-0 py-8 pr-6">
      {MENU_GROUPS.map((group) => (
        <div key={group.label} className="mb-7">
          <p className="text-[11px] text-ink-faint tracking-wide mb-2 px-3">
            {group.label}
          </p>
          <nav className="flex flex-col gap-1">
            {group.items.map((item) => {
              const isActive =
                location.pathname.startsWith(item.path.split("/list")[0]) &&
                item.enabled;

              if (!item.enabled) {
                return (
                  <span
                    key={item.name}
                    title="준비 중인 기능입니다"
                    className="flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm text-ink-faint cursor-not-allowed select-none"
                  >
                    {item.name}
                  </span>
                );
              }

              return (
                <Link
                  key={item.name}
                  to={item.path}
                  className={`flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                    isActive
                      ? "bg-brand-light text-brand-accent font-medium"
                      : "text-ink-soft hover:bg-cream"
                  }`}
                >
                  {item.name}
                </Link>
              );
            })}
          </nav>
        </div>
      ))}
    </aside>
  );
};

export default PrepSidebar;
