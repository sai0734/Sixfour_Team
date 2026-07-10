import { useEffect, useState } from "react";
import {
  createSearchParams,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { getAdminProductList } from "../../api/adminProductApi";
import { deleteOne, getCategories } from "../../api/productApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import PageComponent from "../common/PageComponent";
import AdminLayout from "../../layouts/AdminLayout";

const host = API_SERVER_HOST;

const SALE_STATUS_LABEL = {
  ON_SALE: { label: "판매중", className: "bg-green-100 text-green-700" },
  SOLD_OUT: { label: "품절", className: "bg-amber-100 text-amber-700" },
  HIDDEN: { label: "숨김", className: "bg-gray-200 text-gray-600" },
};

const initState = {
  dtoList: [],
  pageNumList: [],
  totalCount: 0,
  current: 0,
};

const AdminProductListComponent = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const page = Number(searchParams.get("page")) || 1;
  const size = Number(searchParams.get("size")) || 10;

  const moveToList = (pageParam) => {
    const pageNum = pageParam?.page ?? page;
    const sizeNum = pageParam?.size ?? size;

    navigate({
      pathname: "/admin/products",
      search: createSearchParams({ page: pageNum, size: sizeNum }).toString(),
    });
  };

  const listQuery = createSearchParams({ page, size }).toString();

  const [serverData, setServerData] = useState(initState);
  const [categoryList, setCategoryList] = useState([]);

  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("");
  const [saleStatus, setSaleStatus] = useState("");
  const [sortType, setSortType] = useState("latest");

  useEffect(() => {
    getCategories().then((data) => setCategoryList(data));
  }, []);

  useEffect(() => {
    if (!searchParams.get("page")) {
      navigate(
        {
          pathname: "/admin/products",
          search: createSearchParams({ page: 1, size }).toString(),
        },
        { replace: true },
      );
    }
  }, [navigate, searchParams, size]);

  const fetchList = () => {
    getAdminProductList({
      page,
      size,
      keyword,
      category,
      saleStatus,
      sortType,
    }).then((data) => setServerData(data));
  };

  useEffect(() => {
    fetchList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, keyword, category, saleStatus, sortType]);

  const handleSearch = () => {
    moveToList({ page: 1, size });
    setKeyword(keywordInput);
  };

  const handleClickAdd = () => {
    navigate("/product/add");
  };

  const handleClickRow = (pno) => {
    navigate({ pathname: `/product/read/${pno}`, search: listQuery });
  };

  const handleClickModify = (e, pno) => {
    e.stopPropagation();
    navigate({ pathname: `/product/modify/${pno}`, search: listQuery });
  };

  const handleClickDelete = (e, pno, pname) => {
    e.stopPropagation();

    if (!window.confirm(`"${pname}" 상품을 삭제(숨김) 처리하시겠습니까?`))
      return;

    deleteOne(pno).then(() => {
      alert("삭제되었습니다.");
      fetchList();
    });
  };

  return (
    <AdminLayout>
      <div className="flex justify-between items-center mb-5">
        <p className="font-serif text-2xl">상품 관리</p>
        <button
          onClick={handleClickAdd}
          className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium"
        >
          상품 추가
        </button>
      </div>

      <div className="flex flex-wrap gap-2 mb-5">
        <input
          type="text"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          placeholder="상품명 검색"
          className="h-9 px-4 border border-line-soft rounded-lg text-sm w-48"
        />
        <button
          onClick={handleSearch}
          className="h-9 px-4 border border-line-soft rounded-lg text-sm"
        >
          검색
        </button>

        <select
          value={category}
          onChange={(e) => {
            moveToList({ page: 1, size }); // [수정]
            setCategory(e.target.value);
          }}
          className="h-9 px-3 border border-line-soft rounded-lg text-sm"
        >
          <option value="">전체 카테고리</option>
          {categoryList.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>

        <select
          value={saleStatus}
          onChange={(e) => {
            moveToList({ page: 1, size }); // [수정]
            setSaleStatus(e.target.value);
          }}
          className="h-9 px-3 border border-line-soft rounded-lg text-sm"
        >
          <option value="">전체 판매상태</option>
          <option value="ON_SALE">판매중</option>
          <option value="SOLD_OUT">품절</option>
          <option value="HIDDEN">숨김</option>
        </select>

        <select
          value={sortType}
          onChange={(e) => setSortType(e.target.value)}
          className="h-9 px-3 border border-line-soft rounded-lg text-sm"
        >
          <option value="latest">최신등록순</option>
          <option value="stockAsc">재고적은순</option>
          <option value="salesDesc">판매많은순</option>
        </select>
      </div>

      <table className="w-full text-sm border-t border-line">
        <thead>
          <tr className="border-b border-line text-ink-faint text-xs">
            <th className="py-3 text-left">썸네일</th>
            <th className="py-3 text-left">상품명</th>
            <th className="py-3 text-left">카테고리</th>
            <th className="py-3 text-right">가격</th>
            <th className="py-3 text-right">재고</th>
            <th className="py-3 text-center">판매상태</th>
            <th className="py-3 text-center">관리</th>
          </tr>
        </thead>
        <tbody>
          {serverData.dtoList.map((p) => {
            const status = SALE_STATUS_LABEL[p.saleStatus] ?? {
              label: p.saleStatus,
              className: "",
            };

            return (
              <tr
                key={p.pno}
                onClick={() => handleClickRow(p.pno)}
                className={`border-b border-line cursor-pointer hover:bg-cream ${p.lowStock ? "bg-red-50" : ""}`}
              >
                <td className="py-2.5">
                  <div className="w-12 h-12 rounded-lg overflow-hidden bg-surface">
                    {p.thumbnail && (
                      <img
                        alt={p.pname}
                        className="w-full h-full object-cover"
                        src={`${host}/api/product/view/s_${p.thumbnail}`}
                      />
                    )}
                  </div>
                </td>
                <td className="py-2.5">{p.pname}</td>
                <td className="py-2.5 text-ink-faint">{p.category}</td>
                <td className="py-2.5 text-right">
                  {p.price?.toLocaleString()}원
                </td>
                <td className="py-2.5 text-right">
                  {p.stockQty}
                  {p.lowStock && (
                    <span className="ml-1.5 text-xs text-red-600 font-medium">
                      재고부족
                    </span>
                  )}
                </td>
                <td className="py-2.5 text-center">
                  <span
                    className={`px-2 py-1 rounded-full text-xs ${status.className}`}
                  >
                    {status.label}
                  </span>
                </td>
                <td className="py-2.5 text-center">
                  <button
                    onClick={(e) => handleClickModify(e, p.pno)}
                    className="text-xs text-brand-accent underline mr-3"
                  >
                    수정
                  </button>
                  <button
                    onClick={(e) => handleClickDelete(e, p.pno, p.pname)}
                    className="text-xs text-ink-faint underline"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <PageComponent
        serverData={serverData}
        movePage={(pageParam) =>
          moveToList({ page: pageParam.page, size: pageParam.size ?? size })
        }
      />
    </AdminLayout>
  );
};

export default AdminProductListComponent;
