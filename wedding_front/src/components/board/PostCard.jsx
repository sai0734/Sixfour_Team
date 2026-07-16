import { useEffect, useState } from "react";
import { BOARD_TYPE_LABELS } from "./BoardFormModal";
import { getAiSummary } from "../../api/boardApi";

// MemberManageComponent.jsx의 formatDate와 동일한 컨벤션
const formatDate = (value) => {
  if (!value) return "";
  return String(value).slice(0, 16).replace("T", " ");
};

const PostCard = ({ post, onClick, showTypeBadge = false }) => {
  const [aiSummary, setAiSummary] = useState(post.aiSummary || "");
  const [summaryLoading, setSummaryLoading] = useState(!post.aiSummary);
  const [summaryError, setSummaryError] = useState(false);

  // 카드가 화면에 뜨는 시점에 자동으로 요약을 불러옴 (버튼 클릭 불필요).
  // 이미 목록 응답에 aiSummary가 캐시돼서 왔으면 API 호출 자체를 안 함.
  useEffect(() => {
    if (post.aiSummary || !post.boardId) return;

    let cancelled = false;
    setSummaryLoading(true);
    setSummaryError(false);

    getAiSummary(post.boardId)
      .then((summary) => {
        if (!cancelled) setAiSummary(summary);
      })
      .catch((err) => {
        console.error(err);
        if (!cancelled) setSummaryError(true);
      })
      .finally(() => {
        if (!cancelled) setSummaryLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [post.boardId, post.aiSummary]);

  return (
    <div
      onClick={onClick}
      className="bg-white rounded-2xl border border-line px-5 py-4 cursor-pointer hover:border-brand transition-colors"
    >
      <div className="flex items-center gap-2 mb-1.5">
        {showTypeBadge && (
          <span className="text-[11px] bg-brand-light text-brand-accent px-2 py-0.5 rounded-full font-medium">
            {BOARD_TYPE_LABELS[post.boardType]}
          </span>
        )}
        {post.category && (
          <span className="text-[11px] bg-surface text-ink-muted px-2 py-0.5 rounded-full">
            {post.category}
          </span>
        )}
        {post.boardType === "REVIEW" && post.rating && (
          <span className="text-[11px] text-brand">
            {"★".repeat(post.rating)}
          </span>
        )}
      </div>

      <p className="text-sm font-medium text-ink mb-1.5">{post.title}</p>

      <p className="mb-2 text-xs text-ink-muted line-clamp-1">
        {summaryLoading && (
          <span className="text-ink-faint">AI 한줄요약 생성 중...</span>
        )}
        {!summaryLoading && aiSummary && (
          <>
            <span className="text-brand-deep font-medium">AI 한줄요약:</span>{" "}
            {aiSummary}
          </>
        )}
        {!summaryLoading && !aiSummary && summaryError && (
          <span className="text-ink-faint">AI 한줄요약을 불러오지 못했어요.</span>
        )}
      </p>

      <div className="flex items-center justify-between text-xs text-ink-faint">
        <div className="flex items-center gap-2">
          <span>{post.nickname || post.memberEmail}</span>
          <span>·</span>
          <span>{formatDate(post.regDate)}</span>
        </div>
        <div className="flex items-center gap-3">
          <span>♡ {post.likeCount}</span>
          <span>💬 {post.commentCount}</span>
          <span>조회 {post.viewCount}</span>
        </div>
      </div>
    </div>
  );
};

export default PostCard;
