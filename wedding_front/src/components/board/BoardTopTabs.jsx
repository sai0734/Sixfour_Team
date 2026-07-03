import { Link } from "react-router-dom";

const TABS = [
  { key: "ALL", label: "전체", to: "/board/list" },
  { key: "FREE", label: "자유게시판", to: "/board/free" },
  { key: "REVIEW", label: "후기게시판", to: "/board/review" },
  { key: "FAQ", label: "자주 묻는 질문", to: null },
  { key: "SENIOR", label: "선배 부부 매칭", to: "/board/senior" },
];

// active: "ALL" | "FREE" | "REVIEW"
const BoardTopTabs = ({ active }) => {
  return (
    <nav className="flex gap-7 text-sm font-medium border-b border-line">
      {TABS.map((tab) => {
        if (!tab.to) {
          return (
            <span
              key={tab.key}
              title="준비 중인 기능입니다"
              className="pb-3 border-b border-transparent text-ink-faint cursor-not-allowed select-none"
            >
              {tab.label}
            </span>
          );
        }

        return (
          <Link
            key={tab.key}
            to={tab.to}
            className={`pb-3 border-b ${
              active === tab.key
                ? "text-brand border-brand"
                : "text-ink-soft border-transparent hover:text-ink"
            }`}
          >
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
};

export default BoardTopTabs;
