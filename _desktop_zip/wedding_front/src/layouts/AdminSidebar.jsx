import { Link, useLocation } from "react-router-dom";

// 관리자 페이지 전용 좌측 메뉴. 회원/업체 관리는 다른 담당자가 만든 화면으로 연결만 함.
const MENU_GROUPS = [
  {
    label: "관리자 홈",
    items: [{ name: "대시보드", path: "/admin", enabled: true }],
  },
  {
    label: "관리 메뉴",
    items: [
      { name: "회원 관리", path: "/admin/members", enabled: true },
      { name: "업체 관리", path: "/admin/companies", enabled: true },
      { name: "상품 관리", path: "/admin/products", enabled: true },
      { name: "주문 관리", path: "/admin/orders", enabled: true },
    ],
  },
];

const AdminSidebar = () => {
  const location = useLocation();

  return (
    <aside className="hidden md:block w-56 shrink-0 py-8 pr-6">
      {MENU_GROUPS.map((group) => (
        <div key={group.label} className="mb-7">
          <p className="text-[11px] text-ink-faint tracking-wide mb-2 px-3">
            {group.label}
          </p>
          <nav className="flex flex-col gap-1">
            {group.items.map((item) => {
              // "/admin"은 정확히 일치할 때만 활성화 (그래야 다른 하위 메뉴들과 안 겹침)
              const isActive =
                item.path === "/admin"
                  ? location.pathname === "/admin"
                  : location.pathname.startsWith(item.path);

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

export default AdminSidebar;
