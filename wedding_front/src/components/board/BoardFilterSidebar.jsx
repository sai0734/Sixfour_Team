// groups: [{ title, options: string[], activeValue, onSelect, resetLabel?, hideReset? }]
// 그룹마다 "전체"류 리셋 옵션이 맨 위에 자동으로 붙음 (hideReset: true면 생략)
const BoardFilterSidebar = ({ groups }) => {
  return (
    <aside className="flex w-full flex-col gap-4 py-4 lg:w-60 lg:shrink-0 lg:py-8 lg:pr-6">
      {groups.map((group) => (
        <div
          key={group.title}
          className="rounded-2xl bg-white p-4"
          style={{ boxShadow: "0 8px 24px -14px rgba(58,54,47,0.25)" }}
        >
          <p
            className="mb-3 px-3 text-xs font-bold tracking-wide"
            style={{ color: "#C06080" }}
          >
            {group.title}
          </p>
          <nav className="flex flex-col gap-1">
            {!group.hideReset && (
              <span
                onClick={() => group.onSelect(null)}
                className="flex cursor-pointer items-center gap-2 rounded-full px-4 py-2.5 text-sm font-medium transition-colors"
                style={
                  group.activeValue === null
                    ? {
                        background: "#C06080",
                        color: "#FFFFFF",
                        boxShadow: "0 4px 12px -4px rgba(192,96,128,0.6)",
                      }
                    : { color: "#4A3F38" }
                }
                onMouseEnter={(e) => {
                  if (group.activeValue !== null)
                    e.currentTarget.style.background = "#FFE2E2";
                }}
                onMouseLeave={(e) => {
                  if (group.activeValue !== null)
                    e.currentTarget.style.background = "transparent";
                }}
              >
                {group.resetLabel || "전체"}
              </span>
            )}
            {group.options.map((opt) => (
              <span
                key={opt.value}
                onClick={() => group.onSelect(opt.value)}
                className="flex cursor-pointer items-center gap-2 rounded-full px-4 py-2.5 text-sm font-medium transition-colors"
                style={
                  group.activeValue === opt.value
                    ? {
                        background: "#C06080",
                        color: "#FFFFFF",
                        boxShadow: "0 4px 12px -4px rgba(192,96,128,0.6)",
                      }
                    : { color: "#4A3F38" }
                }
                onMouseEnter={(e) => {
                  if (group.activeValue !== opt.value)
                    e.currentTarget.style.background = "#FFE2E2";
                }}
                onMouseLeave={(e) => {
                  if (group.activeValue !== opt.value)
                    e.currentTarget.style.background = "transparent";
                }}
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
