import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import { deleteOne, getCompanyImageUrl, getList } from "../../api/companyApi";
import FetchingModal from "../common/FetchingModal";
import PageComponent from "../common/PageComponent";
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
  const [filters, setFilters] = useState({
    keyword: "",
    category: "",
    sort: "latest",
  });
  const [reloadKey, setReloadKey] = useState(0);
  const { exceptionHandle } = useCustomLogin();
  const { page, size, refresh } = useCustomMove();
  const navigate = useNavigate();
  const location = useLocation();
  const loginState = useSelector((state) => state.loginSlice);
  const canManageCompany = loginState.roleNames?.some((roleName) =>
    adminRoles.includes(roleName),
  );
  // 관리자 경로에서 들어온 경우 등록/수정 이동도 /admin/companies 하위로 유지합니다.
  const companyPathPrefix = location.pathname.startsWith("/admin/companies")
    ? "/admin/companies"
    : "/companies";

  const moveToCompanyRead = (cmno) => {
    navigate({
      pathname: `${companyPathPrefix}/read/${cmno}`,
      search: `page=${page}&size=${size}`,
    });
  };

  const moveToCompanyList = (pageParam) => {
    const pageNum = pageParam?.page || page;
    const sizeNum = pageParam?.size || size;

    navigate({
      pathname: `${companyPathPrefix}/list`,
      search: `page=${pageNum}&size=${sizeNum}`,
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
    if (!window.confirm(`${name} 업체를 삭제하시겠습니까?`)) {
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

  return (
    <section className="mx-auto max-w-6xl p-4 text-slate-800">
      {fetching ? <FetchingModal /> : null}

      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="text-center">
          <h2 className="text-xl font-semibold">
            {canManageCompany ? "업체 관리" : "업체 리스트"}
          </h2>
          <p className="mt-1 text-sm text-slate-500"></p>
        </div>
        {canManageCompany ? (
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-blue-600 bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
            onClick={() => navigate({ pathname: `${companyPathPrefix}/add` })}
          >
            <span className="text-lg leading-none">+</span>
            업체 등록
          </button>
        ) : null}
      </div>

      {canManageCompany ? (
        <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
          <StatCard label="전체 업체" value={stats.total} />
          <StatCard label="활성 업체" value={stats.active} tone="success" />
          <StatCard label="승인 대기" value={stats.pending} tone="warning" />
          <StatCard
            label="이번 달 신규"
            value={stats.newThisMonth}
            tone="accent"
          />
        </div>
      ) : null}

      <div className="mb-5 flex flex-wrap gap-2">
        <input
          className="h-10 min-w-64 flex-1 rounded-md border border-slate-300 bg-white px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          type="text"
          placeholder="업체명, 주소, 연락처로 검색"
          value={filters.keyword}
          onChange={(e) =>
            setFilters((prev) => ({ ...prev, keyword: e.target.value }))
          }
        />
        <select
          className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
          value={filters.category}
          onChange={(e) =>
            setFilters((prev) => ({ ...prev, category: e.target.value }))
          }
        >
          <option value="">업체 유형 전체</option>
          <option value="HALL">웨딩홀</option>
          <option value="DRESS">드레스</option>
          <option value="MAKEUP">메이크업</option>
          <option value="STUDIO">스튜디오</option>
        </select>
        <select
          className="h-10 rounded-md border border-slate-300 bg-white px-3 text-sm"
          value={filters.sort}
          onChange={(e) =>
            setFilters((prev) => ({ ...prev, sort: e.target.value }))
          }
        >
          <option value="latest">정렬: 최신순</option>
          <option value="name">정렬: 이름순</option>
          <option value="price">정렬: 가격순</option>
        </select>
      </div>

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full border-collapse text-left text-sm">
          <thead className="bg-slate-50 text-xs font-medium uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">업체명</th>
              <th className="px-4 py-3">유형</th>
              <th className="px-4 py-3">주소</th>
              <th className="px-4 py-3">연락처</th>
              <th className="px-4 py-3">평균 가격</th>
              <th className="px-4 py-3 text-right">관리</th>
            </tr>
          </thead>
          <tbody>
            {serverData.dtoList.map((company) => (
              <tr
                key={company.cmno}
                className="border-t border-slate-100 hover:bg-slate-50"
              >
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className="h-14 w-20 overflow-hidden rounded-md bg-white">
                      <CompanyThumb company={company} />
                    </div>
                    <div>
                      <div className="font-semibold text-slate-900">
                        {company.name}
                      </div>
                      <div className="text-xs text-slate-500">
                        업체 번호 {company.cmno}
                      </div>
                    </div>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span
                    className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${categoryBadge[company.category] || "border-slate-200 bg-slate-50 text-slate-600"}`}
                  >
                    {categoryLabel[company.category] || company.category}
                  </span>
                </td>
                <td className="max-w-56 px-4 py-3 text-slate-600">
                  {company.address}
                </td>
                <td className="px-4 py-3 text-slate-600">
                  {company.phone || "-"}
                </td>
                <td className="px-4 py-3 font-medium">
                  {company.priceAvg
                    ? Number(company.priceAvg).toLocaleString()
                    : "-"}
                  원
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <button
                      className="h-8 rounded-md border border-slate-200 px-3 text-xs hover:bg-slate-100"
                      type="button"
                      onClick={() => moveToCompanyRead(company.cmno)}
                    >
                      보기
                    </button>
                    {canManageCompany ? (
                      <>
                        <button
                          className="h-8 rounded-md border border-blue-200 px-3 text-xs text-blue-700 hover:bg-blue-50"
                          type="button"
                          onClick={() =>
                            navigate({
                              pathname: `${companyPathPrefix}/modify/${company.cmno}`,
                            })
                          }
                        >
                          수정
                        </button>
                        <button
                          className="h-8 rounded-md border border-red-200 px-3 text-xs text-red-700 hover:bg-red-50"
                          type="button"
                          onClick={() =>
                            handleDelete(company.cmno, company.name)
                          }
                        >
                          삭제
                        </button>
                      </>
                    ) : null}
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
      className="h-full w-full object-contain"
      alt={company.name}
      src={imageUrl}
      onError={() => setHasImageError(true)}
    />
  );
};

const StatCard = ({ label, value, tone = "default" }) => {
  const toneClass = {
    default: "text-slate-900",
    success: "text-emerald-600",
    warning: "text-amber-600",
    accent: "text-blue-600",
  }[tone];

  return (
    <div className="rounded-lg bg-slate-50 p-4">
      <div className="mb-1 text-xs text-slate-500">{label}</div>
      <div className={`text-2xl font-semibold ${toneClass}`}>{value}</div>
    </div>
  );
};

export default CompanyListComponent;
