import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getListByMember } from "../../api/reservationApi";
import {
  getOne as getCompanyOne,
  getCompanyImageUrl,
} from "../../api/companyApi";
import useCustomLogin from "../../hooks/useCustomLogin";

// 재원 추가 - 마이페이지 "결제내역 > 업체" 탭에서 카테고리별로 재사용
// (Reservation.payStatus === "PAID" 인 것만 결제내역으로 노출)

const CATEGORY_LABEL = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

// 승진 코드 추가
const CATEGORY_ICON = {
  HALL: "🏛",
  DRESS: "👗",
  MAKEUP: "💄",
  STUDIO: "📷",
};

const CATEGORY_COLOR = {
  HALL: "bg-violet-50 text-violet-700",
  DRESS: "bg-pink-50 text-pink-700",
  MAKEUP: "bg-rose-50 text-rose-600",
  STUDIO: "bg-sky-50 text-sky-700",
};
// 승진 코드 추가 끝

const VendorReservationPaymentTab = ({ category }) => {
  const { loginState } = useCustomLogin();
  const navigate = useNavigate();

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!loginState.email) return;

    let cancelled = false;
    setLoading(true);

    getListByMember(loginState.email)
      .then(async (reservations) => {
        const paid = reservations.filter((r) => r.payStatus === "PAID");

        // cmno별로 업체 정보(이름/카테고리/썸네일) 한 번씩만 조회
        const uniqueCmnos = [...new Set(paid.map((r) => r.cmno))];
        const companyMap = {};
        await Promise.all(
          uniqueCmnos.map(async (cmno) => {
            try {
              companyMap[cmno] = await getCompanyOne(cmno);
            } catch {
              companyMap[cmno] = null;
            }
          }),
        );

        // 승진 코드 추가 - category prop 없으면 전체 표시
        const merged = paid
          .map((r) => ({ ...r, company: companyMap[r.cmno] }))
          .filter((r) => !category || r.company?.category === category)
          // 결제일 최신순
          .sort((a, b) => (b.paidAt || "").localeCompare(a.paidAt || ""));
        // 승진 코드 추가 끝

        if (!cancelled) setRows(merged);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [loginState.email, category]);

  if (!loginState.email) {
    return (
      <div className="p-10 text-center text-ink-faint">
        로그인 후 이용해주세요.
      </div>
    );
  }

  if (loading) {
    return (
      <div className="p-10 text-center text-ink-faint">불러오는 중...</div>
    );
  }

  const totalPaid = rows.reduce((s, r) => s + (r.amount || 0), 0);

  return (
    <div>
      {/* 총 결제금액 요약 */}
      <div className="bg-white rounded-2xl border border-line p-5 mb-5">
        <p className="text-xs text-ink-muted mb-1">결제 완료 금액</p>
        <p className="text-xl font-medium text-brand">
          {totalPaid.toLocaleString()}원
        </p>
      </div>

      <p className="text-sm text-ink-muted mb-4">
        {/* 승진 코드 추가 */}
        {category
          ? `${CATEGORY_LABEL[category] || category} 예약 결제 ${rows.length}건`
          : `업체 예약 결제 ${rows.length}건`}
        {/* 승진 코드 추가 끝 */}
      </p>

      {rows.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          결제 완료된 예약이 없습니다.
        </div>
      )}

      {/* 승진 코드 추가 - 카드 그리드 */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {rows.map((r) => {
          const cat = r.company?.category;
          const icon = CATEGORY_ICON[cat] || "🏢";
          const catLabel = CATEGORY_LABEL[cat] || "업체";
          const catColor = CATEGORY_COLOR[cat] || "bg-surface text-ink-muted";
          const thumb = r.company?.uploadFileNames?.[0];

          return (
            <div
              key={r.reservationId}
              onClick={() => navigate(`/companies/read/${r.cmno}`)}
              className="bg-white rounded-2xl border border-line hover:border-brand transition cursor-pointer overflow-hidden"
            >
              {/* 썸네일 */}
              {thumb ? (
                <img
                  alt={r.company?.name}
                  src={getCompanyImageUrl(thumb, true)}
                  className="w-full h-32 object-cover"
                />
              ) : (
                <div className="w-full h-32 bg-blush-50 flex items-center justify-center text-3xl">
                  {icon}
                </div>
              )}

              <div className="p-4">
                {/* 카테고리 뱃지 + 업체명 */}
                <div className="flex items-center gap-2 mb-2">
                  <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${catColor}`}>
                    {icon} {catLabel}
                  </span>
                  <span className="text-[11px] px-2 py-0.5 rounded-full font-medium bg-emerald-50 text-emerald-700">
                    결제완료
                  </span>
                </div>

                <p className="text-sm font-semibold text-ink truncate mb-1">
                  {r.company?.name || `업체 #${r.cmno}`}
                </p>

                {/* 옵션 / 날짜 */}
                <div className="flex flex-col gap-0.5 text-xs text-ink-muted mb-3">
                  {r.optionName && (
                    <span className="truncate">{r.optionName}</span>
                  )}
                  {r.weddingDate && <span>{r.weddingDate}</span>}
                  {r.orderNumber && (
                    <span className="text-ink-faint truncate">
                      주문번호 {r.orderNumber}
                    </span>
                  )}
                </div>

                {/* 금액 */}
                <div className="flex justify-end border-t border-line pt-2.5">
                  <span className="text-sm font-semibold text-ink">
                    {Number(r.amount || 0).toLocaleString()}원
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
      {/* 승진 코드 추가 끝 */}
    </div>
  );
};

export default VendorReservationPaymentTab;
