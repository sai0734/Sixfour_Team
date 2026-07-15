import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import {
  getOne as getCompanyOne,
  getCompanyImageUrl,
} from "../../api/companyApi";
import { postAdd } from "../../api/reservationApi";
import FetchingModal from "../common/FetchingModal";
import {
  categoryLabel,
  buildCompanyOptions,
} from "../../util/companyOptionBuilder";

// 재원 추가 - 업체 상세페이지 "예약" 버튼 → 이 컴포넌트로 진입
// 날짜 + 옵션 선택 → 예약 등록(예약대기) → 업체 확인 후 결제대기 전환

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

  const options = useMemo(() => buildCompanyOptions(company), [company]);
  const selectedOption = options.find((o) => o.key === selectedKey);

  const mainImage = company?.uploadFileNames?.[0];

  // 재원 추가 - 결제 최소 기한(예식일 14일 전) 미리보기 - 서버 값과 동일한 규칙,
  // 날짜 선택 단계에서 미리 안내하기 위한 클라이언트 계산 (최종 판단은 서버가 함).
  // 결제는 업체 확인(결제대기 전환) 이후 마이페이지에서 진행되지만, 가격 있는 옵션을
  // 고르는 시점에 미리 마감일을 보여줘야 사용자가 예약 자체를 이 날짜로 할지 판단할 수 있음
  const PAYMENT_DEADLINE_DAYS = 14;
  const paymentDeadlinePreview = useMemo(() => {
    if (!weddingDate) return null;
    const d = new Date(weddingDate);
    d.setDate(d.getDate() - PAYMENT_DEADLINE_DAYS);
    return d;
  }, [weddingDate]);
  const isPastPaymentDeadline = paymentDeadlinePreview
    ? paymentDeadlinePreview < new Date(new Date().toDateString())
    : false;
  const formatDate = (d) =>
    d
      ? `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(
          d.getDate(),
        ).padStart(2, "0")}`
      : "";
  // 재원 추가 끝

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
      await postAdd({
        cmno: Number(cmno),
        memberEmail: loginState.email,
        weddingDate,
        status: "대기",
        memo,
        optionName: selectedOption ? selectedOption.label : "",
        amount: selectedOption ? selectedOption.price : 0,
      });

      alert(
        selectedOption && selectedOption.price > 0
          ? "예약이 등록되었습니다. 업체 확인 후 결제를 진행할 수 있습니다."
          : "예약이 등록되었습니다.",
      );
      navigate("/mypage?tab=reservation");
    } catch (err) {
      console.error(err);
      alert("예약 등록 중 오류가 발생했습니다.");
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
        {/* 재원 추가 - 결제 최소 기한 안내 (가격 있는 옵션을 골랐을 때만 의미가 있음) */}
        {weddingDate && selectedOption && selectedOption.price > 0 && (
          <p
            className={`mt-2 text-xs ${
              isPastPaymentDeadline ? "text-red-600" : "text-ink-faint"
            }`}
          >
            {isPastPaymentDeadline
              ? "선택하신 예식일은 결제 가능 기한(예식일 14일 전)이 이미 지났어요. 업체 확인 후에도 결제가 제한될 수 있어요."
              : `결제는 업체 확인 후 ${formatDate(paymentDeadlinePreview)}까지 가능해요.`}
          </p>
        )}
        {/* 재원 추가 끝 */}
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
                  <p
                    className={`text-sm font-semibold ${opt.discountRate > 0 ? "text-rose-500" : "text-ink"}`}
                  >
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

      <button
        type="button"
        onClick={handleClickSubmit}
        disabled={submitting}
        className="w-full h-12 rounded-full bg-brand text-sm font-medium text-white transition hover:bg-brand-deep disabled:opacity-50"
      >
        {submitting ? "처리 중..." : "예약 등록하기"}
      </button>
    </div>
  );
};

export default ReservationReserveComponent;
