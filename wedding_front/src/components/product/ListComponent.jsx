import { useEffect, useState } from "react";
import {
  createSearchParams,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { getList, getCategories } from "../../api/productApi";
import { postAdd, deleteWish, isWished } from "../../api/wishApi";
import useCustomMove from "../../hooks/useCustomMove";
import FetchingModal from "../common/FetchingModal";
import PageComponent from "../common/PageComponent";
import ProductFilterSidebarComponent, {
  PRICE_BANDS,
  RATING_OPTIONS,
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

  const { page, size, refresh, moveToRead, moveToList } = useCustomMove();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [serverData, setServerData] = useState(initState);
  const [categoryList, setCategoryList] = useState([]);

  const [selectedCategories, setSelectedCategories] = useState([]);
  const [selectedPriceBand, setSelectedPriceBand] = useState(null);
  const [selectedRatingOption, setSelectedRatingOption] = useState(null); // [추가]
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");

  const [sortType, setSortType] = useState("popular");

  const [fetching, setFetching] = useState(false);

  const [wishedSet, setWishedSet] = useState(new Set());

  useEffect(() => {
    getCategories().then((data) => setCategoryList(data));
  }, []);

  useEffect(() => {
    if (!searchParams.get("page")) {
      navigate(
        {
          pathname: "/product/list",
          search: createSearchParams({ page: 1, size }).toString(),
        },
        { replace: true },
      );
    }
  }, [navigate, searchParams, size]);

  useEffect(() => {
    setFetching(true);

    const priceBand =
      selectedPriceBand !== null ? PRICE_BANDS[selectedPriceBand] : null;

    const ratingOption =
      selectedRatingOption !== null
        ? RATING_OPTIONS[selectedRatingOption]
        : null;

    getList({
      page,
      size,
      categories: selectedCategories,
      keyword,
      minPrice: priceBand?.minPrice,
      maxPrice: priceBand?.maxPrice,
      minRating: ratingOption?.minRating ?? null,
      sortType,
    })
      .then((data) => {
        setServerData(data);
        setFetching(false);
      })
      .catch((err) => exceptionHandle(err));
  }, [
    page,
    size,
    selectedCategories,
    keyword,
    selectedPriceBand,
    selectedRatingOption,
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

  const resetPageForFilter = () => {
    if (page === 1) {
      return;
    }

    moveToList({ page: 1, size }, { replace: true, preventScrollReset: true });
  };

  const handleResetToDefaultList = () => {
    setKeywordInput("");
    setKeyword("");
    setSelectedCategories([]);
    setSelectedPriceBand(null);
    setSelectedRatingOption(null);
    setSortType("popular");
    resetPageForFilter();
  };

  const isEmptySearchResult =
    !fetching && keyword.trim() !== "" && serverData.dtoList.length === 0;

  const handleToggleCategory = (cat) => {
    resetPageForFilter();
    setSelectedCategories((prev) =>
      prev.includes(cat) ? prev.filter((c) => c !== cat) : [...prev, cat],
    );
  };

  const handleTogglePriceBand = (idx) => {
    resetPageForFilter();
    setSelectedPriceBand((prev) => (prev === idx ? null : idx));
  };

  const handleToggleRatingOption = (idx) => {
    resetPageForFilter();
    setSelectedRatingOption((prev) => (prev === idx ? null : idx));
  }; // [추가]

  const handleSearch = () => {
    resetPageForFilter();
    setKeyword(keywordInput);
  };

  const handleChangeSort = (key) => {
    resetPageForFilter();
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
          selectedRatingOption={selectedRatingOption}
          onToggleRatingOption={handleToggleRatingOption}
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

          {isEmptySearchResult ? (
            <div className="flex flex-col items-center justify-center py-24 text-center">
              <p className="text-base text-ink mb-2">
                <span className="font-medium text-brand">"{keyword}"</span>
                와(과) 일치하는 항목이 없습니다.
              </p>
              <p className="text-sm text-ink-muted mb-8">
                다른 검색어를 입력하거나 기본 목록으로 돌아가 보세요.
              </p>
              <button
                type="button"
                onClick={handleResetToDefaultList}
                className="h-10 px-6 rounded-full border border-line text-sm text-ink hover:border-brand hover:text-brand transition-colors"
              >
                기본 목록 가기
              </button>
            </div>
          ) : (
            <>
              <ProductGridComponent
                dtoList={serverData.dtoList}
                host={host}
                wishedSet={wishedSet}
                onClickWish={handleClickWish}
                onClickCard={moveToRead}
              />

              <PageComponent
                serverData={serverData}
                movePage={(pageParam) =>
                  moveToList({
                    page: pageParam.page,
                    size: pageParam.size ?? size,
                  })
                }
              />
            </>
          )}
        </section>
      </div>
    </div>
  );
};

export default ListComponent;
