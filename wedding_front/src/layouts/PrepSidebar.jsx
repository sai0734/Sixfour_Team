import { useEffect, useRef } from "react";
import { Link, useLocation } from "react-router-dom";

// 실제로 만들어진 기능만 enabled: true.
// 모바일에서는 가로 스크롤 탭, PC에서는 기존 세로 사이드바로 표시한다.
const MENU_GROUPS = [
  {
    label: "준비 관리",
    items: [
      {
        name: "준비 관리 허브",
        shortName: "허브",
        path: "/prep/hub",
        enabled: true,
      },
      {
        name: "체크리스트",
        shortName: "체크리스트",
        path: "/checklist/list",
        enabled: true,
      },
      {
        name: "예산 관리",
        shortName: "예산",
        path: "/budget/list",
        enabled: true,
      },
      {
        name: "AI 드레스",
        shortName: "AI 드레스",
        path: "/prep/ai-dress",
        enabled: true,
      },
      {
        name: "AI 견적서",
        shortName: "AI 견적서",
        path: "/prep/ai-quote",
        enabled: true,
      },
    ],
  },
];

const isItemActive = (pathname, item) => {
  if (!item.enabled) return false;

  if (item.path.endsWith("/list")) {
    return pathname.startsWith(item.path.replace("/list", ""));
  }

  return pathname.startsWith(item.path);
};

const PrepSidebar = () => {
  const location = useLocation();
  const mobileNavRef = useRef(null);

  useEffect(() => {
    const activeTab = mobileNavRef.current?.querySelector(
      '[data-active="true"]',
    );

    activeTab?.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
      inline: "center",
    });
  }, [location.pathname]);

  const items = MENU_GROUPS.flatMap((group) => group.items);

  return (
    <>
      {/* 모바일/태블릿: 본문을 빠르게 볼 수 있도록 한 줄 가로 스크롤 탭 */}
      <div className="sticky top-[64px] z-30 -mx-5 mb-1 border-y border-line bg-cream/95 px-5 py-3 shadow-[0_8px_18px_-16px_rgba(58,54,47,0.45)] backdrop-blur-md md:-mx-8 md:px-8 lg:hidden">
        <div className="relative">
          <nav
            ref={mobileNavRef}
            aria-label="준비관리 메뉴"
            className="flex snap-x snap-mandatory gap-2 overflow-x-auto pr-8 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
          >
            {items.map((item) => {
              const isActive = isItemActive(location.pathname, item);

              if (!item.enabled) {
                return (
                  <span
                    key={item.name}
                    title="준비 중인 기능입니다"
                    className="shrink-0 snap-start cursor-not-allowed select-none rounded-full border border-line bg-surface px-4 py-2 text-xs font-medium text-ink-faint"
                  >
                    {item.shortName}
                  </span>
                );
              }

              return (
                <Link
                  key={item.name}
                  to={item.path}
                  data-active={isActive}
                  aria-current={isActive ? "page" : undefined}
                  className={`shrink-0 snap-start rounded-full border px-4 py-2 text-xs font-semibold transition-all ${
                    isActive
                      ? "border-brand-deep bg-brand-deep text-white shadow-[0_5px_14px_-6px_rgba(192,96,128,0.75)]"
                      : "border-line bg-white text-ink-soft hover:border-brand hover:bg-blush-50"
                  }`}
                >
                  {item.shortName}
                </Link>
              );
            })}
          </nav>

          {/* 오른쪽에 메뉴가 더 있다는 시각적 힌트 */}
          <div className="pointer-events-none absolute inset-y-0 right-0 w-10 bg-gradient-to-l from-[#FBF7F0] to-transparent" />
        </div>
      </div>

      {/* PC: 기존 세로 사이드바 유지 */}
      <aside className="hidden w-60 shrink-0 py-8 pr-6 lg:block">
        {MENU_GROUPS.map((group) => (
          <div
            key={group.label}
            className="rounded-2xl bg-white p-4"
            style={{ boxShadow: "0 8px 24px -14px rgba(58,54,47,0.25)" }}
          >
            <p className="mb-3 px-3 text-xs font-bold tracking-wide text-brand-deep">
              {group.label}
            </p>

            <nav className="flex flex-col gap-1" aria-label={group.label}>
              {group.items.map((item) => {
                const isActive = isItemActive(location.pathname, item);

                if (!item.enabled) {
                  return (
                    <span
                      key={item.name}
                      title="준비 중인 기능입니다"
                      className="flex cursor-not-allowed select-none items-center gap-2 rounded-full px-4 py-2.5 text-sm text-ink-faint"
                    >
                      {item.name}
                    </span>
                  );
                }

                return (
                  <Link
                    key={item.name}
                    to={item.path}
                    aria-current={isActive ? "page" : undefined}
                    className={`flex items-center gap-2 rounded-full px-4 py-2.5 text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-brand-deep text-white shadow-[0_4px_12px_-4px_rgba(192,96,128,0.6)]"
                        : "text-ink hover:bg-blush-100"
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

export default PrepSidebar;
