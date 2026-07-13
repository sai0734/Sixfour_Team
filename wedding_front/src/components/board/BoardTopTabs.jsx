import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";

const TABS = [
  { key: "ALL", label: "전체", to: "/board/list" },
  { key: "FREE", label: "자유게시판", to: "/board/free" },
  { key: "REVIEW", label: "후기게시판", to: "/board/review" },
  { key: "FAQ", label: "자주 묻는 질문", to: "/board/faq" },
  { key: "SENIOR", label: "선배 부부 매칭", to: "/board/senior" },
];

const BoardTopTabs = ({ active }) => {
  const activeRef = useRef(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "nearest",
      inline: "center",
    });
  }, [active]);

  return (
    <div className="sticky top-[68px] z-20 -mx-5 bg-[#FBF7F0]/95 px-5 py-3 backdrop-blur md:mx-0 md:bg-transparent md:px-0 md:py-0 md:backdrop-blur-0 lg:static">
      <nav className="flex gap-2 overflow-x-auto scroll-smooth border-b border-line pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden md:gap-7 md:pb-0">
        {TABS.map((tab) => {
          const isActive = active === tab.key;

          return (
            <Link
              key={tab.key}
              ref={isActive ? activeRef : null}
              to={tab.to}
              className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition-colors md:rounded-none md:px-0 md:pb-3 md:pt-0 md:border-b ${
                isActive
                  ? "bg-[#7C8B6F] text-white md:bg-transparent md:text-brand md:border-brand"
                  : "bg-white text-[#4A3F38] hover:bg-[#E6EBDD] md:bg-transparent md:text-ink-soft md:border-transparent md:hover:bg-transparent md:hover:text-ink"
              }`}
            >
              {tab.label}
            </Link>
          );
        })}
      </nav>
    </div>
  );
};

export default BoardTopTabs;
