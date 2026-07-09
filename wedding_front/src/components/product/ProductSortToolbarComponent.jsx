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
      <p className="pt-1.5 text-sm text-[#7A7364]">
        전체{" "}
        <b className="font-['Gowun_Batang'] font-bold text-[#3A362F]">
          {totalCount}
        </b>
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
                  ? "border-[#7C8B6F] bg-[#7C8B6F] text-[#FBF7F0]"
                  : "border-[#E0D8CC] bg-[#FFFDF9] text-[#7A7364] hover:border-[#7C8B6F] hover:text-[#5C6B4F]"
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
            className="h-[38px] w-full flex-1 rounded-full border border-[#E0D8CC] bg-[#FFFDF9] px-4 text-[13px] text-[#3A362F] outline-none focus:border-[#7C8B6F] sm:w-[220px] sm:flex-none"
          />
          <button
            type="button"
            onClick={onSearch}
            className="h-[38px] shrink-0 rounded-full bg-[#7C8B6F] px-[18px] text-[13px] text-[#FBF7F0] transition hover:bg-[#5C6B4F]"
          >
            검색
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductSortToolbarComponent;
