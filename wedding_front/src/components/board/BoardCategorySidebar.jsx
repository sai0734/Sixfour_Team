const BoardCategorySidebar = ({
  title,
  categories,
  activeCategory,
  onSelect,
}) => {
  return (
    <aside className="sticky top-[126px] z-10 w-full bg-[#FBF7F0]/95 py-3 backdrop-blur lg:static lg:w-52 lg:shrink-0 lg:bg-transparent lg:py-8 lg:pr-6 lg:backdrop-blur-0">
      <div className="relative overflow-hidden rounded-2xl bg-white p-3 lg:rounded-none lg:bg-transparent lg:p-0">
        <p className="hidden px-3 text-[11px] tracking-wide text-ink-faint lg:mb-2 lg:block">
          {title}
        </p>

        <nav className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden lg:flex-col lg:gap-1 lg:overflow-visible lg:pb-0">
          <button
            type="button"
            onClick={() => onSelect(null)}
            className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2.5 text-sm font-medium transition-colors lg:flex lg:items-center lg:gap-2 lg:rounded-lg lg:px-3 ${
              activeCategory === null
                ? "bg-[#7C8B6F] text-white lg:bg-brand-light lg:text-brand-accent"
                : "bg-[#F7F2EA] text-[#4A3F38] hover:bg-[#E6EBDD] lg:bg-transparent lg:text-ink-soft lg:hover:bg-cream"
            }`}
          >
            전체글
          </button>

          {categories.map((category) => (
            <button
              type="button"
              key={category}
              onClick={() => onSelect(category)}
              className={`shrink-0 whitespace-nowrap rounded-full px-4 py-2.5 text-sm font-medium transition-colors lg:flex lg:items-center lg:gap-2 lg:rounded-lg lg:px-3 ${
                activeCategory === category
                  ? "bg-[#7C8B6F] text-white lg:bg-brand-light lg:text-brand-accent"
                  : "bg-[#F7F2EA] text-[#4A3F38] hover:bg-[#E6EBDD] lg:bg-transparent lg:text-ink-soft lg:hover:bg-cream"
              }`}
            >
              {category}
            </button>
          ))}
        </nav>

        <div className="pointer-events-none absolute bottom-0 right-0 top-0 w-8 bg-gradient-to-l from-white to-transparent lg:hidden" />
      </div>
    </aside>
  );
};

export default BoardCategorySidebar;
