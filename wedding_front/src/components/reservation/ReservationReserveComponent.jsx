import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useSelector } from "react-redux";
import {
  getOne as getCompanyOne,
  getCompanyImageUrl,
} from "../../api/companyApi";
import { postAdd, preparePayment } from "../../api/reservationApi";
import { TOSS_CLIENT_KEY } from "../../api/tossConfig";
import FetchingModal from "../common/FetchingModal";
import {
  categoryLabel,
  buildCompanyOptions,
} from "../../util/companyOptionBuilder";

// 재원 추가 - 업체 상세페이지 "예약" 버튼 → 이 컴포넌트로 진입
// 날짜 + 옵션(홀/드레스/메이크업 패키지) 선택 → 예약 등록 → 결제(토스)창 오픈
// 옵션 목록 구성 로직(buildCompanyOptions)은 업체 찜하기(옵션 선택)에서도 똑같이 써서
// util/companyOptionBuilder.js 로 분리해뒀음

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

const ReservationReserveComponent = () => {
  const { cmno } = useParams();
  const navigate = useNavigate();
  const loginState = useSelector((state) => state.loginSlice);
  // 승진 코드 추가 - mode=reserve(예약만) / mode=pay(결제, 기본값)
  const [searchParams] = useSearchParams();
  const mode = searchParams.get("mode") === "reserve" ? "reserve" : "pay";
  // 승진 코드 추가 끝

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

  const options = useMemo(() => buildCompanyOptions(company), [company]);
  const selectedOption = options.find((o) => o.key === selectedKey);

  const mainImage = company?.uploadFileNames?.[0];

  // 승진 코드 추가 - mode 분기 핸들러
  const handleClickSubmit = async () => {
    if (!weddingDate) {
      alert("예약 날짜를 선택해주세요.");
      return;
    }
    if (options.length > 0 && !selectedOption) {
      alert("옵션을 선택해주세요.");
      return;
    }

    setSubmitting(true);

    try {
      // 1) 예약 등록 (대기 상태)
      const registerResult = await postAdd({
        cmno: Number(cmno),
        memberEmail: loginState.email,
        weddingDate,
        status: "대기",
        memo,
        optionName: selectedOption ? selectedOption.label : "",
        amount: selectedOption ? selectedOption.price : 0,
      });
      const reservationId = registerResult.reservationId;
      // 승진 코드 추가 - postAdd로 저장된 amount 기준으로 결제 여부 판단
      const savedAmount = selectedOption ? selectedOption.price : 0;
      // 승진 코드 추가 끝

      // ── 예약만 하기 모드 ──
      if (mode === "reserve") {
        alert("예약이 등록되었습니다.");
        navigate("/mypage?tab=reservation");
        return;
      }

      // ── 결제 모드 ──
      // 승진 코드 추가 - savedAmount 기준으로 결제 여부 판단 (selectedOption.price 대신)
      if (savedAmount <= 0) {
        alert("예약이 등록되었습니다. (결제 금액이 없어 예약으로 처리됩니다)");
        navigate("/mypage?tab=reservation");
        return;
      }
      // 승진 코드 추가 끝

      // 2) 주문번호 발급
      const prepared = await preparePayment(reservationId);

      // 3) 토스 결제창 오픈
      await loadTossScript();
      const tossPayments = window.TossPayments(TOSS_CLIENT_KEY);

      await tossPayments.requestPayment("카드", {
        amount: prepared.amount || savedAmount,
        orderId: prepared.orderNumber,
        orderName: `${company.name} - ${selectedOption ? selectedOption.label : "예약"}`,
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
  // 승진 코드 추가 끝

  if (fetching || !company) {
    return <FetchingModal />;
  }

  return (
    <div className="max-w-[700px] mx-auto px-4 py-10">
      <p className="mb-1 text-xs text-ink-faint">
        {categoryLabel[company.category] || company.category} {" > "}
        <span className="text-ink-soft">{company.name}</span>
      </p>
      {/* 승진 코드 추가 - mode에 따라 타이틀 변경 */}
      <p className="font-['Gowun_Batang'] text-2xl mb-8 text-ink">
        {mode === "reserve" ? "예약하기" : "결제하기"}
      </p>
      {/* 승진 코드 추가 끝 */}

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
            {/* 승진 코드 추가 - 할인 정보 표시 */}
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
                    {/* 할인율 배지 */}
                    {opt.discountRate > 0 && (
                      <span className="inline-block mt-0.5 text-[11px] font-semibold text-rose-500 bg-rose-50 px-1.5 py-0.5 rounded">
                        {opt.discountRate}% 할인
                      </span>
                    )}
                  </div>
                </div>

                {/* 가격 영역 */}
                <div className="shrink-0 text-right">
                  {opt.discountRate > 0 && opt.originalPrice > 0 && (
                    <p className="text-xs text-ink-faint line-through">
                      {opt.originalPrice.toLocaleString()}원
                    </p>
                  )}
                  <p className={`text-sm font-semibold ${opt.discountRate > 0 ? "text-rose-500" : "text-ink"}`}>
                    {opt.price > 0 ? `${opt.price.toLocaleString()}원` : "-"}
                  </p>
                </div>
              </label>
            ))}
            {/* 승진 코드 추가 끝 */}
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

      {/* 승진 코드 추가 - mode에 따라 버튼 텍스트/스타일 변경 */}
      <button
        type="button"
        onClick={handleClickSubmit}
        disabled={submitting}
        className="w-full h-12 rounded-full bg-brand text-sm font-medium text-white transition hover:bg-brand-deep disabled:opacity-50"
      >
        {submitting
          ? "처리 중..."
          : mode === "reserve"
            ? "예약 등록하기"
            : "업체 결제하기"}
      </button>
      {/* 승진 코드 추가 끝 */}
    </div>
  );
};

export default ReservationReserveComponent;
