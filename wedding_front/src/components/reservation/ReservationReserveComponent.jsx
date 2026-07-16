import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import {
  getOne as getCompanyOne,
  getCompanyImageUrl,
} from "../../api/companyApi";
import { postAdd, checkAvailability } from "../../api/reservationApi";
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

  // 재원 추가 - 옵션 이미지 표시 + 페이징 (홀/드레스만 - 이미지가 있는 카테고리)
  const isImageCategory =
    company?.category === "HALL" || company?.category === "DRESS";
  const OPTIONS_PAGE_SIZE = 4;
  const [optionPage, setOptionPage] = useState(1);

  useEffect(() => {
    setOptionPage(1);
  }, [company?.cmno]);

  const totalOptionPages = isImageCategory
    ? Math.max(1, Math.ceil(options.length / OPTIONS_PAGE_SIZE))
    : 1;

  const visibleOptions = isImageCategory
    ? options.slice(
        (optionPage - 1) * OPTIONS_PAGE_SIZE,
        optionPage * OPTIONS_PAGE_SIZE,
      )
    : options;
  // 재원 추가 끝

  // 재원 수정 - 결제 최소 기한(예식일 14일 전) 안내는 마이페이지 예약현황 카드로 옮김.
  // 여기서는 애초에 14일 이내 날짜를 고를 수 없도록 date input 자체를 막음
  const PAYMENT_DEADLINE_DAYS = 14;
  const minWeddingDate = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() + PAYMENT_DEADLINE_DAYS);
    return d.toISOString().slice(0, 10);
  }, []);
  const formatDate = (d) =>
    d
      ? `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(
          d.getDate(),
        ).padStart(2, "0")}`
      : "";
  // 재원 수정 끝

  // 재원 추가 - 날짜 + 옵션이 둘 다 정해지면 이미 예약된 조합인지 미리 확인
  const [dateTaken, setDateTaken] = useState(false);
  const [checkingDate, setCheckingDate] = useState(false);

  useEffect(() => {
    if (!weddingDate || !selectedOption || !selectedOption.label) {
      setDateTaken(false);
      return;
    }

    let cancelled = false;
    setCheckingDate(true);

    checkAvailability(cmno, selectedOption.label, weddingDate)
      .then((taken) => {
        if (!cancelled) setDateTaken(taken);
      })
      .catch((err) => {
        console.error("예약 가능 여부 확인 실패:", err);
        if (!cancelled) setDateTaken(false); // 확인 실패 시엔 일단 막지 않음, 최종 방어는 서버가 함
      })
      .finally(() => {
        if (!cancelled) setCheckingDate(false);
      });

    return () => {
      cancelled = true;
    };
  }, [cmno, weddingDate, selectedOption?.label]);
  // 재원 추가 끝

  const handleClickSubmit = async () => {
    if (!weddingDate) {
      alert("예약 날짜를 선택해주세요.");
      return;
    }
    if (weddingDate < minWeddingDate) {
      alert(
        `예식일은 ${formatDate(new Date(minWeddingDate))} 이후로 선택해주세요.`,
      );
      return;
    }
    if (options.length > 0 && !selectedOption) {
      alert("옵션을 선택해주세요.");
      return;
    }
    // 재원 추가 - 중복 예약 최종 확인 (버튼 비활성화를 우회해도 여기서 한 번 더 막음)
    if (dateTaken) {
      alert("이미 예약된 날짜입니다. 다른 날짜를 선택해주세요.");
      return;
    }
    // 재원 추가 끝

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
      // 재원 수정 - 서버가 막은 실제 이유(중복 예약 등)를 그대로 보여줌
      alert(err.response?.data?.msg || "예약 등록 중 오류가 발생했습니다.");
      // 재원 수정 끝
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
          min={minWeddingDate}
          onChange={(e) => setWeddingDate(e.target.value)}
          className="h-11 px-4 border border-line-soft rounded-lg text-sm w-full focus:outline-none focus:border-brand"
        />
        {/* 재원 추가 - 최소 선택 가능일 안내 */}
        <p className="mt-2 text-xs text-ink-faint">
          예식일이 결제 최소 기한(14일)보다 가까우면 결제가 제한될 수 있어
          {formatDate(new Date(minWeddingDate))} 이후 날짜만 선택할 수 있어요.
        </p>
        {/* 재원 추가 끝 */}
        {/* 재원 추가 - 중복 예약(같은 옵션+같은 날짜) 확인 결과 */}
        {selectedOption && weddingDate && checkingDate && (
          <p className="mt-2 text-xs text-ink-faint">확인 중...</p>
        )}
        {selectedOption && weddingDate && !checkingDate && dateTaken && (
          <p className="mt-2 text-xs text-red-600 font-medium">
            이미 다른 회원이 예약한 날짜예요. 다른 날짜를 선택해주세요.
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
            {visibleOptions.map((opt) => (
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
                  {/* 재원 추가 - 옵션 이미지 썸네일 (홀/드레스만 image 값이 있어 자연스럽게 그 둘에서만 보임) */}
                  {opt.image && (
                    <div className="w-12 h-12 shrink-0 rounded-lg overflow-hidden bg-surface">
                      <img
                        alt={opt.label}
                        className="w-full h-full object-cover"
                        src={getCompanyImageUrl(opt.image, true)}
                      />
                    </div>
                  )}
                  {/* 재원 추가 끝 */}
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

            {/* 재원 추가 - 옵션 목록 페이징 (홀/드레스 + 옵션이 페이지 크기보다 많을 때만) */}
            {isImageCategory && totalOptionPages > 1 && (
              <div className="flex items-center justify-center gap-3 mt-2">
                <button
                  type="button"
                  onClick={() => setOptionPage((p) => Math.max(1, p - 1))}
                  disabled={optionPage === 1}
                  className="w-8 h-8 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream disabled:opacity-40"
                >
                  ‹
                </button>
                <span className="text-xs text-ink-faint">
                  {optionPage} / {totalOptionPages}
                </span>
                <button
                  type="button"
                  onClick={() =>
                    setOptionPage((p) => Math.min(totalOptionPages, p + 1))
                  }
                  disabled={optionPage === totalOptionPages}
                  className="w-8 h-8 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream disabled:opacity-40"
                >
                  ›
                </button>
              </div>
            )}
            {/* 재원 추가 끝 */}
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
        disabled={submitting || checkingDate || dateTaken}
        className="w-full h-12 rounded-full bg-brand text-sm font-medium text-white transition hover:bg-brand-deep disabled:opacity-50"
      >
        {submitting
          ? "처리 중..."
          : dateTaken
            ? "이미 예약된 날짜입니다"
            : "예약 등록하기"}
      </button>
    </div>
  );
};

export default ReservationReserveComponent;
