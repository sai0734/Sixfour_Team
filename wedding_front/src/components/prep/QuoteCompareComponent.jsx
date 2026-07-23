import { useEffect, useState } from "react";
import {
  uploadQuote,
  listMyQuotes,
  compareQuotes,
  deleteQuote,
  fetchQuoteImageUrl,
} from "../../api/quoteApi";
import { categoryLabel } from "../../util/companyOptionBuilder";

const extractErrorMessage = (err) => {
  const data = err?.response?.data;
  if (typeof data?.msg === "string" && data.msg) return data.msg;
  if (typeof data?.message === "string" && data.message) return data.message;
  if (typeof data?.detail === "string" && data.detail) return data.detail;
  if (err?.response?.status === 401) {
    return "로그인이 만료되었습니다. 다시 로그인해 주세요.";
  }
  return err?.message || "요청 처리에 실패했습니다.";
};

const formatWon = (value) => {
  if (value === null || value === undefined) return "정보 없음";
  return `${Number(value).toLocaleString()}원`;
};

// 견적서 이미지는 소유권 체크가 필요해서 <img src="..."> 에 URL을 직접 못 박는다.
// 각 썸네일마다 별도로 인증된 요청을 보내 blob object URL을 받아서 렌더링한다.
const QuoteThumbnail = ({ quoteId }) => {
  const [url, setUrl] = useState("");

  useEffect(() => {
    let objectUrl = "";
    let cancelled = false;

    fetchQuoteImageUrl(quoteId)
      .then((u) => {
        if (cancelled) {
          URL.revokeObjectURL(u);
          return;
        }
        objectUrl = u;
        setUrl(u);
      })
      .catch(() => {});

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [quoteId]);

  if (!url) {
    return (
      <div className="flex aspect-[3/4] w-full items-center justify-center rounded-lg bg-surface text-[11px] text-ink-faint">
        불러오는 중...
      </div>
    );
  }

  return (
    <img
      src={url}
      alt="견적서"
      className="aspect-[3/4] w-full rounded-lg object-cover"
    />
  );
};

const QuoteCompareComponent = () => {
  const [quotes, setQuotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [selectedIds, setSelectedIds] = useState([]);
  const [comparing, setComparing] = useState(false);
  const [compareResult, setCompareResult] = useState(null);

  const loadQuotes = () => {
    setLoading(true);
    listMyQuotes()
      .then(setQuotes)
      .catch((err) => setMessage(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadQuotes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";

    setUploading(true);
    setMessage("견적서 사진을 분석하는 중... (10~20초 정도 걸려요)");
    try {
      await uploadQuote(file);
      setMessage("업로드 완료! 아래 목록에 추가됐어요.");
      loadQuotes();
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  // 카테고리가 다르면 두 번째로 선택 못 하게 막는다 - 비교 API 자체도 서버에서 막지만,
  // 프론트에서 먼저 걸러야 "왜 안 눌리지?" 하는 혼란이 없다.
  const toggleSelect = (quote) => {
    setCompareResult(null);
    setSelectedIds((prev) => {
      if (prev.includes(quote.quoteId)) {
        return prev.filter((id) => id !== quote.quoteId);
      }
      if (prev.length === 0) {
        return [quote.quoteId];
      }
      const first = quotes.find((q) => q.quoteId === prev[0]);
      if (first && first.category !== quote.category) {
        setMessage("같은 종류의 견적서끼리만 비교할 수 있어요.");
        return prev;
      }
      if (prev.length >= 2) {
        return [prev[1], quote.quoteId];
      }
      return [...prev, quote.quoteId];
    });
  };

  const handleCompare = async () => {
    if (selectedIds.length !== 2) return;
    setComparing(true);
    setMessage("");
    try {
      const result = await compareQuotes(selectedIds[0], selectedIds[1]);
      setCompareResult(result);
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setComparing(false);
    }
  };

  const handleDelete = async (quoteId) => {
    if (!window.confirm("이 견적서를 삭제할까요?")) return;
    try {
      await deleteQuote(quoteId);
      setSelectedIds((prev) => prev.filter((id) => id !== quoteId));
      setCompareResult(null);
      loadQuotes();
    } catch (err) {
      setMessage(extractErrorMessage(err));
    }
  };

  // 비교 결과 화면의 견적서 1개 카드 - 승자/순위 표시는 절대 넣지 않는다(사실 나열만).
  const renderQuoteColumn = (quote) => (
    <div className="rounded-2xl border border-line bg-white p-4">
      <p className="mb-2 text-sm font-semibold text-ink">
        {categoryLabel[quote.category]} ·{" "}
        {quote.vendorNameGuess || "업체명 미상"}
      </p>
      <p className="mb-1 text-sm text-ink">
        총액: {formatWon(quote.totalPrice)}
      </p>
      {quote.perGuestPrice != null && (
        <p className="mb-3 text-xs text-ink-muted">
          인당 {formatWon(quote.perGuestPrice)}
        </p>
      )}
      {quote.items.length > 0 && (
        <ul className="mb-3 space-y-1 text-xs text-ink-soft">
          {quote.items.map((item, idx) => (
            <li key={idx} className="flex justify-between gap-2">
              <span>
                {item.name}
                {item.includedInTotal ? " (포함)" : ""}
              </span>
              <span>{item.price != null ? formatWon(item.price) : "-"}</span>
            </li>
          ))}
        </ul>
      )}
      {quote.hiddenNotes.length > 0 && (
        <div className="rounded-lg bg-amber-50 p-2.5 text-[11px] text-amber-800">
          {quote.hiddenNotes.map((note, idx) => (
            <p key={idx}>· {note}</p>
          ))}
        </div>
      )}
    </div>
  );

  return (
    <div className="space-y-5">
      <section className="rounded-2xl border border-line bg-white p-5">
        <p className="mb-1 text-sm font-medium text-ink">견적서 업로드</p>
        <p className="mb-3 text-xs text-ink-faint">
          홀/스튜디오/드레스/메이크업 견적서 사진(JPG/PNG)을 올리면 AI가 항목을
          정리해요. 업로드 후 30일이 지나면 자동으로 삭제돼요.
        </p>
        <label className="inline-flex cursor-pointer rounded-full bg-brand px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-dark">
          {uploading ? "분석 중..." : "사진 선택"}
          <input
            type="file"
            accept="image/jpeg,image/png"
            className="hidden"
            disabled={uploading}
            onChange={handleUpload}
          />
        </label>
        {message && <p className="mt-3 text-xs text-ink-muted">{message}</p>}
      </section>

      <section className="rounded-2xl border border-line bg-white p-5">
        <div className="mb-3 flex items-center justify-between">
          <p className="text-sm font-medium text-ink">
            내 견적서 ({quotes.length})
          </p>
          {selectedIds.length === 2 && (
            <button
              type="button"
              onClick={handleCompare}
              disabled={comparing}
              className="rounded-full bg-brand px-4 py-1.5 text-xs font-medium text-white disabled:opacity-50"
            >
              {comparing ? "비교하는 중..." : "두 견적 비교하기"}
            </button>
          )}
        </div>

        {loading ? (
          <p className="text-sm text-ink-faint">불러오는 중...</p>
        ) : quotes.length === 0 ? (
          <p className="text-sm text-ink-faint">
            아직 업로드한 견적서가 없어요.
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {quotes.map((quote) => {
              const selected = selectedIds.includes(quote.quoteId);
              return (
                <div
                  key={quote.quoteId}
                  className={`overflow-hidden rounded-xl border p-2 ${
                    selected
                      ? "border-brand ring-2 ring-brand/30"
                      : "border-line"
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => toggleSelect(quote)}
                    className="block w-full text-left"
                  >
                    <QuoteThumbnail quoteId={quote.quoteId} />
                    <p className="mt-1.5 truncate text-xs font-medium text-ink">
                      {categoryLabel[quote.category]}
                    </p>
                    <p className="truncate text-[11px] text-ink-faint">
                      {quote.vendorNameGuess || "업체명 미상"}
                    </p>
                    <p className="text-[11px] text-ink-muted">
                      {formatWon(quote.totalPrice)}
                    </p>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(quote.quoteId)}
                    className="mt-1.5 w-full rounded-full border border-red-200 py-1 text-[10px] text-red-600 hover:border-red-400"
                  >
                    삭제
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {compareResult && (
        <section className="rounded-2xl border border-brand-dark bg-blush-50 p-5">
          <p className="mb-3 text-sm font-semibold text-brand-deep">
            비교 결과
          </p>

          <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {compareResult.priceDifference && (
              <div className="rounded-xl bg-white p-3 text-xs text-ink">
                <p className="mb-1 font-medium text-ink">가격 차이</p>
                <p>{compareResult.priceDifference}</p>
              </div>
            )}
            {compareResult.commonNotes.length > 0 && (
              <div className="rounded-xl bg-white p-3 text-xs text-ink">
                <p className="mb-1 font-medium text-ink">두 견적서 공통 특징</p>
                {compareResult.commonNotes.map((n, idx) => (
                  <p key={idx}>· {n}</p>
                ))}
              </div>
            )}
            {compareResult.conditionDifferences.length > 0 && (
              <div className="rounded-xl bg-white p-3 text-xs text-ink">
                <p className="mb-1 font-medium text-ink">조건 차이</p>
                {compareResult.conditionDifferences.map((n, idx) => (
                  <p key={idx}>· {n}</p>
                ))}
              </div>
            )}
            {compareResult.suggestedQuestions.length > 0 && (
              <div className="rounded-xl bg-white p-3 text-xs text-ink">
                <p className="mb-1 font-medium text-ink">
                  업체에 확인해볼 질문
                </p>
                {compareResult.suggestedQuestions.map((n, idx) => (
                  <p key={idx}>· {n}</p>
                ))}
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              {renderQuoteColumn(compareResult.quoteA)}
              {compareResult.onlyInA.length > 0 && (
                <p className="mt-2 text-[11px] text-ink-muted">
                  이 견적서에만 있음: {compareResult.onlyInA.join(", ")}
                </p>
              )}
            </div>
            <div>
              {renderQuoteColumn(compareResult.quoteB)}
              {compareResult.onlyInB.length > 0 && (
                <p className="mt-2 text-[11px] text-ink-muted">
                  이 견적서에만 있음: {compareResult.onlyInB.join(", ")}
                </p>
              )}
            </div>
          </div>
        </section>
      )}
    </div>
  );
};

export default QuoteCompareComponent;
