const RATING_OPTIONS = [
  { label: "★★★★★", minRating: 5.0 },
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
    <aside>
      <div className="mb-7">
        <p className="text-sm font-medium mb-3">카테고리</p>
        <div className="flex flex-col gap-2.5 text-sm text-ink-soft">
          {categoryList.map((cat) => (
            <label
              key={cat}
              className="flex items-center gap-2 cursor-pointer hover:text-ink"
            >
              <input
                type="checkbox"
                checked={selectedCategories.includes(cat)}
                onChange={() => onToggleCategory(cat)}
                className="accent-brand"
              />
              {cat}
            </label>
          ))}
        </div>
      </div>

      <div className="mb-7">
        <p className="text-sm font-medium mb-3">가격대</p>
        <div className="flex flex-col gap-2.5 text-sm text-ink-soft">
          {PRICE_BANDS.map((band, idx) => (
            <label
              key={band.label}
              className="flex items-center gap-2 cursor-pointer hover:text-ink"
            >
              <input
                type="checkbox"
                checked={selectedPriceBand === idx}
                onChange={() => onTogglePriceBand(idx)}
                className="accent-brand"
              />
              {band.label}
            </label>
          ))}
        </div>
      </div>

      <div className="mb-7">
        <p className="text-sm font-medium mb-3">평점</p>
        <div className="flex flex-col gap-2.5 text-sm text-ink-soft">
          {RATING_OPTIONS.map((option, idx) => (
            <label
              key={option.label}
              className="flex items-center gap-2 cursor-pointer hover:text-ink"
            >
              <input
                type="checkbox"
                checked={selectedRatingOption === idx}
                onChange={() => onToggleRatingOption(idx)}
                className="accent-brand"
              />
              {option.label}
            </label>
          ))}
        </div>
      </div>
    </aside>
  );
};

export { PRICE_BANDS, RATING_OPTIONS };
export default ProductFilterSidebarComponent;
