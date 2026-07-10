import { useEffect, useRef, useState } from "react";
import {
  createSearchParams,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import {
  getAdminOrderList,
  bulkChangeOrderStatus,
} from "../../api/adminOrderApi";
import PageComponent from "../common/PageComponent";
import AdminLayout from "../../layouts/AdminLayout";

const STATUS_TABS = [
  { key: "", label: "전체" },
  { key: "PAID", label: "결제완료" },
  { key: "SHIPPING_READY", label: "배송준비" },
  { key: "SHIPPING", label: "배송중" },
  { key: "DELIVERED", label: "배송완료" },
  { key: "REFUNDED", label: "환불완료" },
  { key: "CANCELLED", label: "취소" },
];

const initState = {
  dtoList: [],
  pageNumList: [],
  totalCount: 0,
  current: 0,
};

const AdminOrderListComponent = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const selectAllRef = useRef(null);

  const page = Number(searchParams.get("page")) || 1;
  const size = Number(searchParams.get("size")) || 10;

  const moveToList = (pageParam) => {
    const pageNum = pageParam?.page ?? page;
    const sizeNum = pageParam?.size ?? size;

    navigate({
      pathname: "/admin/orders",
      search: createSearchParams({ page: pageNum, size: sizeNum }).toString(),
    });
  };

  const listQuery = createSearchParams({ page, size }).toString();

  const goToDetail = (ono) => {
    navigate({ pathname: `/admin/orders/${ono}`, search: listQuery });
  };

  const [serverData, setServerData] = useState(initState);

  const [status, setStatus] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [selectedOnos, setSelectedOnos] = useState([]);
  const [bulkStatus, setBulkStatus] = useState("SHIPPING_READY");

  useEffect(() => {
    if (!searchParams.get("page")) {
      navigate(
        {
          pathname: "/admin/orders",
          search: createSearchParams({ page: 1, size }).toString(),
        },
        { replace: true },
      );
    }
  }, [navigate, searchParams, size]);

  const fetchList = () => {
    getAdminOrderList({ page, size, keyword, status, sortType: "latest" }).then(
      (data) => {
        setServerData(data);
        setSelectedOnos([]);
      },
    );
  };

  useEffect(() => {
    fetchList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, keyword, status]);

  const handleSearch = () => {
    moveToList({ page: 1, size });
    setKeyword(keywordInput);
  };

  const handleToggleSelect = (ono) => {
    setSelectedOnos((prev) =>
      prev.includes(ono) ? prev.filter((o) => o !== ono) : [...prev, ono],
    );
  };

  const currentPageOnos = serverData.dtoList.map((o) => o.ono);

  const isAllSelected =
    currentPageOnos.length > 0 &&
    currentPageOnos.every((ono) => selectedOnos.includes(ono));

  const isSomeSelected = currentPageOnos.some((ono) =>
    selectedOnos.includes(ono),
  );

  useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = isSomeSelected && !isAllSelected;
    }
  }, [isSomeSelected, isAllSelected]);

  const handleToggleSelectAll = () => {
    if (isSomeSelected) {
      setSelectedOnos((prev) =>
        prev.filter((ono) => !currentPageOnos.includes(ono)),
      );
    } else {
      setSelectedOnos((prev) => [...new Set([...prev, ...currentPageOnos])]);
    }
  };

  const handleBulkChange = () => {
    if (selectedOnos.length === 0) {
      alert("변경할 주문을 선택해주세요.");
      return;
    }
    if (
      !window.confirm(
        `선택한 ${selectedOnos.length}건을 상태 변경하시겠습니까?`,
      )
    )
      return;

    bulkChangeOrderStatus(selectedOnos, bulkStatus).then(() => {
      alert("일괄 상태 변경이 완료되었습니다.");
      fetchList();
    });
  };

  return (
    <AdminLayout>
      <p className="font-serif text-2xl mb-5">주문 관리</p>

      <div className="flex gap-5 border-b border-line mb-5 text-sm">
        {STATUS_TABS.map((tab) => (
          <span
            key={tab.key}
            onClick={() => {
              moveToList({ page: 1, size });
              setStatus(tab.key);
            }}
            className={`pb-3 cursor-pointer ${
              status === tab.key
                ? "text-ink font-medium border-b-2 border-brand"
                : "text-ink-faint"
            }`}
          >
            {tab.label}
          </span>
        ))}
      </div>

      <div className="flex justify-between items-center mb-4">
        <div className="flex gap-2">
          <input
            type="text"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            placeholder="주문번호 또는 주문자명 검색"
            className="h-9 px-4 border border-line-soft rounded-lg text-sm w-64"
          />
          <button
            onClick={handleSearch}
            className="h-9 px-4 border border-line-soft rounded-lg text-sm"
          >
            검색
          </button>
        </div>

        <div className="flex gap-2 items-center">
          <select
            value={bulkStatus}
            onChange={(e) => setBulkStatus(e.target.value)}
            className="h-9 px-3 border border-line-soft rounded-lg text-sm"
          >
            <option value="SHIPPING_READY">배송준비로 변경</option>
            <option value="SHIPPING">배송중으로 변경</option>
            <option value="DELIVERED">배송완료로 변경</option>
          </select>
          <button
            onClick={handleBulkChange}
            className="h-9 px-4 rounded-lg bg-brand text-white text-sm"
          >
            선택 일괄 변경 ({selectedOnos.length})
          </button>
        </div>
      </div>

      <table className="w-full text-sm border-t border-line">
        <thead>
          <tr className="border-b border-line text-ink-faint text-xs">
            <th className="py-3 w-10 text-center">
              <input
                ref={selectAllRef}
                type="checkbox"
                checked={isAllSelected}
                onChange={handleToggleSelectAll}
                className="accent-brand"
                title="현재 페이지 전체 선택/해제"
              />
            </th>
            <th className="py-3 text-left">주문번호</th>
            <th className="py-3 text-left">주문자</th>
            <th className="py-3 text-right">결제금액</th>
            <th className="py-3 text-center">상태</th>
            <th className="py-3 text-left">주문일</th>
          </tr>
        </thead>
        <tbody>
          {serverData.dtoList.map((o) => (
            <tr key={o.ono} className="border-b border-line hover:bg-cream">
              <td
                className="py-2.5 text-center"
                onClick={(e) => e.stopPropagation()}
              >
                <input
                  type="checkbox"
                  checked={selectedOnos.includes(o.ono)}
                  onChange={() => handleToggleSelect(o.ono)}
                  className="accent-brand"
                />
              </td>
              <td
                onClick={() => goToDetail(o.ono)}
                className="py-2.5 cursor-pointer text-brand-accent"
              >
                {o.orderNumber}
              </td>
              <td
                onClick={() => goToDetail(o.ono)}
                className="py-2.5 cursor-pointer"
              >
                {o.receiverName} ({o.memberEmail})
              </td>
              <td
                onClick={() => goToDetail(o.ono)}
                className="py-2.5 text-right cursor-pointer"
              >
                {o.totalPrice?.toLocaleString()}원
              </td>
              <td
                onClick={() => goToDetail(o.ono)}
                className="py-2.5 text-center cursor-pointer"
              >
                <span className="px-2 py-1 rounded-full text-xs bg-brand-light text-brand-accent">
                  {STATUS_TABS.find((t) => t.key === o.orderStatus)?.label ??
                    o.orderStatus}
                </span>
              </td>
              <td
                onClick={() => goToDetail(o.ono)}
                className="py-2.5 text-ink-faint cursor-pointer"
              >
                {o.regDate?.slice(0, 10)}
              </td>
            </tr>
          ))}
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

export default AdminOrderListComponent;
