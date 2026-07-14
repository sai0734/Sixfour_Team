import { useSearchParams } from "react-router-dom";
import ProductPaymentTab from "./ProductPaymentTab";
import VendorReservationPaymentTab from "./VendorReservationPaymentTab";

// 재원 수정 - 업체 쪽(스튜디오/드레스/메이크업)은 예약 결제 기능이 없어서 막아뒀었는데,
// 이제 예약(Reservation) 결제가 붙었으니 전부 활성화

const PaymentTab = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // 상품 상세로 갔다가 뒤로가기 눌러도 "결제내역 > 답례품 쇼핑몰"로 그대로
  // 돌아오도록, 탭 상태를 컴포넌트 state가 아니라 URL(?psub=)에 둠
  const topTab = searchParams.get("psub") === "product" ? "product" : "vendor";

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
      {/* 승진 코드 추가 - 상위 탭만 유지 (업체 / 답례품 쇼핑몰) */}
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
        /* 홀/스튜디오/드레스/메이크업 전체 카드 그리드 */
        <VendorReservationPaymentTab />
      )}
      {/* 승진 코드 추가 끝 */}

      {topTab === "product" && <ProductPaymentTab />}
    </div>
  );
};

export default PaymentTab;
