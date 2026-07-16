import { Link, useLocation } from "react-router-dom";

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
      { name: "Q&A 관리", path: "/admin/qna", enabled: true },
    ],
  },
];

const ALL_ITEMS = MENU_GROUPS.flatMap((g) => g.items);

const isItemActive = (path, pathname) =>
  path === "/admin" ? pathname === "/admin" : pathname.startsWith(path);

const AdminSidebar = () => {
  const location = useLocation();

  return (
    <>
      <nav className="md:hidden -mx-3 px-3 sm:-mx-6 sm:px-6 mb-6 pt-5 flex gap-2 overflow-x-auto pb-1">
        {ALL_ITEMS.map((item) => {
          const isActive = isItemActive(item.path, location.pathname);
          return (
            <Link
              key={item.name}
              to={item.path}
              className={`shrink-0 h-9 px-4 flex items-center rounded-full text-xs font-medium border transition ${
                isActive
                  ? "bg-brand text-white border-brand"
                  : "bg-white text-ink-soft border-line-soft"
              }`}
            >
              {item.name}
            </Link>
          );
        })}
      </nav>

      <aside className="hidden md:block w-56 shrink-0 py-8 pr-6">
        {MENU_GROUPS.map((group) => (
          <div key={group.label} className="mb-7">
            <p className="text-[11px] text-ink-faint tracking-wide mb-2 px-3">
              {group.label}
            </p>
            <nav className="flex flex-col gap-1">
              {group.items.map((item) => {
                const isActive = isItemActive(item.path, location.pathname);

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
    </>
  );
};

export default AdminSidebar;
