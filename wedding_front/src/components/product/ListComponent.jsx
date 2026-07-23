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
import ShopTapeLabel from "./ShopTapeLabel";
import ProductFilterSidebarComponent, {
  PRICE_BANDS,
  RATING_OPTIONS,
} from "./ProductFilterSidebarComponent";
import ProductSortToolbarComponent from "./ProductSortToolbarComponent";
import ProductGridComponent from "./ProductGridComponent";
import PopularPicksStripComponent from "./PopularPicksStripComponent";
import { API_SERVER_HOST } from "../../api/reservationApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import { showAlert } from "../../util/globalAlert";

const host = API_SERVER_HOST;

const PRODUCT_LIST_PAGE_SIZE = 12;

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
  const { page, refresh, moveToRead, moveToList } = useCustomMove();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const pageSize = Number(searchParams.get("size")) || PRODUCT_LIST_PAGE_SIZE;

  const [serverData, setServerData] = useState(initState);
  const [categoryList, setCategoryList] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [selectedPriceBand, setSelectedPriceBand] = useState(null);
  const [selectedRatingOption, setSelectedRatingOption] = useState(null);
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [sortType, setSortType] = useState("popular");
  const [fetching, setFetching] = useState(false);
  const [wishedSet, setWishedSet] = useState(new Set());
  const [showMobileFilter, setShowMobileFilter] = useState(false);

  useEffect(() => {
    getCategories().then((data) => setCategoryList(data));
  }, []);

  useEffect(() => {
    const currentPage = searchParams.get("page");
    const currentSize = Number(searchParams.get("size"));

    if (!currentPage || currentSize !== PRODUCT_LIST_PAGE_SIZE) {
      navigate(
        {
          pathname: "/product/list",
          search: createSearchParams({
            page: currentPage || 1,
            size: PRODUCT_LIST_PAGE_SIZE,
          }).toString(),
        },
        { replace: true },
      );
    }
  }, [navigate, searchParams]);

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
      size: pageSize,
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
    pageSize,
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
    if (page === 1) return;
    moveToList(
      { page: 1, size: pageSize },
      { replace: true, preventScrollReset: true },
    );
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
  };

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
      showAlert("로그인이 필요한 기능입니다.");
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

  const activeFilterCount =
    selectedCategories.length +
    (selectedPriceBand !== null ? 1 : 0) +
    (selectedRatingOption !== null ? 1 : 0);

  return (
    <div className="-mx-5 -mb-10 min-h-[calc(100vh-6rem)] bg-cream px-5 text-ink">
      {fetching ? <FetchingModal /> : null}

      <section
        className="-mx-5 -mt-12 pb-10 pt-16 md:pb-12 text-center bg-cover bg-center relative"
        style={{ backgroundImage: "url('/shop-hero.jpg')" }}
      >
        <div className="absolute inset-0 bg-black/45" />

        <div className="relative z-10 mx-auto max-w-[720px] px-5">
          <ShopTapeLabel tone="white" className="mb-5">
            04 — GIFT SHOP
          </ShopTapeLabel>
          <h1 className="mb-2.5 font-['Gowun_Batang'] text-2xl leading-snug text-white md:mb-3.5 md:text-4xl">
            하객들에게 전하는 마음
          </h1>
          <p className="whitespace-pre-line text-sm leading-relaxed text-white/85 md:text-[15px]">
            캔들, 디퓨저, 수건 세트까지{"\n"}취향대로 고르고 바로 주문할 수
            있어요
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-[1200px] pt-6 md:pt-8">
        <PopularPicksStripComponent onClickCard={moveToRead} />
      </div>

      <div className="mx-auto grid max-w-[1200px] grid-cols-1 items-start gap-5 py-6 pb-16 lg:grid-cols-[240px_1fr] lg:gap-10 lg:py-12 lg:pb-20">
        <button
          type="button"
          onClick={() => setShowMobileFilter((v) => !v)}
          className="flex items-center justify-between rounded-full border border-line bg-white px-5 py-3 text-sm text-ink lg:hidden"
        >
          <span className="flex items-center gap-2">
            <svg
              viewBox="0 0 24 24"
              className="h-4 w-4"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            필터 / 골라보기
            {activeFilterCount > 0 && (
              <span className="rounded-full bg-brand px-2 py-0.5 text-[11px] text-white">
                {activeFilterCount}
              </span>
            )}
          </span>
          <span>{showMobileFilter ? "접기 ▲" : "펼치기 ▼"}</span>
        </button>

        <div className={`${showMobileFilter ? "block" : "hidden"} lg:block`}>
          <ProductFilterSidebarComponent
            categoryList={categoryList}
            selectedCategories={selectedCategories}
            onToggleCategory={handleToggleCategory}
            selectedPriceBand={selectedPriceBand}
            onTogglePriceBand={handleTogglePriceBand}
            selectedRatingOption={selectedRatingOption}
            onToggleRatingOption={handleToggleRatingOption}
          />
        </div>

        <section className="min-w-0">
          <ProductSortToolbarComponent
            totalCount={serverData.totalCount}
            sortType={sortType}
            onChangeSort={handleChangeSort}
            keywordInput={keywordInput}
            onChangeKeywordInput={setKeywordInput}
            onSearch={handleSearch}
          />

          {isEmptySearchResult ? (
            <div className="flex flex-col items-center justify-center rounded-[18px] bg-white px-6 py-16 text-center shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] md:py-24">
              <p className="mb-2 font-['Gaegu'] text-lg text-ink">
                <span className="text-brand-deep">"{keyword}"</span> 에 딱 맞는
                답례품을 아직 못 찾았어요 🤍
              </p>
              <p className="mb-7 text-sm text-ink-muted">
                다른 검색어를 입력하거나 기본 목록으로 돌아가 보세요.
              </p>
              <button
                type="button"
                onClick={handleResetToDefaultList}
                className="h-10 rounded-full border border-line px-6 text-sm text-ink transition hover:border-brand hover:text-brand-deep"
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
                    size: pageParam.size ?? pageSize,
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
