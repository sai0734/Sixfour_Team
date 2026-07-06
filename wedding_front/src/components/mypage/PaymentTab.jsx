import { useState } from "react";
import HallPaymentTab from "../hallpayment/HallPaymentTab";

const VENDOR_SUBTABS = [
  { key: "hall", label: "홀", enabled: true },
  { key: "studio", label: "스튜디오", enabled: false },
  { key: "dress", label: "드레스", enabled: false },
  { key: "makeup", label: "메이크업", enabled: false },
];

const PaymentTab = () => {
  const [topTab, setTopTab] = useState("vendor"); // "vendor" | "product"
  const [vendorSubTab, setVendorSubTab] = useState("hall");

  return (
    <div>
      <div className="flex gap-2 mb-5">
        <button
          type="button"
          onClick={() => setTopTab("vendor")}
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
          onClick={() => setTopTab("product")}
          title="준비 중인 기능입니다"
          className="h-9 px-5 rounded-full text-sm font-medium bg-surface text-ink-faint cursor-not-allowed"
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
                  onClick={() => setVendorSubTab(tab.key)}
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

      {topTab === "product" && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          준비 중인 기능입니다.
        </div>
      )}
    </div>
  );
};

export default PaymentTab;
