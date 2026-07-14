import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getListByMember } from "../../api/reservationApi";
import {
  getOne as getCompanyOne,
  getCompanyImageUrl,
} from "../../api/companyApi";
import useCustomLogin from "../../hooks/useCustomLogin";

// 재원 추가 - 마이페이지 "결제내역 > 업체" 탭에서 카테고리별로 재사용
// (Reservation.payStatus === "PAID" 인 것만 결제내역으로 노출)

const categoryLabel = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

const VendorReservationPaymentTab = ({ category }) => {
  const { loginState } = useCustomLogin();

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

        const merged = paid
          .map((r) => ({ ...r, company: companyMap[r.cmno] }))
          .filter((r) => r.company?.category === category)
          // 결제일 최신순
          .sort((a, b) => (b.paidAt || "").localeCompare(a.paidAt || ""));

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
      <div className="bg-white rounded-2xl border border-line p-5 mb-5">
        <p className="text-xs text-ink-muted mb-1">결제 완료 금액</p>
        <p className="text-xl font-medium text-brand">
          {totalPaid.toLocaleString()}원
        </p>
      </div>

      <p className="text-sm text-ink-muted mb-4">
        {categoryLabel[category] || category} 예약 결제 {rows.length}건
      </p>

      {rows.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          결제 완료된 예약이 없습니다.
        </div>
      )}

      <div className="flex flex-col gap-3">
        {rows.map((r) => (
          <Link
            key={r.reservationId}
            to={`/companies/read/${r.cmno}`}
            className="bg-white rounded-2xl border border-line px-5 py-4 flex items-center gap-4 hover:border-brand transition"
          >
            <div className="w-12 h-12 shrink-0 rounded-xl overflow-hidden bg-surface">
              {r.company?.uploadFileNames?.[0] && (
                <img
                  alt={r.company.name}
                  className="w-full h-full object-cover"
                  src={getCompanyImageUrl(r.company.uploadFileNames[0], true)}
                />
              )}
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-sm font-medium text-ink truncate">
                  {r.company?.name || `업체 #${r.cmno}`}
                </span>
                <span className="text-[11px] px-2 py-0.5 rounded-full font-medium bg-green-50 text-green-700 shrink-0">
                  결제완료
                </span>
              </div>
              <p className="text-xs text-ink-faint truncate">
                {r.optionName}
                {r.weddingDate && ` · ${r.weddingDate}`}
              </p>
              <p className="text-[11px] text-ink-faint mt-0.5 truncate">
                주문번호 {r.orderNumber}
              </p>
            </div>

            <span className="shrink-0 text-sm font-medium text-ink">
              {Number(r.amount || 0).toLocaleString()}원
            </span>
          </Link>
        ))}
      </div>
    </div>
  );
};

export default VendorReservationPaymentTab;
