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
    <aside className="w-full py-4 lg:w-60 lg:shrink-0 lg:py-8 lg:pr-6">
      {MENU_GROUPS.map((group) => (
        <div
          key={group.label}
          className="rounded-2xl bg-white p-4"
          style={{ boxShadow: "0 8px 24px -14px rgba(58,54,47,0.25)" }}
        >
          <p
            className="mb-3 px-3 text-xs font-bold tracking-wide"
            style={{ color: "#C06080" }}
          >
            {group.label}
          </p>
          <nav className="flex flex-col gap-1">
            {group.items.map((item) => {
              const isActive = location.pathname.startsWith(item.path);

              return (
                <Link
                  key={item.name}
                  to={item.path}
                  className="flex items-center gap-2 rounded-full px-4 py-2.5 text-sm font-medium transition-colors"
                  style={
                    isActive
                      ? {
                          background: "#C06080",
                          color: "#FFFFFF",
                          boxShadow: "0 4px 12px -4px rgba(192,96,128,0.6)",
                        }
                      : { color: "#4A3F38" }
                  }
                  onMouseEnter={(e) => {
                    if (!isActive) e.currentTarget.style.background = "#FFE2E2";
                  }}
                  onMouseLeave={(e) => {
                    if (!isActive)
                      e.currentTarget.style.background = "transparent";
                  }}
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
