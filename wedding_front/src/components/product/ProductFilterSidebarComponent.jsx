const RATING_OPTIONS = [
  { label: "★★★★☆ 이상", minRating: 4.0 },
  { label: "★★★☆☆ 이상", minRating: 3.0 },
  { label: "★★☆☆☆ 이상", minRating: 2.0 },
  { label: "★☆☆☆☆ 이상", minRating: 1.0 },
];

const PRICE_BANDS = [
  { label: "1만원 이하", minPrice: null, maxPrice: 10000 },
  { label: "1~3만원", minPrice: 10000, maxPrice: 30000 },
  { label: "3만원 이상", minPrice: 30000, maxPrice: null },
];

const ProductFilterSidebarComponent = ({
  categoryList,
  selectedCategories,
  onToggleCategory,
  selectedPriceBand,
  onTogglePriceBand,
  selectedRatingOption,
  onToggleRatingOption,
}) => {
  return (
    <aside className="mt-3 rounded-[18px] bg-white p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] lg:sticky lg:top-[100px] lg:mt-0 lg:p-6">
      <div className="mb-7">
        <span className="mb-3 inline-block -rotate-2 bg-lavender-light px-3.5 py-1 font-['Gaegu'] text-[13px] text-lavender-dark">
          필터
        </span>
        <h2 className="font-['Gowun_Batang'] text-[22px] text-ink">골라보기</h2>
      </div>

      <div className="mb-6">
        <p className="mb-3 text-[13px] font-medium text-brand-deep">카테고리</p>
        <div className="flex flex-col gap-2.5">
          {categoryList.map((cat) => (
            <label
              key={cat}
              className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink-muted transition hover:text-ink"
            >
              <input
                type="checkbox"
                checked={selectedCategories.includes(cat)}
                onChange={() => onToggleCategory(cat)}
                className="h-[15px] w-[15px] cursor-pointer accent-brand"
              />
              <span>{cat}</span>
            </label>
          ))}
        </div>
      </div>

      <div className="mb-6">
        <p className="mb-3 text-[13px] font-medium text-brand-deep">가격대</p>
        <div className="flex flex-col gap-2.5">
          {PRICE_BANDS.map((band, idx) => (
            <label
              key={band.label}
              className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink-muted transition hover:text-ink"
            >
              <input
                type="checkbox"
                checked={selectedPriceBand === idx}
                onChange={() => onTogglePriceBand(idx)}
                className="h-[15px] w-[15px] cursor-pointer accent-brand"
              />
              <span>{band.label}</span>
            </label>
          ))}
        </div>
      </div>

      <div>
        <p className="mb-3 text-[13px] font-medium text-brand-deep">평점</p>
        <div className="flex flex-col gap-2.5">
          {RATING_OPTIONS.map((option, idx) => (
            <label
              key={option.label}
              className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink-muted transition hover:text-ink"
            >
              <input
                type="checkbox"
                checked={selectedRatingOption === idx}
                onChange={() => onToggleRatingOption(idx)}
                className="h-[15px] w-[15px] cursor-pointer accent-brand"
              />
              <span>{option.label}</span>
            </label>
          ))}
        </div>
      </div>
    </aside>
  );
};

export { PRICE_BANDS, RATING_OPTIONS };
export default ProductFilterSidebarComponent;
