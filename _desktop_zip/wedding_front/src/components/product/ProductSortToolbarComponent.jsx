const SORT_OPTIONS = [
  { key: "popular", label: "인기순" },
  { key: "latest", label: "최신순" },
  { key: "priceAsc", label: "가격낮은순" },
  { key: "priceDesc", label: "가격높은순" },
  { key: "reviews", label: "리뷰많은순" },
];

const ProductSortToolbarComponent = ({
  totalCount,
  sortType,
  onChangeSort,
  keywordInput,
  onChangeKeywordInput,
  onSearch,
}) => {
  return (
    <div className="mb-7 flex flex-wrap items-start justify-between gap-5">
      <p className="pt-1.5 text-sm text-ink-muted">
        전체{" "}
        <b className="font-['Gowun_Batang'] font-bold text-ink">{totalCount}</b>
        개 상품
      </p>

      <div className="flex w-full min-w-[280px] flex-1 flex-col items-end gap-3.5">
        <div className="flex flex-wrap justify-end gap-2">
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.key}
              type="button"
              onClick={() => onChangeSort(opt.key)}
              className={`rounded-full border px-3.5 py-1.5 text-[13px] transition ${
                sortType === opt.key
                  ? "border-brand bg-brand text-white"
                  : "border-line bg-white text-ink-muted hover:border-brand hover:text-brand-deep"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <div className="flex w-full gap-2 sm:w-auto">
          <input
            type="text"
            value={keywordInput}
            onChange={(e) => onChangeKeywordInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && onSearch()}
            placeholder="상품명, 설명 검색"
            className="h-[38px] w-full flex-1 rounded-full border border-line bg-white px-4 text-[13px] text-ink outline-none focus:border-brand sm:w-[220px] sm:flex-none"
          />
          <button
            type="button"
            onClick={onSearch}
            className="h-[38px] shrink-0 rounded-full bg-brand px-[18px] text-[13px] text-white transition hover:bg-brand-dark"
          >
            검색
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductSortToolbarComponent;
