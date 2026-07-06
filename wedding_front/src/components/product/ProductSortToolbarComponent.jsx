const SORT_OPTIONS = [
  { key: "popular", label: "인기순" },
  { key: "latest", label: "최신순" },
  { key: "priceAsc", label: "가격낮은순" },
  { key: "priceDesc", label: "가격높은순" },
  { key: "reviews", label: "리뷰많은순" },
];

// 상품 목록 페이지의 상단 툴바 (전체개수 + 정렬탭 + 검색창)
const ProductSortToolbarComponent = ({
  totalCount,
  sortType,
  onChangeSort,
  keywordInput,
  onChangeKeywordInput,
  onSearch,
}) => {
  return (
    <div className="flex items-center justify-between mb-5 gap-4">
      <p className="text-ink-muted text-sm">
        전체 <b className="text-ink font-medium">{totalCount}</b>개 상품
      </p>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-4 text-sm text-ink-muted">
          {SORT_OPTIONS.map((opt) => (
            <span
              key={opt.key}
              onClick={() => onChangeSort(opt.key)}
              className={`cursor-pointer ${
                sortType === opt.key ? "text-ink font-medium" : ""
              }`}
            >
              {opt.label}
            </span>
          ))}
        </div>

        <div className="flex gap-2">
          <input
            type="text"
            value={keywordInput}
            onChange={(e) => onChangeKeywordInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && onSearch()}
            placeholder="상품명, 설명 검색"
            className="h-9 px-4 rounded-full border border-line text-sm w-56 focus:outline-none focus:border-brand"
          />
          <button
            onClick={onSearch}
            className="h-9 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
          >
            검색
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductSortToolbarComponent;
