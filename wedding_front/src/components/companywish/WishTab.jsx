import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getListByMember, postAdd, deleteOne } from "../../api/companywishApi";
import {
  getListByMember as getProductWishList,
  deleteWish as deleteProductWish,
} from "../../api/wishApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import WishFormModal from "./WishFormModal";

const SUB_TABS = [
  { key: "company", label: "업체 찜" },
  { key: "product", label: "답례품 찜" },
];

const WishTab = () => {
  const { loginState } = useCustomLogin();
  const [searchParams, setSearchParams] = useSearchParams();

  // 답례품 상세로 갔다가 뒤로가기 눌러도 "찜 목록 > 답례품 찜"으로 돌아오도록
  // 서브탭 상태를 URL(?wsub=)에 둠
  const subTab = searchParams.get("wsub") === "product" ? "product" : "company";
  const setSubTab = (key) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set("wsub", key);
        return next;
      },
      { replace: true },
    );
  };

  const [wishList, setWishList] = useState([]);
  const [refresh, setRefresh] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState(new Set());

  const [productWishList, setProductWishList] = useState([]);
  const [productRefresh, setProductRefresh] = useState(false);
  const [selectedPnos, setSelectedPnos] = useState(new Set());

  useEffect(() => {
    if (!loginState.email) return;

    getListByMember(loginState.email).then((data) => {
      setWishList(data);
      setSelectedIds(new Set());
    });
  }, [loginState.email, refresh]);

  useEffect(() => {
    if (!loginState.email) return;

    getProductWishList().then((data) => {
      setProductWishList(data);
      setSelectedPnos(new Set());
    });
  }, [loginState.email, productRefresh]);

  const handleAdd = (cmno) => {
    postAdd({ memberEmail: loginState.email, cmno })
      .then(() => {
        setModalOpen(false);
        setRefresh(!refresh);
      })
      .catch((e) => {
        console.error(e);
        alert("찜 등록에 실패했습니다. 이미 찜한 업체일 수 있습니다.");
      });
  };

  const handleDelete = (wishId) => {
    deleteOne(wishId)
      .then(() => setRefresh(!refresh))
      .catch((e) => console.error(e));
  };

  const handleDeleteProductWish = (pno) => {
    deleteProductWish(pno)
      .then(() => setProductRefresh(!productRefresh))
      .catch((e) => console.error(e));
  };

  const toggleSelect = (id) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    setSelectedIds((prev) =>
      prev.size === wishList.length
        ? new Set()
        : new Set(wishList.map((item) => item.wishId)),
    );
  };

  const handleBulkDelete = () => {
    if (selectedIds.size === 0) return;
    if (!window.confirm(`선택한 ${selectedIds.size}곳을 찜 취소하시겠습니까?`))
      return;

    Promise.all([...selectedIds].map((id) => deleteOne(id)))
      .then(() => setRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  const toggleSelectProduct = (pno) => {
    setSelectedPnos((prev) => {
      const next = new Set(prev);
      next.has(pno) ? next.delete(pno) : next.add(pno);
      return next;
    });
  };

  const toggleSelectAllProduct = () => {
    setSelectedPnos((prev) =>
      prev.size === productWishList.length
        ? new Set()
        : new Set(productWishList.map((item) => item.pno)),
    );
  };

  const handleBulkDeleteProduct = () => {
    if (selectedPnos.size === 0) return;
    if (!window.confirm(`선택한 ${selectedPnos.size}개를 찜 취소하시겠습니까?`))
      return;

    Promise.all([...selectedPnos].map((pno) => deleteProductWish(pno)))
      .then(() => setProductRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  return (
    <div>
      <nav className="flex gap-6 text-sm font-medium border-b border-line mb-6">
        {SUB_TABS.map((tab) => (
          <span
            key={tab.key}
            onClick={() => setSubTab(tab.key)}
            className={`pb-3 border-b cursor-pointer ${
              subTab === tab.key
                ? "text-brand border-brand"
                : "text-ink-soft border-transparent hover:text-ink"
            }`}
          >
            {tab.label}
          </span>
        ))}
      </nav>

      {subTab === "company" && (
        <div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-6">
            <div className="flex items-center gap-4">
              <p className="text-sm text-ink-muted">
                찜한 업체 {wishList.length}곳
              </p>
              {wishList.length > 0 && (
                <label className="flex items-center gap-1.5 text-xs text-ink-soft cursor-pointer">
                  <input
                    type="checkbox"
                    checked={
                      wishList.length > 0 &&
                      selectedIds.size === wishList.length
                    }
                    onChange={toggleSelectAll}
                    className="accent-brand"
                  />
                  전체선택
                </label>
              )}
            </div>
            <div className="flex items-center gap-2">
              {selectedIds.size > 0 && (
                <button
                  type="button"
                  onClick={handleBulkDelete}
                  className="h-10 px-4 rounded-full border border-red-200 text-red-600 text-sm font-medium hover:bg-red-50"
                >
                  선택 삭제 ({selectedIds.size})
                </button>
              )}
              <button
                type="button"
                onClick={() => setModalOpen(true)}
                className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
              >
                + 업체 찜하기
              </button>
            </div>
          </div>

          {wishList.length === 0 && (
            <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
              찜한 업체가 없습니다.
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {wishList.map((item) => (
              <div
                key={item.wishId}
                className="relative bg-white rounded-2xl border border-line p-5"
              >
                <input
                  type="checkbox"
                  checked={selectedIds.has(item.wishId)}
                  onChange={() => toggleSelect(item.wishId)}
                  className="absolute top-4 left-4 w-4 h-4 accent-brand z-10"
                />

                <div className="aspect-square rounded-xl bg-surface flex items-center justify-center mb-3">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#D4537E"
                    strokeWidth="1.5"
                    className="w-7 h-7 opacity-60"
                  >
                    <path d="M19.5 12.572 12 20l-7.5-7.428a5 5 0 1 1 7.5-6.566 5 5 0 1 1 7.5 6.566Z" />
                  </svg>
                </div>

                <p className="text-sm font-medium text-ink mb-1">
                  업체 번호 #{item.cmno}
                </p>
                <p className="text-xs text-ink-muted mb-4">{item.regDate}</p>

                <button
                  type="button"
                  onClick={() => handleDelete(item.wishId)}
                  className="w-full h-9 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
                >
                  찜 취소
                </button>
              </div>
            ))}
          </div>

          {modalOpen && (
            <WishFormModal
              onSubmit={handleAdd}
              onClose={() => setModalOpen(false)}
            />
          )}
        </div>
      )}

      {subTab === "product" && (
        <div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-6">
            <div className="flex items-center gap-4">
              <p className="text-sm text-ink-muted">
                찜한 답례품 {productWishList.length}개
              </p>
              {productWishList.length > 0 && (
                <label className="flex items-center gap-1.5 text-xs text-ink-soft cursor-pointer">
                  <input
                    type="checkbox"
                    checked={
                      productWishList.length > 0 &&
                      selectedPnos.size === productWishList.length
                    }
                    onChange={toggleSelectAllProduct}
                    className="accent-brand"
                  />
                  전체선택
                </label>
              )}
            </div>
            {selectedPnos.size > 0 && (
              <button
                type="button"
                onClick={handleBulkDeleteProduct}
                className="h-10 px-4 rounded-full border border-red-200 text-red-600 text-sm font-medium hover:bg-red-50"
              >
                선택 삭제 ({selectedPnos.size})
              </button>
            )}
          </div>

          {productWishList.length === 0 && (
            <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
              찜한 답례품이 없습니다.
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {productWishList.map((item) => (
              <div
                key={item.wno}
                className="relative bg-white rounded-2xl border border-line p-5"
              >
                <input
                  type="checkbox"
                  checked={selectedPnos.has(item.pno)}
                  onChange={() => toggleSelectProduct(item.pno)}
                  className="absolute top-4 left-4 w-4 h-4 accent-brand z-10"
                />

                <Link
                  to={`/product/read/${item.pno}`}
                  className="block aspect-square rounded-xl bg-surface overflow-hidden mb-3"
                >
                  {item.thumbnail && (
                    <img
                      src={`${API_SERVER_HOST}/api/product/view/s_${item.thumbnail}`}
                      alt={item.pname}
                      className="w-full h-full object-cover"
                    />
                  )}
                </Link>

                <Link
                  to={`/product/read/${item.pno}`}
                  className="text-sm font-medium text-ink mb-1 truncate block hover:text-brand hover:underline"
                >
                  {item.pname}
                </Link>
                <p className="text-xs text-ink-muted mb-4">
                  {Number(item.price).toLocaleString()}원
                </p>

                <button
                  type="button"
                  onClick={() => handleDeleteProductWish(item.pno)}
                  className="w-full h-9 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
                >
                  찜 취소
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default WishTab;
