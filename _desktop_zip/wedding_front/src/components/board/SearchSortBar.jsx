const SearchSortBar = ({ keyword, onKeywordChange, sort, onSortChange }) => {
  return (
    <div className="flex items-center gap-3 mb-5">
      <input
        type="text"
        placeholder="제목 또는 내용으로 검색"
        value={keyword}
        onChange={(e) => onKeywordChange(e.target.value)}
        className="flex-1 h-10 px-4 rounded-full border border-line-soft text-sm focus:outline-none focus:border-brand"
      />

      <div className="flex items-center gap-1 bg-surface rounded-full p-1 shrink-0">
        <button
          type="button"
          onClick={() => onSortChange("recent")}
          className={`h-8 px-4 rounded-full text-xs font-medium transition-colors ${
            sort === "recent"
              ? "bg-white text-brand shadow-sm"
              : "text-ink-muted"
          }`}
        >
          최신순
        </button>
        <button
          type="button"
          onClick={() => onSortChange("popular")}
          className={`h-8 px-4 rounded-full text-xs font-medium transition-colors ${
            sort === "popular"
              ? "bg-white text-brand shadow-sm"
              : "text-ink-muted"
          }`}
        >
          인기순
        </button>
      </div>
    </div>
  );
};

export default SearchSortBar;
