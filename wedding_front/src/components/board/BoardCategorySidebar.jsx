const BoardCategorySidebar = ({
  title,
  categories,
  activeCategory,
  onSelect,
}) => {
  return (
    <aside className="w-52 shrink-0 py-8 pr-6">
      <p className="text-[11px] text-ink-faint tracking-wide mb-2 px-3">
        {title}
      </p>
      <nav className="flex flex-col gap-1">
        <span
          onClick={() => onSelect(null)}
          className={`flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm cursor-pointer transition-colors ${
            activeCategory === null
              ? "bg-brand-light text-brand-accent font-medium"
              : "text-ink-soft hover:bg-cream"
          }`}
        >
          전체글
        </span>
        {categories.map((c) => (
          <span
            key={c}
            onClick={() => onSelect(c)}
            className={`flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm cursor-pointer transition-colors ${
              activeCategory === c
                ? "bg-brand-light text-brand-accent font-medium"
                : "text-ink-soft hover:bg-cream"
            }`}
          >
            {c}
          </span>
        ))}
      </nav>
    </aside>
  );
};

export default BoardCategorySidebar;
