import { useEffect, useState } from "react";
import { getList } from "../../api/productApi";
import ShopTapeLabel from "./ShopTapeLabel";

const RelatedProductsComponent = ({
  currentPno,
  category,
  host,
  onClickProduct,
}) => {
  const [related, setRelated] = useState([]);

  useEffect(() => {
    if (!category) return;

    getList({
      page: 1,
      size: 8,
      categories: [category],
      sortType: "popular",
    }).then((data) => {
      const filtered = data.dtoList
        .filter((p) => p.pno !== currentPno)
        .slice(0, 4);
      setRelated(filtered);
    });
  }, [currentPno, category]);

  if (related.length === 0) return null;

  return (
    <div className="max-w-[1320px] mx-auto px-6 pb-16">
      <ShopTapeLabel tone="lavender" rotate={-1} className="mb-4">
        이런 상품은 어떠세요?
      </ShopTapeLabel>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4 md:gap-6">
        {related.map((product) => (
          <div
            key={product.pno}
            onClick={() => onClickProduct(product.pno)}
            className="cursor-pointer"
          >
            <div className="aspect-square rounded-2xl overflow-hidden bg-surface">
              <img
                alt={product.pname}
                className="w-full h-full object-cover"
                src={`${host}/api/product/view/s_${product.uploadFileNames?.[0]}`}
              />
            </div>
            <p className="text-sm mt-2.5 line-clamp-1">{product.pname}</p>
            <p className="text-sm font-medium mt-0.5">
              {product.price?.toLocaleString()}원
            </p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default RelatedProductsComponent;
