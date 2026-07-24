// 페이지 번호를 현재 페이지 기준으로 5개씩 보여주는 페이지네이션.
// 1페이지 -> 1 2 3 4 5, 7페이지 -> 5 6 7 8 9 (항상 현재 페이지가 가운데 오도록,
// 앞/뒤 끝에서는 5개를 채우기 위해 창을 안쪽으로 당김)
const getPageWindow = (currentPage, totalPages) => {
  let start = currentPage - 2;
  let end = currentPage + 2;

  if (start < 1) {
    end += 1 - start;
    start = 1;
  }

  if (end > totalPages) {
    start -= end - totalPages;
    end = totalPages;
  }

  start = Math.max(1, start);

  const pages = [];
  for (let p = start; p <= end; p++) {
    pages.push(p);
  }
  return pages;
};

const BoardPagination = ({
  currentPage,
  totalItems,
  pageSize,
  onPageChange,
}) => {
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

  if (totalPages <= 1) return null;

  const pages = getPageWindow(currentPage, totalPages);

  return (
    <div className="flex items-center justify-center gap-1.5 mt-8">
      <button
        type="button"
        disabled={currentPage === 1}
        onClick={() => onPageChange(currentPage - 1)}
        className={`h-9 px-3 rounded-full text-sm ${
          currentPage === 1
            ? "text-ink-faint cursor-not-allowed"
            : "text-ink-soft hover:bg-cream"
        }`}
      >
        이전
      </button>

      {pages[0] > 1 && (
        <>
          <button
            type="button"
            onClick={() => onPageChange(1)}
            className="w-9 h-9 rounded-full text-sm text-ink-soft hover:bg-cream"
          >
            1
          </button>
          <span className="text-ink-faint px-1">…</span>
        </>
      )}

      {pages.map((p) => (
        <button
          key={p}
          type="button"
          onClick={() => onPageChange(p)}
          className={`w-9 h-9 rounded-full text-sm font-medium ${
            p === currentPage
              ? "bg-brand text-white"
              : "text-ink-soft hover:bg-cream"
          }`}
        >
          {p}
        </button>
      ))}

      {pages[pages.length - 1] < totalPages && (
        <>
          <span className="text-ink-faint px-1">…</span>
          <button
            type="button"
            onClick={() => onPageChange(totalPages)}
            className="w-9 h-9 rounded-full text-sm text-ink-soft hover:bg-cream"
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        type="button"
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        className={`h-9 px-3 rounded-full text-sm ${
          currentPage === totalPages
            ? "text-ink-faint cursor-not-allowed"
            : "text-ink-soft hover:bg-cream"
        }`}
      >
        다음
      </button>
    </div>
  );
};

export default BoardPagination;
