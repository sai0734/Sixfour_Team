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
  const [selectedRatingOption, setSelectedRatingOption] = useState(null);
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
    if (page === 1) return;
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
    <div className="min-h-screen bg-[#FBF7F0] text-[#3A362F]">
      {fetching ? <FetchingModal /> : null}

      <section className="bg-gradient-to-b from-[#EFE6D8] to-[#FBF7F0] px-5 py-10 text-center md:px-8 md:py-12 lg:px-[60px]">
        <div className="mx-auto max-w-[720px]">
          <span className="mb-4 inline-block -rotate-2 bg-[#E4C9B8] px-3.5 py-1 font-['Gaegu'] text-[13px] text-[#6B4A3A]">
            03 — GIFT SHOP
          </span>
          <h1 className="mb-3.5 font-['Gowun_Batang'] text-[28px] leading-snug text-[#3A362F] md:text-4xl">
            하객들에게 전하는 마음
          </h1>
          <p className="whitespace-pre-line text-[15px] leading-relaxed text-[#7A7364]">
            캔들, 디퓨저, 수건 세트까지{"\n"}취향대로 고르고 바로 주문할 수
            있어요
          </p>
        </div>
      </section>

      <div className="mx-auto grid max-w-[1200px] grid-cols-1 items-start gap-7 px-5 py-8 md:px-8 lg:grid-cols-[240px_1fr] lg:gap-10 lg:px-[60px] lg:py-12">
        <ProductFilterSidebarComponent
          categoryList={categoryList}
          selectedCategories={selectedCategories}
          onToggleCategory={handleToggleCategory}
          selectedPriceBand={selectedPriceBand}
          onTogglePriceBand={handleTogglePriceBand}
          selectedRatingOption={selectedRatingOption}
          onToggleRatingOption={handleToggleRatingOption}
        />

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
            <div className="flex flex-col items-center justify-center rounded-[18px] bg-[#FFFDF9] px-6 py-24 text-center shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
              <p className="mb-2 text-base text-[#3A362F]">
                <span className="font-['Gowun_Batang'] text-[#5C6B4F]">
                  "{keyword}"
                </span>{" "}
                와(과) 일치하는 항목이 없습니다.
              </p>
              <p className="mb-7 text-sm text-[#7A7364]">
                다른 검색어를 입력하거나 기본 목록으로 돌아가 보세요.
              </p>
              <button
                type="button"
                onClick={handleResetToDefaultList}
                className="h-10 rounded-full border border-[#D5D0C6] px-6 text-sm text-[#3A362F] transition hover:border-[#7C8B6F] hover:text-[#5C6B4F]"
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
