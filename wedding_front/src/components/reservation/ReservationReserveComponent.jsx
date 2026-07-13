import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import {
  getOne as getCompanyOne,
  getCompanyImageUrl,
} from "../../api/companyApi";
import { postAdd, preparePayment } from "../../api/reservationApi";
import { TOSS_CLIENT_KEY } from "../../api/tossConfig";
import FetchingModal from "../common/FetchingModal";

// 재원 추가 - 업체 상세페이지 "예약" 버튼 → 이 컴포넌트로 진입
// 날짜 + 옵션(홀/드레스/메이크업 패키지) 선택 → 예약 등록 → 결제(토스)창 오픈

const categoryLabel = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

const packageTypeLabel = {
  HAIR: "헤어 패키지",
  MAKEUP: "메이크업 패키지",
  NAIL: "네일 패키지",
  HAIR_MAKEUP: "헤어+메이크업 패키지",
  HAIR_NAIL: "헤어+네일 패키지",
  MAKEUP_NAIL: "메이크업+네일 패키지",
  FULL: "풀 패키지",
};

let tossScriptPromise = null;
const loadTossScript = () => {
  if (tossScriptPromise) return tossScriptPromise;

  tossScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://js.tosspayments.com/v1/payment";
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });

  return tossScriptPromise;
};

// 업체 카테고리별로 선택 가능한 "옵션" 목록을 만들어주는 함수
const buildOptions = (company) => {
  if (!company) return [];

  if (company.category === "HALL") {
    const items =
      company.hallDetail?.items || company.hallDetail?.hallItems || [];
    return items.map((item, idx) => ({
      key: `hall-${idx}`,
      label: item.itemName || `홀 옵션 ${idx + 1}`,
      detail: [
        item.capacity ? `${Number(item.capacity).toLocaleString()}명` : null,
        item.mealType || null,
      ]
        .filter(Boolean)
        .join(" · "),
      price: Number(item.price || 0),
    }));
  }

  if (company.category === "DRESS") {
    const items =
      company.dressDetail?.items || company.dressDetail?.dressItems || [];
    return items.map((item, idx) => ({
      key: `dress-${idx}`,
      label: item.itemName || `드레스 옵션 ${idx + 1}`,
      detail: item.sizeRange || "",
      price: Number(item.price || 0),
    }));
  }

  if (company.category === "MAKEUP") {
    const detail = company.makeupDetail;
    const packages = (detail?.packages || []).filter(Boolean);

    if (packages.length > 0) {
      const priceMap = {
        hair: Number(detail.hairPrice || 0),
        makeup: Number(detail.makeupPrice || 0),
        nail: Number(detail.nailPrice || 0),
      };
      const keysByType = {
        HAIR: ["hair"],
        MAKEUP: ["makeup"],
        NAIL: ["nail"],
        HAIR_MAKEUP: ["hair", "makeup"],
        HAIR_NAIL: ["hair", "nail"],
        MAKEUP_NAIL: ["makeup", "nail"],
        FULL: ["hair", "makeup", "nail"],
      };
      return packages.map((pkg, idx) => {
        const keys = keysByType[pkg.packageType] || [];
        const basePrice = keys.reduce((sum, k) => sum + (priceMap[k] || 0), 0);
        const rate = Number(pkg.discountRate || 0);
        const discountRate = rate > 1 ? rate / 100 : rate;
        const finalPrice =
          basePrice > 0 && discountRate > 0
            ? Math.round(basePrice * (1 - discountRate))
            : basePrice;
        return {
          key: `makeup-pkg-${idx}`,
          label:
            packageTypeLabel[pkg.packageType] ||
            pkg.packageType ||
            `패키지 ${idx + 1}`,
          detail:
            discountRate > 0
              ? `${Math.round(discountRate * 100)}% 할인 적용가`
              : "",
          price: finalPrice,
        };
      });
    }

    // 패키지가 없으면 단품 가격이라도 노출
    const single = [];
    if (detail?.hairPrice)
      single.push({
        key: "hair",
        label: "헤어",
        price: Number(detail.hairPrice),
      });
    if (detail?.makeupPrice)
      single.push({
        key: "makeup",
        label: "메이크업",
        price: Number(detail.makeupPrice),
      });
    if (detail?.nailPrice)
      single.push({
        key: "nail",
        label: "네일",
        price: Number(detail.nailPrice),
      });
    return single.map((s) => ({ ...s, key: `makeup-${s.key}`, detail: "" }));
  }

  // STUDIO 등 별도 아이템 가격이 없는 카테고리 - 대표 가격(priceAvg) 하나로 예약
  if (company.priceAvg) {
    return [
      {
        key: "base",
        label: `${categoryLabel[company.category] || "기본"} 촬영/이용`,
        detail: "",
        price: Number(company.priceAvg),
      },
    ];
  }

  return [];
};

const ReservationReserveComponent = () => {
  const { cmno } = useParams();
  const navigate = useNavigate();
  const loginState = useSelector((state) => state.loginSlice);

  const [company, setCompany] = useState(null);
  const [fetching, setFetching] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [weddingDate, setWeddingDate] = useState("");
  const [selectedKey, setSelectedKey] = useState("");
  const [memo, setMemo] = useState("");

  useEffect(() => {
    if (!loginState.email) {
      alert("로그인이 필요한 기능입니다.");
      navigate("/auth/login");
      return;
    }

    setFetching(true);
    getCompanyOne(cmno)
      .then((data) => {
        setCompany(data);
        setFetching(false);
      })
      .catch((err) => {
        console.error(err);
        setFetching(false);
        alert("업체 정보를 불러오지 못했습니다.");
        navigate(`/companies/read/${cmno}`);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cmno]);

  const options = useMemo(() => buildOptions(company), [company]);
  const selectedOption = options.find((o) => o.key === selectedKey);

  const mainImage = company?.uploadFileNames?.[0];

  const handleClickPay = async () => {
    if (!weddingDate) {
      alert("예약 날짜를 선택해주세요.");
      return;
    }
    if (options.length > 0 && !selectedOption) {
      alert("옵션을 선택해주세요.");
      return;
    }

    setSubmitting(true);

    let reservationId = null;

    try {
      // 1) 예약 등록 (대기 상태, 결제 전)
      const registerResult = await postAdd({
        cmno: Number(cmno),
        memberEmail: loginState.email,
        weddingDate,
        status: "대기",
        memo,
        optionName: selectedOption ? selectedOption.label : "",
        amount: selectedOption ? selectedOption.price : 0,
      });
      reservationId = registerResult.reservationId;

      // 결제 금액이 0원이면(옵션 가격 미등록 업체) 결제 없이 예약만 완료
      if (!selectedOption || selectedOption.price <= 0) {
        alert("예약이 등록되었습니다. (결제 없이 진행되는 예약입니다)");
        navigate(`/companies/read/${cmno}`);
        return;
      }

      // 2) 주문번호 발급
      const prepared = await preparePayment(reservationId);

      // 3) 토스 결제창 오픈
      await loadTossScript();
      const tossPayments = window.TossPayments(TOSS_CLIENT_KEY);

      await tossPayments.requestPayment("카드", {
        amount: prepared.amount,
        orderId: prepared.orderNumber,
        orderName: `${company.name} - ${selectedOption.label}`,
        customerName: loginState.nickname || loginState.email,
        successUrl: `${window.location.origin}/companies/reserve/${cmno}/success?reservationId=${reservationId}`,
        failUrl: `${window.location.origin}/companies/reserve/${cmno}/fail?reservationId=${reservationId}`,
      });
    } catch (err) {
      console.error(err);
      alert("결제가 취소되었거나 오류가 발생했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (fetching || !company) {
    return <FetchingModal />;
  }

  return (
    <div className="max-w-[700px] mx-auto px-4 py-10">
      <p className="mb-1 text-xs text-ink-faint">
        {categoryLabel[company.category] || company.category} {" > "}
        <span className="text-ink-soft">{company.name}</span>
      </p>
      <p className="font-['Gowun_Batang'] text-2xl mb-8 text-ink">예약하기</p>

      <div className="bg-white rounded-2xl p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-5 flex items-center gap-4">
        <div className="w-16 h-16 shrink-0 rounded-xl overflow-hidden bg-surface">
          {mainImage && (
            <img
              alt={company.name}
              className="w-full h-full object-cover"
              src={getCompanyImageUrl(mainImage, true)}
            />
          )}
        </div>
        <div>
          <p className="text-sm font-medium text-ink">{company.name}</p>
          <p className="text-xs text-ink-faint mt-0.5">{company.address}</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-5">
        <p className="text-sm font-medium mb-3 text-brand-deep">예약 날짜</p>
        <input
          type="date"
          value={weddingDate}
          onChange={(e) => setWeddingDate(e.target.value)}
          className="h-11 px-4 border border-line-soft rounded-lg text-sm w-full focus:outline-none focus:border-brand"
        />
      </div>

      <div className="bg-white rounded-2xl p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-5">
        <p className="text-sm font-medium mb-3 text-brand-deep">옵션 선택</p>

        {options.length === 0 ? (
          <p className="text-sm text-ink-faint py-4">
            등록된 옵션이 없어 결제 없이 예약 문의로 진행됩니다.
          </p>
        ) : (
          <div className="flex flex-col gap-2">
            {options.map((opt) => (
              <label
                key={opt.key}
                className={`flex items-center justify-between gap-3 rounded-xl border px-4 py-3 cursor-pointer transition ${
                  selectedKey === opt.key
                    ? "border-brand bg-blush-50"
                    : "border-line-soft hover:border-brand"
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <input
                    type="radio"
                    name="option"
                    checked={selectedKey === opt.key}
                    onChange={() => setSelectedKey(opt.key)}
                  />
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-ink truncate">
                      {opt.label}
                    </p>
                    {opt.detail && (
                      <p className="text-xs text-ink-faint truncate">
                        {opt.detail}
                      </p>
                    )}
                  </div>
                </div>
                <span className="shrink-0 text-sm font-medium text-ink">
                  {opt.price > 0 ? `${opt.price.toLocaleString()}원` : "-"}
                </span>
              </label>
            ))}
          </div>
        )}
      </div>

      <div className="bg-white rounded-2xl p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
        <p className="text-sm font-medium mb-3 text-brand-deep">요청사항</p>
        <textarea
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          placeholder="업체에 전달할 요청사항을 입력해주세요 (선택)"
          rows={3}
          className="p-3 border border-line-soft rounded-lg text-sm resize-none w-full focus:outline-none focus:border-brand"
        />
      </div>

      {selectedOption && selectedOption.price > 0 && (
        <div className="bg-white rounded-2xl p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
          <div className="flex justify-between items-baseline">
            <span className="text-sm text-ink-soft">결제 금액</span>
            <span className="text-xl font-medium text-brand-deep">
              {selectedOption.price.toLocaleString()}원
            </span>
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={handleClickPay}
        disabled={submitting}
        className="w-full h-12 rounded-full bg-brand text-sm font-medium text-white transition hover:bg-brand-deep disabled:opacity-50"
      >
        {submitting
          ? "처리 중..."
          : selectedOption && selectedOption.price > 0
            ? "결제하기"
            : "예약하기"}
      </button>
    </div>
  );
};

export default ReservationReserveComponent;
