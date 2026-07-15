import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getPackageOne } from "../../api/packageApi";
import FetchingModal from "../common/FetchingModal";
import useCustomLogin from "../../hooks/useCustomLogin";

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

const formatPrice = (price) => {
  if (price == null) return "-";
  return `${Number(price).toLocaleString()}원`;
};

const PackageReadComponent = () => {
  const { packageId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { exceptionHandle } = useCustomLogin();

  const [pkg, setPkg] = useState(null);
  const [fetching, setFetching] = useState(false);

  const companyPathPrefix = location.pathname.startsWith("/admin/companies")
    ? "/admin/companies"
    : "/companies";

  useEffect(() => {
    setFetching(true);
    getPackageOne(packageId)
      .then((data) => {
        setPkg(data);
        setFetching(false);
      })
      .catch((err) => {
        setFetching(false);
        exceptionHandle(err);
      });
  }, [packageId]);

  if (!pkg && fetching) {
    return <FetchingModal />;
  }

  if (!pkg) {
    return null;
  }

  return (
    <section className="text-ink">
      {fetching ? <FetchingModal /> : null}

      <button
        type="button"
        onClick={() => navigate(`${companyPathPrefix}/packages`)}
        className="mb-4 text-sm text-ink-muted hover:text-brand"
      >
        ← 패키지 목록
      </button>

      <div className="mb-6 rounded-2xl border border-line bg-white p-5 sm:p-6">
        <h1 className="font-['Gowun_Batang'] text-2xl text-ink">{pkg.name}</h1>
        <p className="mt-2 whitespace-pre-line text-sm text-ink-muted">
          {pkg.description || "설명이 없습니다."}
        </p>
        <div className="mt-4 flex flex-wrap items-end gap-3">
          <p className="text-xl font-semibold text-brand">
            {formatPrice(pkg.salePrice ?? pkg.totalPrice)}
          </p>
          {pkg.salePrice != null && pkg.totalPrice != null && (
            <p className="text-sm text-ink-muted line-through">
              {formatPrice(pkg.totalPrice)}
            </p>
          )}
          <p className="text-xs text-ink-muted">구성 {pkg.items?.length || 0}개</p>
        </div>
      </div>

      <h2 className="mb-3 font-['Gowun_Batang'] text-lg text-ink">포함 구성</h2>

      {!pkg.items?.length ? (
        <div className="rounded-2xl border border-line bg-white py-12 text-center text-sm text-ink-muted">
          포함된 업체가 없습니다.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {pkg.items.map((item) => (
            <div
              key={item.itemId}
              className="rounded-2xl border border-line bg-white p-4"
            >
              <span
                className={`inline-flex rounded-full border px-2.5 py-0.5 text-[11px] ${
                  categoryBadge[item.category] ||
                  "border-slate-200 bg-slate-50 text-slate-600"
                }`}
              >
                {categoryLabel[item.category] || item.category}
              </span>

              <p className="mt-3 font-medium text-ink">{item.companyName}</p>
              <p className="mt-1 text-sm text-ink-muted">
                {item.optionLabel || "기본 옵션"}
              </p>
              <p className="mt-3 text-sm font-semibold text-brand">
                {formatPrice(item.price)}
              </p>

              {item.cmno ? (
                <button
                  type="button"
                  onClick={() =>
                    navigate(`${companyPathPrefix}/read/${item.cmno}`)
                  }
                  className="mt-3 text-xs text-brand hover:underline"
                >
                  업체 상세 보기 →
                </button>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </section>
  );
};

export default PackageReadComponent;
