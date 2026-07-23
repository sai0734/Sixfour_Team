import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  confirmReservationByManager,
  getMyManagedCompanyReservations,
} from "../../api/companyReservationApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import useManagedCompany from "../../hooks/useManagedCompany";
import ManagerReservationDateModal from "./ManagerReservationDateModal";
import { showAlert } from "../../util/globalAlert";
import { showConfirm } from "../../util/globalConfirm";
import {
  WEEKDAY_LABELS,
  buildCalendarDays,
  buildReservationStats,
  formatDisplayDate,
  formatMonthTitle,
  getDateCellState,
  getSortedDatesByFilter,
  groupReservationsByDate,
  isToday,
  parseDateKey,
} from "../../util/managerReservationCalendarUtils";

const CELL_STYLE = {
  empty: "border-line bg-white text-ink hover:border-brand/40",
  pending:
    "border-amber-300 bg-amber-50 text-amber-800 hover:border-amber-400",
  payment: "border-blue-200 bg-blue-50 text-blue-800 hover:border-blue-300",
  closed:
    "border-zinc-300 bg-zinc-200 text-zinc-500 cursor-default line-through decoration-zinc-400",
  other: "border-line bg-surface text-ink-muted hover:border-brand/40",
};

const SIDEBAR_ITEMS = [
  {
    key: "pending",
    label: "예약대기",
    countKey: "pending",
    unit: "건",
    style: "border-amber-200 bg-amber-50 hover:border-amber-400",
    labelStyle: "text-amber-800",
    countStyle: "text-amber-700",
  },
  {
    key: "payment",
    label: "결제대기",
    countKey: "payment",
    unit: "건",
    style: "border-blue-200 bg-blue-50 hover:border-blue-400",
    labelStyle: "text-blue-800",
    countStyle: "text-blue-700",
  },
  {
    key: "closed",
    label: "마감",
    countKey: "closedDates",
    unit: "일",
    style: "border-zinc-300 bg-zinc-100 hover:border-zinc-400",
    labelStyle: "text-zinc-700",
    countStyle: "text-zinc-600",
  },
];

const cellStateToFilter = (cellState) => {
  if (cellState === "pending") return "pending";
  if (cellState === "payment") return "payment";
  if (cellState === "closed") return "closed";
  return "any";
};

const ManagerReservationInbox = () => {
  const navigate = useNavigate();
  const { loginState } = useCustomLogin();
  const { isManager, company, loading } = useManagedCompany({
    enabled: Boolean(loginState.email),
  });

  const [reservations, setReservations] = useState([]);
  const [listLoading, setListLoading] = useState(false);
  const [confirmingId, setConfirmingId] = useState(null);
  const [loadError, setLoadError] = useState(null);

  const today = new Date();
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());
  const [selectedDate, setSelectedDate] = useState(null);
  const [modalFilter, setModalFilter] = useState(null);
  const [navigationDates, setNavigationDates] = useState([]);
  const [navigationIndex, setNavigationIndex] = useState(0);

  const loadReservations = useCallback(async () => {
    if (!company?.cmno) return;

    setListLoading(true);
    setLoadError(null);
    try {
      const data = await getMyManagedCompanyReservations();
      setReservations(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("예약 목록 조회 실패:", err);
      setReservations([]);
      setLoadError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          "예약 목록을 불러오지 못했습니다. 백엔드 재시작 후 다시 시도해주세요.",
      );
    } finally {
      setListLoading(false);
    }
  }, [company?.cmno]);

  useEffect(() => {
    if (!company?.cmno) return;
    loadReservations();
  }, [company?.cmno, loadReservations]);

  const reservationsByDate = useMemo(
    () => groupReservationsByDate(reservations),
    [reservations],
  );

  const stats = useMemo(
    () => buildReservationStats(reservations, reservationsByDate),
    [reservations, reservationsByDate],
  );

  const calendarDays = useMemo(
    () => buildCalendarDays(viewYear, viewMonth),
    [viewYear, viewMonth],
  );

  const selectedReservations = selectedDate
    ? reservationsByDate[selectedDate] || []
    : [];
  const selectedIsClosed =
    selectedDate && getDateCellState(selectedReservations) === "closed";

  const isModalOpen = Boolean(selectedDate);

  // 모달 열림 시 배경 스크롤 잠금 (화살표 클릭 시 페이지 점프 방지)
  useEffect(() => {
    if (!isModalOpen) return undefined;

    const scrollY = window.scrollY;
    const { style } = document.body;
    const prevOverflow = style.overflow;
    const prevPosition = style.position;
    const prevTop = style.top;
    const prevWidth = style.width;

    style.overflow = "hidden";
    style.position = "fixed";
    style.top = `-${scrollY}px`;
    style.width = "100%";

    return () => {
      style.overflow = prevOverflow;
      style.position = prevPosition;
      style.top = prevTop;
      style.width = prevWidth;
      window.scrollTo(0, scrollY);
    };
  }, [isModalOpen]);

  const goToCalendarDate = useCallback((dateKey) => {
    const { year, month } = parseDateKey(dateKey);
    setViewYear(year);
    setViewMonth(month);
    setSelectedDate(dateKey);
  }, []);

  const openDateModal = useCallback(
    (filter, dateKey, datesOverride) => {
      const dates =
        datesOverride ?? getSortedDatesByFilter(reservationsByDate, filter);

      if (!dates.length) {
        showAlert("해당 예약이 없습니다.");
        return;
      }

      const targetDate = dateKey ?? dates[0];
      const idx = Math.max(0, dates.indexOf(targetDate));

      setModalFilter(filter);
      setNavigationDates(dates);
      setNavigationIndex(idx);
      goToCalendarDate(dates[idx]);
    },
    [reservationsByDate, goToCalendarDate],
  );

  const closeModal = () => {
    setSelectedDate(null);
    setModalFilter(null);
    setNavigationDates([]);
    setNavigationIndex(0);
  };

  const moveMonth = (delta) => {
    const scrollY = window.scrollY;
    const next = new Date(viewYear, viewMonth + delta, 1);
    setViewYear(next.getFullYear());
    setViewMonth(next.getMonth());
    requestAnimationFrame(() => {
      window.scrollTo(0, scrollY);
    });
  };

  const handleDateClick = (dateKey, cellState) => {
    if (!dateKey || cellState === "empty") return;
    openDateModal(cellStateToFilter(cellState), dateKey);
  };

  const handleSidebarClick = (filter) => {
    openDateModal(filter);
  };

  const handleModalPrev = () => {
    if (navigationIndex <= 0) return;
    const nextIdx = navigationIndex - 1;
    setNavigationIndex(nextIdx);
    goToCalendarDate(navigationDates[nextIdx]);
  };

  const handleModalNext = () => {
    if (navigationIndex >= navigationDates.length - 1) return;
    const nextIdx = navigationIndex + 1;
    setNavigationIndex(nextIdx);
    goToCalendarDate(navigationDates[nextIdx]);
  };

  const handleConfirm = async (reservationId) => {
    if (!(await showConfirm("예약을 확인하고 결제대기로 전환하시겠습니까?"))) return;

    try {
      setConfirmingId(reservationId);
      await confirmReservationByManager(reservationId);
      const data = await getMyManagedCompanyReservations();
      const nextReservations = Array.isArray(data) ? data : [];
      setReservations(nextReservations);

      const nextByDate = groupReservationsByDate(nextReservations);
      const pendingDates = getSortedDatesByFilter(nextByDate, "pending");

      if (modalFilter === "pending" && pendingDates.length > 0) {
        const currentIdx = pendingDates.indexOf(selectedDate);
        const nextIdx =
          currentIdx >= 0 && currentIdx < pendingDates.length
            ? currentIdx
            : Math.min(navigationIndex, pendingDates.length - 1);

        setNavigationDates(pendingDates);
        setNavigationIndex(nextIdx);
        goToCalendarDate(pendingDates[nextIdx]);
      } else if (modalFilter === "pending" && pendingDates.length === 0) {
        showAlert("모든 예약대기를 확인했습니다.");
        closeModal();
      } else {
        showAlert("예약이 확인되었습니다. 고객이 결제를 진행할 수 있습니다.");
      }
    } catch (err) {
      console.error(err);
      showAlert(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          "예약 확인에 실패했습니다.",
      );
    } finally {
      setConfirmingId(null);
    }
  };

  if (!loginState.email) {
    return (
      <div className="rounded-2xl border border-line bg-white p-8 text-center">
        <p className="text-sm text-ink-muted">
          로그인 후 업체 예약관리를 이용할 수 있습니다.
        </p>
        <button
          type="button"
          className="mt-4 rounded-full border border-brand px-5 py-2 text-sm text-brand transition hover:bg-blush-50"
          onClick={() => navigate("/auth/login")}
        >
          로그인
        </button>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-2xl border border-line bg-white p-8 text-center text-sm text-ink-muted">
        업체 정보를 불러오는 중...
      </div>
    );
  }

  if (!isManager || !company) {
    return (
      <div className="rounded-2xl border border-line bg-white p-8 text-center">
        <p className="text-sm text-ink-muted">
          담당 업체가 없어 예약관리를 이용할 수 없습니다.
        </p>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-ink">{company.name}</p>
          <p className="mt-1 text-xs text-ink-muted">
            전체 {reservations.length}건 · 사이드바를 누르면 가장 빠른 날짜부터
            확인할 수 있습니다
          </p>
        </div>
        <button
          type="button"
          onClick={loadReservations}
          disabled={listLoading}
          className="h-9 rounded-full border border-line-soft px-4 text-xs text-ink-soft transition hover:border-brand hover:text-brand disabled:opacity-50"
        >
          {listLoading ? "새로고침 중..." : "새로고침"}
        </button>
      </div>

      {loadError && (
        <div className="mb-4 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-600">
          {loadError}
        </div>
      )}

      <div className="flex flex-col gap-4 lg:flex-row lg:items-start">
        <div className="min-w-0 flex-1 rounded-2xl border border-line bg-white p-4 sm:p-5">
          <div className="mb-3 flex items-center justify-between">
            <button
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => moveMonth(-1)}
              className="h-8 w-8 rounded-full border border-line-soft text-sm text-ink-soft transition hover:border-brand hover:text-brand"
              aria-label="이전 달"
            >
              ‹
            </button>
            <p className="text-sm font-semibold text-ink">
              {formatMonthTitle(viewYear, viewMonth)}
            </p>
            <button
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => moveMonth(1)}
              className="h-8 w-8 rounded-full border border-line-soft text-sm text-ink-soft transition hover:border-brand hover:text-brand"
              aria-label="다음 달"
            >
              ›
            </button>
          </div>

          <div className="mb-2 grid grid-cols-7 gap-1.5 text-center text-xs font-medium text-ink-muted">
            {WEEKDAY_LABELS.map((label) => (
              <div key={label} className="py-1">
                {label}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-1.5">
            {calendarDays.map((dateKey, idx) => {
              if (!dateKey) {
                return <div key={`empty-${idx}`} className="h-16" />;
              }

              const dayReservations = reservationsByDate[dateKey] || [];
              const cellState = getDateCellState(dayReservations);
              const dayNum = Number(dateKey.split("-")[2]);
              const pendingOnDay = dayReservations.filter(
                (r) => r.status === "대기",
              ).length;
              const isSelected = selectedDate === dateKey;

              if (cellState === "empty") {
                return (
                  <div
                    key={dateKey}
                    className={`relative flex h-16 flex-col items-center justify-center rounded-xl border text-sm ${CELL_STYLE.empty} ${isToday(dateKey) ? "ring-2 ring-brand/30" : ""}`}
                  >
                    <span className="font-medium text-ink-soft">{dayNum}</span>
                  </div>
                );
              }

              return (
                <button
                  key={dateKey}
                  type="button"
                  onClick={() => handleDateClick(dateKey, cellState)}
                  className={`relative flex h-16 flex-col items-center justify-center rounded-xl border text-sm transition ${CELL_STYLE[cellState]} ${isToday(dateKey) ? "ring-2 ring-brand/30" : ""} ${isSelected ? "ring-2 ring-brand" : ""}`}
                >
                  <span className="font-semibold leading-none">{dayNum}</span>
                  {cellState === "pending" && (
                    <span className="mt-1 text-[10px] font-semibold leading-none text-amber-700">
                      대기 {pendingOnDay}
                    </span>
                  )}
                  {cellState === "payment" && (
                    <span className="mt-1 text-[10px] font-semibold leading-none text-blue-700">
                      결제대기
                    </span>
                  )}
                  {cellState === "closed" && (
                    <span className="mt-1 text-[10px] font-semibold leading-none">
                      마감
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        <aside className="w-full shrink-0 lg:w-52">
          <p className="mb-2 text-xs font-semibold text-ink-muted">예약 집계</p>
          <div className="flex flex-col gap-2">
            {SIDEBAR_ITEMS.map((item) => {
              const count = stats[item.countKey] ?? 0;
              const isActive = modalFilter === item.key;

              return (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => handleSidebarClick(item.key)}
                  disabled={count === 0}
                  className={`rounded-2xl border p-4 text-left transition disabled:cursor-not-allowed disabled:opacity-50 ${item.style} ${isActive ? "ring-2 ring-brand/40" : ""}`}
                >
                  <p className={`text-xs font-medium ${item.labelStyle}`}>
                    {item.label}
                  </p>
                  <p className={`mt-1 text-2xl font-semibold ${item.countStyle}`}>
                    {count}
                    <span className="ml-1 text-sm font-medium">{item.unit}</span>
                  </p>
                  {count > 0 && (
                    <p className="mt-2 text-[10px] text-ink-muted">
                      클릭 시 가장 빠른 날짜부터
                    </p>
                  )}
                </button>
              );
            })}
          </div>
        </aside>
      </div>

      {!listLoading && !loadError && reservations.length === 0 && (
        <div className="mt-4 rounded-2xl border border-line bg-white p-8 text-center text-sm text-ink-faint">
          들어온 예약이 없습니다.
        </div>
      )}

      {selectedDate && selectedReservations.length > 0 && (
        <ManagerReservationDateModal
          dateKey={selectedDate}
          dateLabel={formatDisplayDate(selectedDate)}
          reservations={selectedReservations}
          isClosed={selectedIsClosed}
          filterType={modalFilter}
          navIndex={navigationIndex}
          navTotal={navigationDates.length}
          hasPrev={navigationIndex > 0}
          hasNext={navigationIndex < navigationDates.length - 1}
          onPrev={handleModalPrev}
          onNext={handleModalNext}
          onClose={closeModal}
          onConfirm={handleConfirm}
          confirmingId={confirmingId}
        />
      )}
    </div>
  );
};

export default ManagerReservationInbox;
