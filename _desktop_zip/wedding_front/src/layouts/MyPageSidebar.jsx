import { Link, useLocation } from "react-router-dom";

// 준비관리(PrepSidebar)와 분리된 마이페이지 전용 사이드바.
// 마이페이지 내부 탭(플랜/예약현황/결제내역/찜목록)은 아직 별도 라우트가 아니라
// HubPage 내부 상태로 전환되므로, 여기서는 "마이페이지" 진입 링크와
// 그 바깥에 있는 "회원정보 수정" 링크만 보여준다.
const MENU_GROUPS = [
  {
    label: "마이페이지",
    items: [
      { name: "마이페이지", path: "/mypage", enabled: true },
      { name: "회원정보 수정", path: "/auth/modify", enabled: true },
    ],
  },
];

const MyPageSidebar = () => {
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
              const isActive = location.pathname.startsWith(item.path);

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

export default MyPageSidebar;
