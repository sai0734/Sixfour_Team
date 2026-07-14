import { useSearchParams } from "react-router-dom";
import HallPaymentTab from "../hallpayment/HallPaymentTab";
import ProductPaymentTab from "./ProductPaymentTab";
import VendorReservationPaymentTab from "./VendorReservationPaymentTab";

// 재원 수정 - 업체 쪽(스튜디오/드레스/메이크업)은 예약 결제 기능이 없어서 막아뒀었는데,
// 이제 예약(Reservation) 결제가 붙었으니 전부 활성화
const VENDOR_SUBTABS = [
  { key: "hall", label: "홀", category: "HALL" },
  { key: "studio", label: "스튜디오", category: "STUDIO" },
  { key: "dress", label: "드레스", category: "DRESS" },
  { key: "makeup", label: "메이크업", category: "MAKEUP" },
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
            {VENDOR_SUBTABS.map((tab) => (
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
            ))}
          </div>

          {vendorSubTab === "hall" && (
            <>
              <VendorReservationPaymentTab category="HALL" />
              <div className="mt-10 pt-8 border-t border-line">
                <p className="text-sm font-medium text-brand-deep mb-4">
                  계약금 · 잔금 납부 관리
                </p>
                <HallPaymentTab />
              </div>
            </>
          )}
          {vendorSubTab === "studio" && (
            <VendorReservationPaymentTab category="STUDIO" />
          )}
          {vendorSubTab === "dress" && (
            <VendorReservationPaymentTab category="DRESS" />
          )}
          {vendorSubTab === "makeup" && (
            <VendorReservationPaymentTab category="MAKEUP" />
          )}
        </>
      )}

      {topTab === "product" && <ProductPaymentTab />}
    </div>
  );
};

export default PaymentTab;
