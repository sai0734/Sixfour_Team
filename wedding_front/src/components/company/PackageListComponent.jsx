import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getPackageList } from "../../api/packageApi";
import FetchingModal from "../common/FetchingModal";
import PageComponent from "../common/PageComponent";
import TapeLabel from "../common/TapeLabel";
import useCustomLogin from "../../hooks/useCustomLogin";
import useCustomMove from "../../hooks/useCustomMove";

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

const formatPrice = (price) => {
  if (price == null) return "-";
  return `${Number(price).toLocaleString()}원`;
};

const PackageListComponent = () => {
  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const [filters, setFilters] = useState({ keyword: "", sort: "latest" });
  const isFirstRender = useRef(true);

  const navigate = useNavigate();
  const location = useLocation();
  const { exceptionHandle } = useCustomLogin();
  const { page, size, refresh } = useCustomMove();

  const companyPathPrefix = location.pathname.startsWith("/admin/companies")
    ? "/admin/companies"
    : "/companies";

  const moveToPackageRead = (weddingPackageId) => {
    navigate({
      pathname: `${companyPathPrefix}/packages/read/${weddingPackageId}`,
      search: `page=${page}&size=${size}`,
    });
  };

  const moveToPackageList = (pageParam) => {
    const pageNum = pageParam?.page || page;
    const sizeNum = pageParam?.size || size;

    navigate({
      pathname: `${companyPathPrefix}/packages`,
      search: `page=${pageNum}&size=${sizeNum}`,
    });
  };

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    if (page !== 1) {
      navigate(
        { pathname: `${companyPathPrefix}/packages`, search: `page=1&size=${size}` },
        { replace: true },
      );
    }
  }, [filters.keyword]);

  useEffect(() => {
    setFetching(true);
    getPackageList({
      page,
      size,
      keyword: filters.keyword || undefined,
      sort: filters.sort,
    })
      .then((data) => {
        setServerData(data);
        setFetching(false);
      })
      .catch((err) => {
        setFetching(false);
        exceptionHandle(err);
      });
  }, [page, size, refresh, filters.keyword, filters.sort]);

  const SORT_OPTIONS = [
    { value: "latest", label: "최신순" },
    { value: "name", label: "이름순" },
    { value: "price", label: "가격순" },
  ];

  return (
    <section className="text-ink">
      {fetching ? <FetchingModal /> : null}

      <div className="mb-6 rounded-2xl bg-gradient-to-b from-brand-light to-white px-4 py-6 text-center">
        <TapeLabel className="mb-3">WEDDING PACKAGE</TapeLabel>
        <h1 className="mt-4 font-['Gowun_Batang'] text-xl sm:text-2xl text-ink">
          패키지 리스트
        </h1>
        <p className="mt-1.5 text-xs sm:text-sm text-ink-muted">
          홀·드레스·스튜디오·메이크업이 묶인 패키지를 한눈에 비교해보세요
        </p>
      </div>

      <div className="mb-4">
        <button
          type="button"
          onClick={() =>
            navigate({
              pathname: `${companyPathPrefix}/list`,
              search: `page=${page}&size=${size}`,
            })
          }
          className="rounded-full border border-line px-3.5 py-1.5 text-[13px] text-ink-muted transition hover:border-brand hover:text-brand-deep"
        >
          ← 업체 목록
        </button>
        <button
          type="button"
          className="ml-1.5 rounded-full border border-brand bg-brand px-3.5 py-1.5 text-[13px] text-white"
        >
          패키지
        </button>
      </div>

      <div className="mb-6 space-y-3">
        <div className="flex gap-2">
          <input
            className="h-[38px] flex-1 rounded-full border border-line px-4 text-[13px] text-ink outline-none transition focus:border-brand"
            type="text"
            placeholder="패키지명으로 검색"
            value={filters.keyword}
            onChange={(e) =>
              setFilters((prev) => ({ ...prev, keyword: e.target.value }))
            }
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                setFilters((prev) => ({ ...prev, keyword: e.target.value }));
              }
            }}
          />
          <button
            type="button"
            className="h-[38px] shrink-0 rounded-full bg-brand px-[18px] text-[13px] text-white transition hover:bg-brand-dark"
            onClick={() => setFilters((prev) => ({ ...prev }))}
          >
            검색
          </button>
        </div>

        <div className="flex flex-wrap gap-1.5">
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => setFilters((prev) => ({ ...prev, sort: opt.value }))}
              className={`rounded-full border px-3.5 py-1.5 text-[13px] transition ${
                filters.sort === opt.value
                  ? "border-brand bg-brand text-white"
                  : "border-line text-ink-muted hover:border-brand hover:text-brand-deep"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {serverData.dtoList.length === 0 ? (
        <div className="rounded-2xl border border-line bg-white py-12 text-center text-sm text-ink-muted">
          {filters.keyword
            ? `"${filters.keyword}" 에 해당하는 패키지가 없습니다.`
            : "등록된 패키지가 없습니다."}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {serverData.dtoList.map((pkg) => (
            <div
              key={pkg.weddingPackageId}
              onClick={() => moveToPackageRead(pkg.weddingPackageId)}
              className="cursor-pointer rounded-2xl border border-line bg-white p-4 transition hover:border-brand hover:shadow-sm"
            >
              <div className="mb-3 aspect-[4/3] overflow-hidden rounded-xl bg-blush-100">
                {pkg.thumbnail ? (
                  <img
                    src={pkg.thumbnail}
                    alt={pkg.name}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full items-center justify-center text-xs text-ink-muted">
                    이미지 없음
                  </div>
                )}
              </div>

              <p className="font-['Gowun_Batang'] text-base text-ink">{pkg.name}</p>
              <p className="mt-1 line-clamp-2 text-xs text-ink-muted">
                {pkg.description || "패키지 설명이 없습니다."}
              </p>
              <div className="mt-3 flex items-end justify-between">
                <div>
                  <p className="text-xs text-ink-muted">구성 {pkg.itemCount}개</p>
                  <p className="mt-0.5 text-sm font-semibold text-brand">
                    {formatPrice(pkg.salePrice ?? pkg.totalPrice)}
                  </p>
                </div>
                <span className="text-xs text-ink-muted">상세 보기 →</span>
              </div>
            </div>
          ))}
        </div>
      )}

      <PageComponent serverData={serverData} movePage={moveToPackageList} />
    </section>
  );
};

export default PackageListComponent;
