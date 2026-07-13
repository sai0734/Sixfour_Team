import { useEffect, useRef } from "react";
import { Link, useLocation } from "react-router-dom";

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
  const activeRef = useRef(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
      inline: "center",
    });
  }, [location.pathname]);

  const isActivePath = (path) => {
    if (path === "/mypage") return location.pathname === "/mypage";
    return location.pathname.startsWith(path);
  };

  return (
    <aside className="sticky top-[68px] z-20 w-full bg-[#FBF7F0]/95 py-3 backdrop-blur lg:static lg:z-auto lg:w-60 lg:shrink-0 lg:bg-transparent lg:py-8 lg:pr-6 lg:backdrop-blur-0">
      {MENU_GROUPS.map((group) => (
        <div
          key={group.label}
          className="relative overflow-hidden rounded-2xl bg-white p-3 lg:p-4"
          style={{ boxShadow: "0 8px 24px -14px rgba(58,54,47,0.25)" }}
        >
          <p className="hidden px-3 text-xs font-bold tracking-wide text-[#7C8B6F] lg:mb-3 lg:block">
            {group.label}
          </p>

          <nav className="flex gap-2 overflow-x-auto scroll-smooth pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden lg:flex-col lg:gap-1 lg:overflow-visible lg:pb-0">
            {group.items.map((item) => {
              const isActive = isActivePath(item.path);

              return (
                <Link
                  key={item.name}
                  ref={isActive ? activeRef : null}
                  to={item.path}
                  className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2.5 text-sm font-medium transition-colors lg:flex lg:items-center lg:gap-2 ${
                    isActive
                      ? "bg-[#7C8B6F] text-white shadow-[0_4px_12px_-4px_rgba(92,107,79,0.55)]"
                      : "bg-[#F7F2EA] text-[#4A3F38] hover:bg-[#E6EBDD] lg:bg-transparent"
                  }`}
                >
                  {item.name}
                </Link>
              );
            })}
          </nav>

          <div className="pointer-events-none absolute bottom-0 right-0 top-0 w-8 bg-gradient-to-l from-white to-transparent lg:hidden" />
        </div>
      ))}
    </aside>
  );
};

export default MyPageSidebar;
