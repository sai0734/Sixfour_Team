import HorizontalDragScroll from "../common/HorizontalDragScroll";

// groups: [{ title, options: [{ value, label }], activeValue, onSelect,
//            resetLabel?, hideReset?, enhancedMobileScroll? }]
const BoardFilterSidebar = ({ groups }) => {
  const renderOptions = (group) => (
    <>
      {!group.hideReset && (
        <button
          type="button"
          onClick={() => group.onSelect(null)}
          className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2.5 text-sm font-medium transition-colors ${
            group.activeValue === null
              ? "bg-[#7C8B6F] text-white shadow-[0_4px_12px_-4px_rgba(92,107,79,0.55)]"
              : "bg-[#F7F2EA] text-[#4A3F38] hover:bg-[#E6EBDD] lg:bg-transparent"
          }`}
        >
          {group.resetLabel || "전체"}
        </button>
      )}

      {group.options.map((opt) => (
        <button
          type="button"
          key={opt.value}
          onClick={() => group.onSelect(opt.value)}
          className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2.5 text-sm font-medium transition-colors ${
            group.activeValue === opt.value
              ? "bg-[#7C8B6F] text-white shadow-[0_4px_12px_-4px_rgba(92,107,79,0.55)]"
              : "bg-[#F7F2EA] text-[#4A3F38] hover:bg-[#E6EBDD] lg:bg-transparent"
          }`}
        >
          {opt.label}
        </button>
      ))}
    </>
  );

  return (
    <aside className="flex w-full flex-col gap-3 py-3 lg:w-60 lg:shrink-0 lg:gap-4 lg:py-8 lg:pr-6">
      {groups.map((group) => (
        <div
          key={group.title}
          className="relative overflow-hidden rounded-2xl bg-white p-3 lg:p-4"
          style={{ boxShadow: "0 8px 24px -14px rgba(58,54,47,0.25)" }}
        >
          <p className="mb-2 px-1 text-xs font-bold tracking-wide text-[#7C8B6F] lg:mb-3 lg:px-3">
            {group.title}
          </p>

          {group.enhancedMobileScroll ? (
            <HorizontalDragScroll>{renderOptions(group)}</HorizontalDragScroll>
          ) : (
            <nav className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden lg:flex-col lg:gap-1 lg:overflow-visible lg:pb-0">
              {renderOptions(group)}
            </nav>
          )}

          {!group.enhancedMobileScroll && (
            <div className="pointer-events-none absolute bottom-0 right-0 top-0 w-8 bg-gradient-to-l from-white to-transparent lg:hidden" />
          )}
        </div>
      ))}
    </aside>
  );
};

export default BoardFilterSidebar;
