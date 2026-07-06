// 상품 카드 그리드 + 찜(하트) 토글
const ProductGridComponent = ({
  dtoList,
  host,
  wishedSet,
  onClickWish,
  onClickCard,
}) => {
  return (
    <div className="grid grid-cols-4 gap-6">
      {dtoList.map((product) => (
        <div
          key={product.pno}
          className="cursor-pointer"
          onClick={() => onClickCard(product.pno)}
        >
          <div className="relative aspect-square rounded-2xl overflow-hidden bg-surface">
            <img
              alt={product.pname}
              className="w-full h-full object-cover"
              src={`${host}/api/product/view/s_${product.uploadFileNames?.[0]}`}
            />

            <button
              onClick={(e) => onClickWish(e, product.pno)}
              className="absolute top-2.5 right-2.5 w-7 h-7 rounded-full bg-white/90 flex items-center justify-center"
            >
              <svg
                viewBox="0 0 24 24"
                className="w-3.5 h-3.5"
                fill={wishedSet.has(product.pno) ? "#D4537E" : "none"}
                stroke="#D4537E"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M19.5 12.572 12 20l-7.5-7.428a5 5 0 1 1 7.5-6.566 5 5 0 1 1 7.5 6.566Z" />
              </svg>
            </button>
          </div>
          <p className="text-sm mt-2.5">{product.pname}</p>
          <p className="text-sm font-medium mt-0.5">
            {product.price?.toLocaleString()}원
          </p>
          <p className="text-xs text-ink-faint flex items-center gap-1 mt-0.5">
            ★ {product.ratingAvg?.toFixed(1)} ({product.reviewCount})
          </p>
        </div>
      ))}
    </div>
  );
};

export default ProductGridComponent;
