import { useEffect, useState } from "react";
import { getList, getCategories } from "../../api/productApi";
import { postAdd, deleteWish, isWished } from "../../api/wishApi";
import useCustomMove from "../../hooks/useCustomMove";
import FetchingModal from "../common/FetchingModal";
import PageComponent from "../common/PageComponent";
import ProductFilterSidebarComponent, {
  PRICE_BANDS,
} from "./ProductFilterSidebarComponent";
import ProductSortToolbarComponent from "./ProductSortToolbarComponent";
import ProductGridComponent from "./ProductGridComponent";

import { API_SERVER_HOST } from "../../api/reservationApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const host = API_SERVER_HOST;

const initState = {
  dtoList: [],
  pageNumList: [],
  pageRequestDTO: null,
  prev: false,
  next: false,
  totalCount: 0,
  prevPage: 0,
  nextPage: 0,
  totalPage: 0,
  current: 0,
};

const ListComponent = () => {
  const { exceptionHandle, loginState } = useCustomLogin();
  const { size, refresh, moveToRead } = useCustomMove();

  const [serverData, setServerData] = useState(initState);
  const [categoryList, setCategoryList] = useState([]);

  const [selectedCategories, setSelectedCategories] = useState([]);
  const [selectedPriceBand, setSelectedPriceBand] = useState(null);
  const [ratingFilterOn, setRatingFilterOn] = useState(false);
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [localPage, setLocalPage] = useState(1);
  const [sortType, setSortType] = useState("popular");

  const [fetching, setFetching] = useState(false);

  const [wishedSet, setWishedSet] = useState(new Set());

  useEffect(() => {
    getCategories().then((data) => setCategoryList(data));
  }, []);

  useEffect(() => {
    setFetching(true);

    const priceBand =
      selectedPriceBand !== null ? PRICE_BANDS[selectedPriceBand] : null;

    getList({
      page: localPage,
      size,
      categories: selectedCategories,
      keyword,
      minPrice: priceBand?.minPrice,
      maxPrice: priceBand?.maxPrice,
      minRating: ratingFilterOn ? 4.0 : null,
      sortType,
    })
      .then((data) => {
        setServerData(data);
        setFetching(false);
      })
      .catch((err) => exceptionHandle(err));
  }, [
    localPage,
    size,
    selectedCategories,
    keyword,
    selectedPriceBand,
    ratingFilterOn,
    sortType,
    refresh,
  ]);

  useEffect(() => {
    if (!loginState.email || serverData.dtoList.length === 0) {
      return;
    }

    Promise.all(
      serverData.dtoList.map((product) =>
        isWished(product.pno).then((data) => ({
          pno: product.pno,
          wished: data.wished,
        })),
      ),
    ).then((results) => {
      const next = new Set();
      results.forEach((r) => {
        if (r.wished) next.add(r.pno);
      });
      setWishedSet(next);
    });
  }, [serverData.dtoList, loginState.email]);

  const handleToggleCategory = (cat) => {
    setLocalPage(1);
    setSelectedCategories((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat],
    );
  };

  const handleTogglePriceBand = (idx) => {
    setLocalPage(1);
    setSelectedPriceBand((prev) => (prev === idx ? null : idx));
  };

  const handleToggleRating = () => {
    setLocalPage(1);
    setRatingFilterOn((prev) => !prev);
  };

  const handleSearch = () => {
    setLocalPage(1);
    setKeyword(keywordInput);
  };

  const handleChangeSort = (key) => {
    setLocalPage(1);
    setSortType(key);
  };

  const handleClickWish = (e, pno) => {
    e.stopPropagation();

    if (!loginState.email) {
      alert("로그인이 필요한 기능입니다.");
      return;
    }

    const alreadyWished = wishedSet.has(pno);

    const action = alreadyWished ? deleteWish(pno) : postAdd(pno);

    action.then(() => {
      setWishedSet((prev) => {
        const next = new Set(prev);
        if (alreadyWished) {
          next.delete(pno);
        } else {
          next.add(pno);
        }
        return next;
      });
    });
  };

  return (
    <div className="bg-white">
      {fetching ? <FetchingModal /> : <></>}

      <section className="bg-brand-light py-14 px-6 text-center">
        <p className="text-xs tracking-[0.15em] text-brand-accent mb-2">
          GIFT SHOP
        </p>
        <p className="font-serif text-3xl text-brand-deep mb-2">
          하객들에게 전하는 마음
        </p>
        <p className="text-sm text-brand-accent">
          취향대로 고르고, 바로 주문할 수 있어요
        </p>
      </section>

      <div className="max-w-[1140px] mx-auto px-6 py-10 grid grid-cols-[200px_1fr] gap-10">
        <ProductFilterSidebarComponent
          categoryList={categoryList}
          selectedCategories={selectedCategories}
          onToggleCategory={handleToggleCategory}
          selectedPriceBand={selectedPriceBand}
          onTogglePriceBand={handleTogglePriceBand}
          ratingFilterOn={ratingFilterOn}
          onToggleRating={handleToggleRating}
        />

        <section>
          <ProductSortToolbarComponent
            totalCount={serverData.totalCount}
            sortType={sortType}
            onChangeSort={handleChangeSort}
            keywordInput={keywordInput}
            onChangeKeywordInput={setKeywordInput}
            onSearch={handleSearch}
          />

          <ProductGridComponent
            dtoList={serverData.dtoList}
            host={host}
            wishedSet={wishedSet}
            onClickWish={handleClickWish}
            onClickCard={moveToRead}
          />

          <PageComponent
            serverData={serverData}
            movePage={(pageParam) => setLocalPage(pageParam.page)}
          />
        </section>
      </div>
    </div>
  );
};

export default ListComponent;
