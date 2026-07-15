export const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

export const toDateKey = (value) => {
  if (!value) return "";
  return String(value).slice(0, 10);
};

export const groupReservationsByDate = (reservations) => {
  const map = {};

  reservations.forEach((r) => {
    const key = toDateKey(r.weddingDate);
    if (!key) return;
    if (!map[key]) map[key] = [];
    map[key].push(r);
  });

  return map;
};

/** empty | closed | pending | payment | other */
export const getDateCellState = (reservationsOnDate) => {
  if (!reservationsOnDate?.length) return "empty";

  if (
    reservationsOnDate.some(
      (r) => r.status === "확정" || r.payStatus === "PAID",
    )
  ) {
    return "closed";
  }

  if (reservationsOnDate.some((r) => r.status === "대기")) return "pending";
  if (reservationsOnDate.some((r) => r.status === "결제대기")) return "payment";

  return "other";
};

export const buildCalendarDays = (year, month) => {
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startPad = firstDay.getDay();
  const daysInMonth = lastDay.getDate();
  const cells = [];

  for (let i = 0; i < startPad; i += 1) cells.push(null);

  for (let day = 1; day <= daysInMonth; day += 1) {
    const mm = String(month + 1).padStart(2, "0");
    const dd = String(day).padStart(2, "0");
    cells.push(`${year}-${mm}-${dd}`);
  }

  return cells;
};

export const formatMonthTitle = (year, month) =>
  `${year}년 ${month + 1}월`;

export const formatDisplayDate = (dateKey) => {
  if (!dateKey) return "";
  const [y, m, d] = dateKey.split("-");
  return `${y}년 ${Number(m)}월 ${Number(d)}일`;
};

export const isToday = (dateKey) => {
  const today = new Date();
  const key = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  return dateKey === key;
};

/** pending | payment | closed | any */
export const getSortedDatesByFilter = (reservationsByDate, filter) => {
  const dates = Object.keys(reservationsByDate).filter((dateKey) => {
    const list = reservationsByDate[dateKey];
    if (!list?.length) return false;

    if (filter === "pending") {
      return list.some((r) => r.status === "대기");
    }
    if (filter === "payment") {
      return list.some((r) => r.status === "결제대기");
    }
    if (filter === "closed") {
      return getDateCellState(list) === "closed";
    }

    return true;
  });

  return dates.sort();
};

export const parseDateKey = (dateKey) => {
  const [y, m] = dateKey.split("-").map(Number);
  return { year: y, month: m - 1 };
};

export const buildReservationStats = (reservations, reservationsByDate) => ({
  pending: reservations.filter((r) => r.status === "대기").length,
  payment: reservations.filter((r) => r.status === "결제대기").length,
  closedDates: getSortedDatesByFilter(reservationsByDate, "closed").length,
  closedReservations: reservations.filter(
    (r) => r.status === "확정" || r.payStatus === "PAID",
  ).length,
});

