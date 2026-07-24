import { useEffect, useState } from "react";
import {
  uploadQuote,
  listMyQuotes,
  compareQuotes,
  listComparisons,
  deleteComparison,
  deleteQuote,
  fetchQuoteImageUrl,
} from "../../api/quoteApi";
import { categoryLabel } from "../../util/companyOptionBuilder";
import { showConfirm } from "../../util/globalConfirm";

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

const formatDate = (value) => {
  if (!value) return "";
  try {
    return new Date(value).toLocaleString("ko-KR", {
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return "";
  }
};

// 비교 기록 배지에 쓸 라벨 - "웨딩홀 · 7/23 오후 04:25" 같은 카테고리+시간 대신, 사용자가
// 한눈에 "이게 어느 견적서끼리 비교한 거였는지" 알아볼 수 있게 실제 업체명 두 개로 보여준다.
const comparisonLabel = (comparison) => {
  const nameA = comparison.quoteA?.vendorNameGuess || "업체명 미상";
  const nameB = comparison.quoteB?.vendorNameGuess || "업체명 미상";
  return `${nameA} vs ${nameB}`;
};

// 견적서 이미지는 소유권 체크가 필요해서 <img src="/api/...">로 바로 못 박고, jwtAxios로 인증
// 요청 후 받은 blob을 로컬 object URL로 바꿔서 써야 한다 - 그래서 컴포넌트 하나로 분리했다.
const QuoteThumbnail = ({ quoteId }) => {
  const [url, setUrl] = useState("");

  useEffect(() => {
    let objUrl = "";
    let cancelled = false;
    fetchQuoteImageUrl(quoteId)
      .then((u) => {
        if (cancelled) {
          URL.revokeObjectURL(u);
          return;
        }
        objUrl = u;
        setUrl(u);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
      if (objUrl) URL.revokeObjectURL(objUrl);
    };
  }, [quoteId]);

  if (!url) {
    return (
      <div className="flex aspect-[3/4] w-full items-center justify-center rounded-lg bg-surface text-[10px] text-ink-faint">
        불러오는 중
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

  // 비교 기록 - 새로고침/다른 페이지 갔다와도 서버에서 다시 불러와서 그대로 복원한다
  // (AI웨딩플랜의 히스토리 배지와 같은 사상 - 로컬 상태가 아니라 서버가 진실의 원천).
  const [comparisons, setComparisons] = useState([]);
  const [activeComparisonId, setActiveComparisonId] = useState(null);

  const activeComparison = comparisons.find(
    (c) => c.comparisonId === activeComparisonId,
  );

  const loadQuotes = () => {
    setLoading(true);
    listMyQuotes()
      .then(setQuotes)
      .catch((err) => setMessage(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  };

  const loadComparisons = () => {
    listComparisons()
      .then((data) => {
        setComparisons(data);
        if (data.length > 0) {
          setActiveComparisonId((prev) => prev ?? data[0].comparisonId);
        }
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadQuotes();
    loadComparisons();
  }, []);

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";

    setUploading(true);
    setMessage("견적서 사진을 분석하는 중... (10~20초 정도 걸려요)");
    try {
      await uploadQuote(file);
      setMessage("업로드 완료! 아래에서 견적서 2개를 눌러 비교해보세요.");
      loadQuotes();
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  // 같은 카테고리끼리만 최대 2개 선택 가능 - 카테고리 다른 걸 누르면 안내만 하고 무시
  const toggleSelect = (quote) => {
    setSelectedIds((prev) => {
      if (prev.includes(quote.quoteId)) {
        return prev.filter((id) => id !== quote.quoteId);
      }
      if (prev.length === 0) {
        return [quote.quoteId];
      }
      const first = quotes.find((q) => q.quoteId === prev[0]);
      if (first && first.category !== quote.category) {
        setMessage("같은 업체의 견적서끼리만 비교할 수 있어요.");
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
      setComparisons((prev) => [result, ...prev]);
      setActiveComparisonId(result.comparisonId);
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setComparing(false);
    }
  };

  // 삭제 확인창은 브라우저 기본 confirm() 대신 다른 페이지(AI웨딩플랜 등)와 동일한
  // 전역 확인 모달(showConfirm)을 쓴다 - Promise<boolean>이라 반드시 await로 받아야 함.
  const handleDelete = async (quoteId) => {
    if (!(await showConfirm("이 견적서를 삭제할까요?"))) return;
    try {
      await deleteQuote(quoteId);
      setSelectedIds((prev) => prev.filter((id) => id !== quoteId));
      loadQuotes();
    } catch (err) {
      setMessage(extractErrorMessage(err));
    }
  };

  // 히스토리 배지 - 이미 선택된 배지를 다시 누르면 해제(토글)돼서 비교 결과 카드가 사라짐
  const handleBadgeClick = (comparisonId) => {
    setActiveComparisonId((prev) =>
      prev === comparisonId ? null : comparisonId,
    );
  };

  // 배지의 작은 x 버튼 - 비교 기록 자체를 삭제. 배지 클릭(토글)과 겹치지 않게 stopPropagation.
  const handleDeleteComparison = async (e, comparisonId) => {
    e.stopPropagation();
    if (!(await showConfirm("이 비교 기록을 삭제할까요?"))) return;
    try {
      await deleteComparison(comparisonId);
      setComparisons((prev) =>
        prev.filter((c) => c.comparisonId !== comparisonId),
      );
      setActiveComparisonId((prev) => (prev === comparisonId ? null : prev));
    } catch (err) {
      setMessage(extractErrorMessage(err));
    }
  };

  // 견적서 한쪽 카드 - 항목이 많아도 페이지 전체가 늘어지지 않게 목록 안에서만 스크롤되게 함
  const renderQuoteColumn = (quote) => (
    <div className="rounded-xl border border-line bg-white p-3">
      <p className="mb-1.5 truncate text-sm font-semibold text-ink">
        {categoryLabel[quote.category]} ·{" "}
        {quote.vendorNameGuess || "업체명 미상"}
      </p>
      <p className="mb-0.5 text-sm text-ink">
        총액: {formatWon(quote.totalPrice)}
      </p>
      {quote.perGuestPrice != null && (
        <p className="mb-2 text-xs text-ink-muted">
          인당 {formatWon(quote.perGuestPrice)}
        </p>
      )}
      <ul className="mb-2 max-h-40 space-y-1 overflow-y-auto pr-1 text-xs text-ink-soft">
        {quote.items.map((item, idx) => (
          <li key={idx} className="flex justify-between gap-2">
            <span className="truncate">
              {item.name}
              {item.includedInTotal ? " (포함)" : ""}
            </span>
            <span className="shrink-0">
              {item.price != null ? formatWon(item.price) : "-"}
            </span>
          </li>
        ))}
      </ul>
      {quote.hiddenNotes.length > 0 && (
        <div className="rounded-lg bg-amber-50 p-2 text-[11px] leading-relaxed text-amber-800">
          {quote.hiddenNotes.map((note, idx) => (
            <p key={idx}>· {note}</p>
          ))}
        </div>
      )}
    </div>
  );

  return (
    <div className="space-y-4">
      {/* 비교 기록 배지 - 맨 위, 짧게. 업체명 두 개로 라벨을 보여줘서 어느 견적서끼리 비교한
          건지 한눈에 알아볼 수 있게 함. 배지를 누르면 토글, 오른쪽 작은 x로 기록 자체를 삭제 */}
      {comparisons.length > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          <span className="shrink-0 text-xs font-medium text-ink-muted">
            비교 기록
          </span>
          {comparisons.map((c) => {
            const active = c.comparisonId === activeComparisonId;
            const label = comparisonLabel(c);
            const tooltip = active
              ? "다시 누르면 결과가 접혀요"
              : `${label} · ${formatDate(c.regDate)}`;
            return (
              <div key={c.comparisonId} className="relative shrink-0">
                <button
                  type="button"
                  onClick={() => handleBadgeClick(c.comparisonId)}
                  title={tooltip}
                  className={`whitespace-nowrap rounded-full border py-1.5 pl-3 pr-6 text-xs font-medium transition ${
                    active
                      ? "border-brand-dark bg-blush-100 text-brand-deep"
                      : "border-line text-ink-soft hover:bg-blush-50"
                  }`}
                >
                  {label}
                </button>
                <button
                  type="button"
                  onClick={(e) => handleDeleteComparison(e, c.comparisonId)}
                  title="비교 기록 삭제"
                  className="absolute right-1.5 top-1/2 flex h-4 w-4 -translate-y-1/2 items-center justify-center rounded-full text-[11px] leading-none text-ink-faint hover:bg-red-100 hover:text-red-600"
                >
                  ×
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* 비교 결과 - 이 페이지의 메인 콘텐츠. 항상 이 자리에 있고, 없으면 안내 문구만 표시 */}
      {activeComparison ? (
        <section className="rounded-2xl border border-brand-dark bg-blush-50 p-5">
          <h2 className="mb-4 font-['Gowun_Batang'] text-xl text-ink">
            💰 비교 결과
          </h2>

          {/* 요약 카드 2x2 - 핵심만 한눈에 */}
          <div className="mb-4 grid grid-cols-1 gap-2.5 sm:grid-cols-2">
            {activeComparison.priceDifference && (
              <div className="rounded-lg bg-white p-3 text-xs text-ink">
                <p className="mb-0.5 font-medium text-ink">가격 차이</p>
                <p>{activeComparison.priceDifference}</p>
              </div>
            )}
            {activeComparison.commonNotes.length > 0 && (
              <div className="rounded-lg bg-white p-3 text-xs text-ink">
                <p className="mb-0.5 font-medium text-ink">공통 특징</p>
                {activeComparison.commonNotes.map((n, idx) => (
                  <p key={idx}>· {n}</p>
                ))}
              </div>
            )}
            {activeComparison.conditionDifferences.length > 0 && (
              <div className="rounded-lg bg-white p-3 text-xs text-ink">
                <p className="mb-0.5 font-medium text-ink">조건 차이</p>
                {activeComparison.conditionDifferences.map((n, idx) => (
                  <p key={idx}>· {n}</p>
                ))}
              </div>
            )}
            {activeComparison.suggestedQuestions.length > 0 && (
              <div className="rounded-lg bg-white p-3 text-xs text-ink">
                <p className="mb-0.5 font-medium text-ink">확인해볼 질문</p>
                {activeComparison.suggestedQuestions.map((n, idx) => (
                  <p key={idx}>· {n}</p>
                ))}
              </div>
            )}
          </div>

          {/* 견적서 2개 - 각 카드 안 항목 목록만 내부 스크롤(max-h-40), 카드 자체는 짧게 유지 */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {renderQuoteColumn(activeComparison.quoteA)}
            {renderQuoteColumn(activeComparison.quoteB)}
          </div>
        </section>
      ) : (
        <section className="rounded-2xl border border-dashed border-line bg-surface p-8 text-center">
          <p className="text-sm text-ink-faint">
            {comparisons.length > 0
              ? "위 비교 기록을 눌러서 다시 볼 수 있어요."
              : "아직 비교 결과가 없어요."}
          </p>
          <p className="mt-1 text-xs text-ink-faint">
            아래에서 같은 업체의 견적서 2개를 골라 비교해보세요 ↓
          </p>
        </section>
      )}

      {/* 업로드 - 보조 영역 */}
      <section className="rounded-2xl border border-line bg-white p-4">
        <p className="mb-1 text-xs font-semibold text-ink-muted">
          견적서 업로드
        </p>
        <p className="mb-1 text-[11px] text-ink-faint">
          웨딩홀/스튜디오/드레스/메이크업 견적서 사진(JPG/PNG)을 올리면 AI가
          항목을 정리해요. 업로드 후 30일이 지나면 자동으로 삭제돼요.
        </p>
        {/* 낙서/메모가 겹친 사진이나 표가 복잡한 사진은 OCR 정확도가 떨어지는 걸
            실제 테스트로 확인해서, 업로드 전에 미리 기대치를 낮춰두는 안내 문구 추가 */}
        <p className="mb-3 text-[11px] text-ink-faint">
          손글씨로 동그라미·메모가 적혀 있거나 사진이 흐릿하면 정확도가 떨어질
          수 있어요. 가능하면 깨끗하고 선명한 사진을 올려주세요.
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

      {/* 갤러리 - 보조 영역 */}
      <section className="rounded-2xl border border-line bg-white p-4">
        <div className="mb-3 flex items-center justify-between">
          <p className="text-xs font-semibold text-ink-muted">
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

        {/* 사용자가 사진만 올려두고 다음에 뭘 해야할지 몰라 헤매지 않도록, 견적서가 2개
            이상인데 아직 2개를 고르지 않았으면 바로 눈에 띄는 안내 문구를 보여준다 */}
        {quotes.length >= 2 && selectedIds.length < 2 && (
          <p className="mb-3 rounded-lg bg-blush-50 px-3 py-2 text-xs font-medium text-brand-deep">
            {selectedIds.length === 0
              ? "💡 아래에서 같은 업체의 견적서 2개를 눌러서 선택하면 비교할 수 있어요"
              : "1개만 더 선택하면 비교할 수 있어요"}
          </p>
        )}

        {loading ? (
          <p className="text-sm text-ink-faint">불러오는 중...</p>
        ) : quotes.length === 0 ? (
          <p className="text-sm text-ink-faint">
            아직 업로드한 견적서가 없어요.
          </p>
        ) : (
          <div className="grid grid-cols-3 gap-2.5 sm:grid-cols-4 md:grid-cols-6">
            {quotes.map((quote) => {
              const selectedOrder = selectedIds.indexOf(quote.quoteId);
              const selected = selectedOrder !== -1;
              return (
                <div
                  key={quote.quoteId}
                  className={`relative overflow-hidden rounded-lg border-2 p-1.5 transition ${
                    selected
                      ? "border-brand bg-blush-50 shadow-[0_0_0_3px_rgba(0,0,0,0.04)] ring-2 ring-brand ring-offset-1"
                      : "border-line"
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => toggleSelect(quote)}
                    className="relative block w-full text-left"
                  >
                    {selected && (
                      <span className="absolute right-1 top-1 z-10 flex h-5 w-5 items-center justify-center rounded-full bg-brand text-[11px] font-bold text-white shadow">
                        {selectedOrder + 1}
                      </span>
                    )}
                    <QuoteThumbnail quoteId={quote.quoteId} />
                    <p className="mt-1 truncate text-[11px] font-medium text-ink">
                      {categoryLabel[quote.category]}
                    </p>
                    <p className="truncate text-[10px] text-ink-faint">
                      {quote.vendorNameGuess || "업체명 미상"}
                    </p>
                    {selected && (
                      <p className="truncate text-[10px] font-semibold text-brand-dark">
                        선택됨
                      </p>
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(quote.quoteId)}
                    className="mt-1 w-full rounded-full border border-red-200 py-0.5 text-[9px] text-red-600 hover:border-red-400"
                  >
                    삭제
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};

export default QuoteCompareComponent;
