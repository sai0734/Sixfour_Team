// groups: [{ title, options: string[], activeValue, onSelect, resetLabel? }]
// 그룹마다 "전체"류 리셋 옵션이 맨 위에 자동으로 붙음
const BoardFilterSidebar = ({ groups }) => {
  return (
    <aside className="w-52 shrink-0 py-8 pr-6">
      {groups.map((group) => (
        <div key={group.title} className="mb-7">
          <p className="text-[11px] text-ink-faint tracking-wide mb-2 px-3">
            {group.title}
          </p>
          <nav className="flex flex-col gap-1">
            <span
              onClick={() => group.onSelect(null)}
              className={`flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm cursor-pointer transition-colors ${
                group.activeValue === null
                  ? "bg-brand-light text-brand-accent font-medium"
                  : "text-ink-soft hover:bg-cream"
              }`}
            >
              {group.resetLabel || "전체"}
            </span>
            {group.options.map((opt) => (
              <span
                key={opt.value}
                onClick={() => group.onSelect(opt.value)}
                className={`flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm cursor-pointer transition-colors ${
                  group.activeValue === opt.value
                    ? "bg-brand-light text-brand-accent font-medium"
                    : "text-ink-soft hover:bg-cream"
                }`}
              >
                {opt.label}
              </span>
            ))}
          </nav>
        </div>
      ))}
    </aside>
  );
};

export default BoardFilterSidebar;
