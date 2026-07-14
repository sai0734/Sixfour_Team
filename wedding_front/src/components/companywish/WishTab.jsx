import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  getMyCompanyWishes,
  removeCompanyWishByWishId,
} from "../../api/companywishApi";
import {
  getListByMember as getProductWishList,
  deleteWish as deleteProductWish,
} from "../../api/wishApi";
import { getCompanyImageUrl } from "../../api/companyApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const SUB_TABS = [
  { key: "company", label: "업체 찜" },
  { key: "product", label: "답례품 찜" },
];

const categoryLabel = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

const WishTab = () => {
  const { loginState } = useCustomLogin();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

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

  // ── 업체 찜 상태 ──
  // 재원 수정 - 옵션(홀/드레스/메이크업)별로 같은 업체를 여러 건 찜할 수 있게 되면서
  // cmno 대신 wishId를 선택/삭제 기준 키로 사용
  const [wishList, setWishList] = useState([]);
  const [companyLoading, setCompanyLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [companyRefresh, setCompanyRefresh] = useState(false);

  // ── 답례품 찜 상태 ──
  const [productWishList, setProductWishList] = useState([]);
  const [selectedPnos, setSelectedPnos] = useState(new Set());
  const [productRefresh, setProductRefresh] = useState(false);

  // ── 업체 찜 목록 로드 ──
  useEffect(() => {
    if (!loginState.email) return;

    setCompanyLoading(true);
    getMyCompanyWishes()
      .then((data) => {
        const list = Array.isArray(data)
          ? data
          : (data?.dtoList ?? data?.content ?? []);
        setWishList(list);
        setSelectedIds(new Set());
      })
      .catch((e) => {
        console.error("업체 찜 목록 조회 실패:", e);
      })
      .finally(() => setCompanyLoading(false));
  }, [loginState.email, companyRefresh]);

  // ── 답례품 찜 목록 로드 ──
  useEffect(() => {
    if (!loginState.email) return;

    getProductWishList().then((data) => {
      setProductWishList(data);
      setSelectedPnos(new Set());
    });
  }, [loginState.email, productRefresh]);

  // ── 업체 찜 해제 (단건, wishId 기준) ──
  const handleRemoveCompanyWish = async (event, wishId) => {
    event.stopPropagation();
    if (!window.confirm("찜한 업체에서 삭제하시겠습니까?")) return;

    try {
      await removeCompanyWishByWishId(wishId);
      setWishList((prev) => prev.filter((c) => c.wishId !== wishId));
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(wishId);
        return next;
      });
    } catch (e) {
      console.error("찜 삭제 실패:", e);
      alert("찜 삭제에 실패했습니다.");
    }
  };

  // ── 업체 찜 선택/전체선택 (wishId 기준) ──
  const toggleSelect = (wishId) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.has(wishId) ? next.delete(wishId) : next.add(wishId);
      return next;
    });
  };

  const toggleSelectAll = () => {
    setSelectedIds((prev) =>
      prev.size === wishList.length
        ? new Set()
        : new Set(wishList.map((c) => c.wishId)),
    );
  };

  // ── 업체 찜 선택 삭제 (wishId 기준) ──
  const handleBulkDelete = async () => {
    if (selectedIds.size === 0) return;
    if (!window.confirm(`선택한 ${selectedIds.size}건을 찜 취소하시겠습니까?`))
      return;

    try {
      await Promise.all(
        [...selectedIds].map((wishId) => removeCompanyWishByWishId(wishId)),
      );
      setCompanyRefresh((r) => !r);
    } catch (e) {
      console.error("선택 삭제 실패:", e);
      alert("일부 항목 삭제에 실패했습니다.");
    }
  };

  // ── 답례품 찜 선택/전체선택 ──
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
      {/* ── 서브탭 ── */}
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

      {/* ── 업체 찜 탭 ── */}
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
            {selectedIds.size > 0 && (
              <button
                type="button"
                onClick={handleBulkDelete}
                className="h-10 px-4 rounded-full border border-red-200 text-red-600 text-sm font-medium hover:bg-red-50"
              >
                선택 삭제 ({selectedIds.size})
              </button>
            )}
          </div>

          {companyLoading && (
            <div className="text-center text-ink-faint py-16">로딩 중...</div>
          )}

          {!companyLoading && wishList.length === 0 && (
            <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
              <p className="text-sm">찜한 업체가 없습니다.</p>
              <button
                type="button"
                onClick={() => navigate("/companies/list")}
                className="mt-4 h-10 rounded-full border border-line px-5 text-sm transition hover:border-brand hover:text-brand"
              >
                업체 보러가기
              </button>
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {wishList.map((item) => {
              const mainImage = item.uploadFileNames?.[0];

              return (
                <article
                  key={item.wishId}
                  className="relative bg-white rounded-2xl border border-line overflow-hidden transition hover:border-brand hover:shadow-md cursor-pointer"
                  onClick={() => navigate(`/companies/read/${item.cmno}`)}
                >
                  {/* 체크박스 - 카드 우측 상단, 조금 더 크게 */}
                  <input
                    type="checkbox"
                    checked={selectedIds.has(item.wishId)}
                    onChange={(e) => {
                      e.stopPropagation();
                      toggleSelect(item.wishId);
                    }}
                    onClick={(e) => e.stopPropagation()}
                    className="absolute top-3 right-3 w-5 h-5 accent-brand z-10"
                  />

                  {/* 대표 이미지 */}
                  {mainImage ? (
                    <img
                      src={getCompanyImageUrl(mainImage)}
                      alt={item.name}
                      className="h-44 w-full object-cover"
                    />
                  ) : (
                    <div className="flex h-44 items-center justify-center bg-blush-50 text-sm text-ink-faint">
                      대표 이미지 없음
                    </div>
                  )}

                  <div className="p-4">
                    <div className="mb-2 flex items-center justify-between gap-2">
                      <span className="rounded-full bg-blush-100 px-2.5 py-1 text-xs text-brand-deep">
                        {categoryLabel[item.category] || item.category}
                      </span>

                      {/* 찜 해제 버튼 */}
                      <button
                        type="button"
                        onClick={(e) => handleRemoveCompanyWish(e, item.wishId)}
                        className="text-lg text-rose-500 transition hover:scale-110"
                        title="찜 해제"
                        aria-label={`${item.name} 찜 해제`}
                      >
                        ♥
                      </button>
                    </div>

                    <p className="truncate text-base font-semibold text-ink">
                      {item.name}
                    </p>

                    {/* 재원 추가 - 옵션과 함께 찜한 경우 어떤 옵션인지 표시 */}
                    {item.optionName && (
                      <p className="mt-1 truncate text-xs font-medium text-brand">
                        찜한 옵션: {item.optionName}
                      </p>
                    )}

                    {item.address && (
                      <p className="mt-2 line-clamp-2 text-sm text-ink-muted">
                        📍 {item.address}
                      </p>
                    )}

                    {item.phone && (
                      <p className="mt-1 text-sm text-ink-muted">
                        📞 {item.phone}
                      </p>
                    )}

                    {item.priceAvg && (
                      <p className="mt-3 text-base font-medium">
                        {Number(item.priceAvg).toLocaleString()}원~
                      </p>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      )}

      {/* ── 답례품 찜 탭 ── */}
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
                  className="absolute top-3 right-3 w-5 h-5 accent-brand z-10"
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
                  onClick={() =>
                    deleteProductWish(item.pno)
                      .then(() => setProductRefresh((r) => !r))
                      .catch((e) => console.error(e))
                  }
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
