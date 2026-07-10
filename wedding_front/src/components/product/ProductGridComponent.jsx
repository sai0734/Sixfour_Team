const CATEGORY_TAG = {
  타월: "혼주님께 딱이에요",
  "식기/머그": "부모님께 감사한 마음을 담아",
  "디퓨저/향수": "친한 친구에게",
  "비누/핸드워시": "정성스러운 첫인사로",
  "과자/한과": "정겨운 마음을 담아",
  "차/커피": "직장 동료에게",
  "곡물/식품": "건강을 기원하며",
  "생활/건강": "소소한 진심을 담아",
};

const ProductGridComponent = ({
  dtoList,
  host,
  wishedSet,
  onClickWish,
  onClickCard,
}) => {
  return (
    <div className="grid grid-cols-2 gap-3.5 md:grid-cols-3 md:gap-5 lg:grid-cols-4 lg:gap-[22px]">
      {dtoList.map((product) => (
        <article
          key={product.pno}
          onClick={() => onClickCard(product.pno)}
          className="cursor-pointer overflow-hidden rounded-[18px] bg-white shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] transition duration-250 hover:-translate-y-1.5 hover:shadow-[0_18px_40px_-12px_rgba(58,54,47,0.2)]"
        >
          <div className="relative aspect-square overflow-hidden bg-surface">
            <img
              alt={product.pname}
              className="h-full w-full object-cover transition duration-300 hover:scale-[1.04]"
              src={`${host}/api/product/view/s_${product.uploadFileNames?.[0]}`}
            />

            {CATEGORY_TAG[product.category] && (
              <span className="absolute bottom-0 left-0 right-0 bg-brand-deep/90 px-3 py-1.5 text-center font-['Gaegu'] text-[15px] font-medium text-white shadow-[0_-2px_8px_rgba(58,54,47,0.15)]">
                {CATEGORY_TAG[product.category]}
              </span>
            )}

            <button
              type="button"
              onClick={(e) => onClickWish(e, product.pno)}
              aria-label="찜하기"
              className="absolute right-2.5 top-2.5 flex h-[34px] w-[34px] items-center justify-center rounded-full bg-white/90 shadow-[0_4px_12px_rgba(58,54,47,0.12)] transition hover:scale-105"
            >
              <svg
                viewBox="0 0 24 24"
                className="h-4 w-4"
                fill={wishedSet.has(product.pno) ? "#C87070" : "none"}
                stroke="#C87070"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M19.5 12.572 12 20l-7.5-7.428a5 5 0 1 1 7.5-6.566 5 5 0 1 1 7.5 6.566Z" />
              </svg>
            </button>
          </div>

          <div className="px-4 pb-[18px] pt-4">
            <p className="mb-1.5 line-clamp-2 text-sm leading-snug text-ink">
              {product.pname}
            </p>
            <p className="mb-1 font-['Gowun_Batang'] text-[15px] text-ink">
              {product.price?.toLocaleString()}원
            </p>
            <p className="flex items-center gap-1 text-xs text-ink-faint">
              <span className="text-[#C9A96A]">★</span>
              {product.ratingAvg?.toFixed(1)} ({product.reviewCount})
            </p>
          </div>
        </article>
      ))}
    </div>
  );
};

export default ProductGridComponent;
