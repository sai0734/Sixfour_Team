import { useSearchParams } from "react-router-dom";
import HallPaymentTab from "../hallpayment/HallPaymentTab";
import ProductPaymentTab from "./ProductPaymentTab";

const VENDOR_SUBTABS = [
  { key: "hall", label: "홀", enabled: true },
  { key: "studio", label: "스튜디오", enabled: false },
  { key: "dress", label: "드레스", enabled: false },
  { key: "makeup", label: "메이크업", enabled: false },
];

const PaymentTab = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // 상품 상세로 갔다가 뒤로가기 눌러도 "결제내역 > 답례품 쇼핑몰"로 그대로
  // 돌아오도록, 탭 상태를 컴포넌트 state가 아니라 URL(?psub=, &pvsub=)에 둠
  const topTab = searchParams.get("psub") === "product" ? "product" : "vendor";
  const vendorSubTab = searchParams.get("pvsub") || "hall";

  const updateParam = (key, value) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set(key, value);
        return next;
      },
      { replace: true },
    );
  };

  return (
    <div>
      <div className="flex gap-2 mb-5">
        <button
          type="button"
          onClick={() => updateParam("psub", "vendor")}
          className={`h-9 px-5 rounded-full text-sm font-medium ${
            topTab === "vendor"
              ? "bg-brand text-white"
              : "bg-surface text-ink-muted"
          }`}
        >
          업체
        </button>
        <button
          type="button"
          onClick={() => updateParam("psub", "product")}
          className={`h-9 px-5 rounded-full text-sm font-medium ${
            topTab === "product"
              ? "bg-brand text-white"
              : "bg-surface text-ink-muted"
          }`}
        >
          답례품 쇼핑몰
        </button>
      </div>

      {topTab === "vendor" && (
        <>
          <div className="flex gap-1 mb-6 border-b border-line">
            {VENDOR_SUBTABS.map((tab) => {
              if (!tab.enabled) {
                return (
                  <span
                    key={tab.key}
                    title="준비 중인 기능입니다"
                    className="px-4 pb-2.5 text-sm text-ink-faint cursor-not-allowed"
                  >
                    {tab.label}
                  </span>
                );
              }

              return (
                <span
                  key={tab.key}
                  onClick={() => updateParam("pvsub", tab.key)}
                  className={`px-4 pb-2.5 text-sm cursor-pointer border-b-2 ${
                    vendorSubTab === tab.key
                      ? "border-brand text-brand font-medium"
                      : "border-transparent text-ink-soft hover:text-ink"
                  }`}
                >
                  {tab.label}
                </span>
              );
            })}
          </div>

          {vendorSubTab === "hall" && <HallPaymentTab />}
        </>
      )}

      {topTab === "product" && <ProductPaymentTab />}
    </div>
  );
};

export default PaymentTab;
