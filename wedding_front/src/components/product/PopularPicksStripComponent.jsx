import { useEffect, useState } from "react";
import { getList } from "../../api/productApi";
import { API_SERVER_HOST } from "../../api/reservationApi";

const host = API_SERVER_HOST;

const ROTATIONS = ["-rotate-3", "rotate-2", "-rotate-1", "rotate-3"];

const PopularPicksStripComponent = ({ onClickCard }) => {
  const [picks, setPicks] = useState([]);

  useEffect(() => {
    getList({ page: 1, size: 4, sortType: "popular" }).then((data) => {
      setPicks(data.dtoList ?? []);
    });
  }, []);

  if (picks.length === 0) return null;

  return (
    <section className="mb-8 md:mb-10">
      <div className="mb-4 flex flex-wrap items-center gap-3 md:mb-5">
        <span className="inline-block -rotate-2 bg-blush-100 px-3.5 py-1 font-['Gaegu'] text-[13px] text-brand-deep">
          요즘 인기있는 답례품
        </span>
        <p className="text-xs text-ink-faint">
          하객들이 가장 많이 선택한 답례품이에요
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 md:gap-6">
        {picks.map((product, idx) => (
          <div
            key={product.pno}
            onClick={() => onClickCard(product.pno)}
            className={`cursor-pointer bg-white p-2.5 pb-8 shadow-[0_10px_28px_-10px_rgba(58,54,47,0.25)] transition duration-300 hover:-translate-y-1.5 hover:shadow-[0_18px_36px_-10px_rgba(58,54,47,0.3)] ${ROTATIONS[idx % ROTATIONS.length]}`}
          >
            <div className="aspect-square overflow-hidden bg-surface">
              <img
                alt={product.pname}
                className="h-full w-full object-cover"
                src={`${host}/api/product/view/s_${product.uploadFileNames?.[0]}`}
              />
            </div>
            <p className="mt-2.5 line-clamp-1 text-center font-['Gaegu'] text-[13px] text-ink-soft">
              {product.pname}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
};

export default PopularPicksStripComponent;
