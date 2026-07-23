import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { deleteOne, getCompanyImageUrl, getList } from "../../api/companyApi";
import FetchingModal from "../common/FetchingModal";
import PageComponent from "../common/PageComponent";
import useCustomLogin from "../../hooks/useCustomLogin";
import useCustomMove from "../../hooks/useCustomMove";
import ShopTapeLabel from "../product/ShopTapeLabel";
import { showConfirm } from "../../util/globalConfirm";

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

const categoryLabel = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

const categoryBadge = {
  HALL: "bg-emerald-50 text-emerald-700 border-emerald-200",
  DRESS: "bg-pink-50 text-pink-700 border-pink-200",
  MAKEUP: "bg-amber-50 text-amber-700 border-amber-200",
  STUDIO: "bg-blue-50 text-blue-700 border-blue-200",
};

const adminRoles = ["ADMIN", "ROLE_ADMIN"];

const CompanyListComponent = () => {
  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const { exceptionHandle } = useCustomLogin();
  const { page, size, refresh } = useCustomMove();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const loginState = useSelector((state) => state.loginSlice);
  const canManageCompany = loginState.roleNames?.some((roleName) =>
    adminRoles.includes(roleName),
  );
  // 관리자 경로에서 들어온 경우 등록/수정 이동도 /admin/companies 하위로 유지합니다.
  const companyPathPrefix = location.pathname.startsWith("/admin/companies")
    ? "/admin/companies"
    : "/companies";

  // ★ 필터는 URL 쿼리가 기준 (상세→뒤로가기 시에도 유지)
  const filters = {
    keyword: searchParams.get("keyword") || "",
    category: searchParams.get("category") || "",
    sort: searchParams.get("sort") || "latest",
  };
  const [keywordInput, setKeywordInput] = useState(filters.keyword);

  useEffect(() => {
    setKeywordInput(filters.keyword);
  }, [filters.keyword]);

  const buildListSearch = ({
    page: pageNum = page,
    size: sizeNum = size,
    keyword = filters.keyword,
    category = filters.category,
    sort = filters.sort,
  } = {}) => {
    const params = new URLSearchParams();
    params.set("page", String(pageNum));
    params.set("size", String(sizeNum));
    if (keyword) params.set("keyword", keyword);
    if (category) params.set("category", category);
    if (sort) params.set("sort", sort);
    return params.toString();
  };

  const applyListFilters = (patch = {}) => {
    navigate({
      pathname: `${companyPathPrefix}/list`,
      search: buildListSearch({ page: 1, ...patch }),
    });
  };

  const moveToCompanyRead = (cmno) => {
    navigate({
      pathname: `${companyPathPrefix}/read/${cmno}`,
      // ★ page/size + 필터를 상세 URL에도 전달
      search: buildListSearch(),
    });
  };

  const moveToCompanyList = (pageParam) => {
    navigate({
      pathname: `${companyPathPrefix}/list`,
      // ★ 페이지 이동 시에도 필터 유지
      search: buildListSearch({
        page: pageParam?.page || page,
        size: pageParam?.size || size,
      }),
    });
  };

  useEffect(() => {
    setFetching(true);
    getList({
      page,
      size,
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
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
  }, [
    page,
    size,
    refresh,
    filters.keyword,
    filters.category,
    filters.sort,
    reloadKey,
  ]);

  const stats = useMemo(() => {
    const total = serverData.totalCount || serverData.dtoList.length;
    const active =
      serverData.dtoList.filter((company) => !company.delFlag).length || total;
    return {
      total,
      active,
      pending: 0,
      newThisMonth: Math.min(serverData.dtoList.length, 14),
    };
  }, [serverData]);

  const handleDelete = async (cmno, name) => {
    if (!(await showConfirm(`${name} 업체를 삭제하시겠습니까?`))) {
      return;
    }

    try {
      setFetching(true);
      await deleteOne(cmno);
      setReloadKey((prev) => prev + 1);
    } catch (err) {
      exceptionHandle(err);
    } finally {
      setFetching(false);
    }
  };

  const CATEGORY_OPTIONS = [
    { value: "", label: "전체" },
    { value: "HALL", label: "웨딩홀" },
    { value: "DRESS", label: "드레스" },
    { value: "MAKEUP", label: "메이크업" },
    { value: "STUDIO", label: "스튜디오" },
  ];

  const SORT_OPTIONS = [
    { value: "latest", label: "최신순" },
    { value: "name", label: "이름순" },
    { value: "price", label: "가격순" },
  ];

  return (
    <section className="text-ink">
      {fetching ? <FetchingModal /> : null}

      {!canManageCompany && (
        /* ── 배너: 상단 고정 메뉴에 붙이고 좌우 full-bleed / 아래 리스트는 원래 폭 ── */
        <section
          className="relative mb-6 bg-cover bg-center pb-10 pt-24 text-center md:pb-12 sm:pt-28 -mt-20 sm:-mt-28"
          style={{
            backgroundImage: "url('/company-hero.png')",
            width: "100vw",
            maxWidth: "100vw",
            marginLeft: "calc(50% - 50vw)",
          }}
        >
          <div className="absolute inset-0 bg-black/45" />

          <div className="relative z-10 mx-auto max-w-[720px] px-5">
            <ShopTapeLabel tone="white" className="mb-5">
              02 — WEDDING COMPANY
            </ShopTapeLabel>
            <h1 className="mb-2.5 font-['Gowun_Batang'] text-2xl leading-snug text-white md:mb-3.5 md:text-4xl">
              우리 결혼식에 어울리는 업체
            </h1>
            <p className="whitespace-pre-line text-sm leading-relaxed text-white/85 md:text-[15px]">
              웨딩홀부터 드레스, 메이크업, 스튜디오까지{"\n"}
              원하는 조건으로 찾아보세요
            </p>
          </div>
        </section>
      )}

      {/* 답례품(max-w-[1200px])·준비관리와 맞춘 본문 가로폭 */}
      <div className="mx-auto w-full max-w-[1200px]">
      {/* ── 헤더: 관리자 뷰 (상품관리 스타일) ── */}
      {canManageCompany ? (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <ShopTapeLabel className="mb-2.5">관리자</ShopTapeLabel>
            <p className="mt-4 font-['Gowun_Batang'] text-2xl text-ink">업체 관리</p>
          </div>
          <button
            type="button"
            className="h-10 rounded-full bg-brand px-5 text-sm font-medium text-white transition hover:bg-brand-dark"
            onClick={() => navigate({ pathname: `${companyPathPrefix}/add` })}
          >
            + 업체 등록
          </button>
        </div>
      ) : null}

      {/* ── 통계 카드 (관리자) 대시보드 페이지랑 겹치는거 같아서 우선 빼는걸로 수정함 ──
      {canManageCompany && (
        <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatCard label="전체 업체" value={stats.total} />
          <StatCard label="활성 업체" value={stats.active} tone="success" />
          <StatCard label="승인 대기" value={stats.pending} tone="warning" />
          <StatCard label="이번 달 신규" value={stats.newThisMonth} tone="accent" />
        </div>
      )} */}

      {/* ── 필터 (컨테이너 없이 오픈 스타일) ── */}
      <div className="mb-6 space-y-3">
        {/* 검색 */}
        <div className="mx-auto flex w-1/2 max-w-full gap-2">
          <input
            className="h-[38px] min-w-0 flex-1 rounded-full border border-line px-4 text-[13px] text-ink outline-none transition focus:border-brand"
            type="text"
            placeholder="업체명, 주소, 연락처로 검색"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            onKeyDown={(e) => {
              // ★ Enter 시 URL에 keyword 반영
              if (e.key === "Enter") {
                applyListFilters({ keyword: e.target.value.trim() });
              }
            }}
          />
          <button
            type="button"
            className="h-[38px] shrink-0 rounded-full bg-brand px-[18px] text-[13px] text-white transition hover:bg-brand-dark"
            // ★ 검색 시 URL에 keyword 반영
            onClick={() => applyListFilters({ keyword: keywordInput.trim() })}
          >
            검색
          </button>
        </div>

        {/* 카테고리 + 정렬 pill */}
        <div className="flex flex-wrap items-center justify-between gap-y-2">
          <div className="flex flex-wrap gap-1.5">
            {CATEGORY_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                // ★ 카테고리 필터를 URL에 저장
                onClick={() => applyListFilters({ category: opt.value })}
                className={`rounded-full border px-3.5 py-1.5 text-[13px] transition ${
                  filters.category === opt.value
                    ? "border-brand bg-brand text-white"
                    : "border-line text-ink-muted hover:border-brand hover:text-brand-deep"
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
          <div className="flex flex-wrap gap-1.5">
            {SORT_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                // ★ 정렬을 URL에 저장
                onClick={() => applyListFilters({ sort: opt.value })}
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
      </div>

      {/* ── 모바일: 카드 뷰 ── */}
      <div className="block md:hidden space-y-3">
        {serverData.dtoList.length === 0 && (
          <div className="rounded-2xl border border-line bg-white py-12 text-center text-sm text-ink-muted">
            {filters.keyword || filters.category
              ? `"${filters.keyword || categoryLabel[filters.category]}" 에 해당하는 업체가 없습니다.`
              : "등록된 업체가 없습니다."}
          </div>
        )}
        {serverData.dtoList.map((company) => (
          <div
            key={company.cmno}
            className="rounded-2xl border border-line bg-white p-4 cursor-pointer transition hover:border-brand hover:shadow-sm active:bg-blush-50"
            onClick={() => moveToCompanyRead(company.cmno)}
          >
            <div className="flex items-center gap-3 mb-3">
              <div className="h-14 w-20 overflow-hidden rounded-xl bg-blush-50 shrink-0">
                <CompanyThumb company={company} />
              </div>
              <div className="min-w-0 flex-1">
                <div className="font-semibold text-ink truncate">{company.name}</div>
                <div className="text-xs text-ink-faint mt-0.5">업체 번호 {company.cmno}</div>
                <span className={`mt-1 inline-flex rounded-full border px-2 py-0.5 text-xs font-medium ${categoryBadge[company.category] || "border-line bg-blush-50 text-ink-muted"}`}>
                  {categoryLabel[company.category] || company.category}
                </span>
              </div>
            </div>
            <div className="text-xs text-ink-muted mb-2 truncate">{company.address}</div>
            <div className="flex items-center justify-between">
              <div className="text-sm font-medium text-ink">
                {company.priceAvg ? `${Number(company.priceAvg).toLocaleString()}원` : "-"}
              </div>
              {canManageCompany && (
                <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                  <button
                    className="h-8 rounded-full border border-line px-3 text-xs text-ink-muted transition hover:border-brand hover:text-brand-deep"
                    type="button"
                    onClick={() => navigate({ pathname: `${companyPathPrefix}/modify/${company.cmno}` })}
                  >
                    수정
                  </button>
                  <button
                    className="h-8 rounded-full border border-red-200 px-3 text-xs text-red-500 transition hover:bg-red-50"
                    type="button"
                    onClick={() => handleDelete(company.cmno, company.name)}
                  >
                    삭제
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* ── 데스크톱: 테이블 뷰 ── */}
      <div className="hidden md:block rounded-2xl border border-line overflow-hidden">
        <table className="w-full text-left text-sm border-collapse">
          <thead className="border-b-2 border-brand text-xs text-ink-muted whitespace-nowrap">
            <tr>
              <th className="px-2 py-3">업체명</th>
              <th className="px-2 py-3">유형</th>
              <th className="px-2 py-3 w-full">주소</th>
              <th className="px-2 py-3">연락처</th>
              <th className="px-2 py-3">평균 가격</th>
              <th className="px-2 py-3 text-right">관리</th>
            </tr>
          </thead>
          <tbody>
            {serverData.dtoList.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center text-sm text-ink-muted">
                  {filters.keyword || filters.category
                    ? `"${filters.keyword || categoryLabel[filters.category]}" 에 해당하는 업체가 없습니다.`
                    : "등록된 업체가 없습니다."}
                </td>
              </tr>
            )}
            {serverData.dtoList.map((company) => (
              <tr
                key={company.cmno}
                className="border-b border-line transition hover:bg-blush-50 cursor-pointer"
                onClick={() => moveToCompanyRead(company.cmno)}
              >
                {/* 업체명 */}
                <td className="px-2 py-3">
                  <div className="flex items-center gap-3">
                    <div className="h-[52px] w-[72px] overflow-hidden rounded-lg bg-blush-50 shrink-0">
                      <CompanyThumb company={company} />
                    </div>
                    <div>
                      <div className="font-semibold text-ink text-sm leading-tight whitespace-nowrap">{company.name}</div>
                      <div className="text-xs text-ink-faint whitespace-nowrap">#{company.cmno}</div>
                    </div>
                  </div>
                </td>
                {/* 유형 */}
                <td className="px-2 py-3 whitespace-nowrap">
                  <span className={`inline-flex rounded-full border px-3 py-1 text-sm font-medium ${categoryBadge[company.category] || "border-line bg-blush-50 text-ink-muted"}`}>
                    {categoryLabel[company.category] || company.category}
                  </span>
                </td>
                {/* 주소 — 남은 공간 전부 차지, 길면 말줄임 */}
                <td className="px-2 py-3 text-sm text-ink-muted w-full">
                  <div className="truncate" title={company.address}>{company.address}</div>
                </td>
                {/* 연락처 */}
                <td className="px-2 py-3 text-sm text-ink-muted whitespace-nowrap">{company.phone || "-"}</td>
                {/* 가격 */}
                <td className="px-2 py-3 text-sm font-medium text-ink whitespace-nowrap">
                  {company.priceAvg ? `${Number(company.priceAvg).toLocaleString()}원` : "-"}
                </td>
                {/* 관리 버튼 */}
                <td className="px-2 py-3" onClick={(e) => e.stopPropagation()}>
                  <div className="flex justify-end gap-1.5 whitespace-nowrap">
                    <button
                      className="h-8 rounded-full border border-line px-3.5 text-xs text-ink transition hover:border-brand hover:text-brand-deep"
                      type="button"
                      onClick={() => moveToCompanyRead(company.cmno)}
                    >
                      보기
                    </button>
                    {canManageCompany && (
                      <>
                        <button
                          className="h-8 rounded-full border border-line px-3.5 text-xs text-ink-muted transition hover:border-brand hover:text-brand-deep"
                          type="button"
                          onClick={() => navigate({ pathname: `${companyPathPrefix}/modify/${company.cmno}` })}
                        >
                          수정
                        </button>
                        <button
                          className="h-8 rounded-full border border-red-200 px-3.5 text-xs text-red-500 transition hover:bg-red-50"
                          type="button"
                          onClick={() => handleDelete(company.cmno, company.name)}
                        >
                          삭제
                        </button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-5">
        <PageComponent serverData={serverData} movePage={moveToCompanyList} />
      </div>
      </div>
    </section>
  );
};

// 이미지가 깨질 때 브라우저 깨진 아이콘 대신 "이미지 없음" 상태를 보여줍니다.
const CompanyThumb = ({ company }) => {
  const [hasImageError, setHasImageError] = useState(false);
  const imageFile = company.mainImage || company.uploadFileNames?.[0];
  const imageUrl = imageFile ? getCompanyImageUrl(imageFile, true) : "";

  if (!imageUrl || hasImageError) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50 text-xs text-slate-400">
        이미지 없음
      </div>
    );
  }

  return (
    <img
      className="h-full w-full object-contain rounded-lg"
      alt={company.name}
      src={imageUrl}
      onError={() => setHasImageError(true)}
    />
  );
};

const StatCard = ({ label, value, tone = "default" }) => {
  const toneClass = {
    default: "text-ink",
    success: "text-emerald-600",
    warning: "text-amber-600",
    accent: "text-brand",
  }[tone];

  return (
    <div className="rounded-2xl border border-line bg-white p-3 sm:p-4">
      <div className="mb-1 text-xs text-ink-muted truncate">{label}</div>
      <div className={`text-xl sm:text-2xl font-semibold ${toneClass}`}>{value}</div>
    </div>
  );
};

export default CompanyListComponent;
