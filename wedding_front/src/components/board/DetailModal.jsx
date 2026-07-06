import { useEffect, useState } from "react";
import { BOARD_TYPE_LABELS } from "./BoardFormModal";
import CommentSection from "./CommentSection";
import { listByBoard, fileUrl, isVideoFile } from "../../api/boardImageApi";

const DetailModal = ({
  board,
  isOwner,
  isAdmin,
  liked,
  onToggleLike,
  onCommentCountChange,
  onEdit,
  onDelete,
  onClose,
}) => {
  const [images, setImages] = useState([]);

  useEffect(() => {
    if (!board) return;

    listByBoard(board.boardId).then((data) => setImages(data));
  }, [board?.boardId]);

  if (!board) return null;

  const canHide = isOwner || isAdmin;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-[11px] bg-brand-light text-brand-accent px-2.5 py-1 rounded-full font-medium">
            {BOARD_TYPE_LABELS[board.boardType]}
          </span>
          {board.category && (
            <span className="text-[11px] bg-surface text-ink-muted px-2.5 py-1 rounded-full">
              {board.category}
            </span>
          )}
          {board.boardType === "REVIEW" && board.rating && (
            <span className="text-xs text-brand">
              {"★".repeat(board.rating)}
              {"☆".repeat(5 - board.rating)}
            </span>
          )}
        </div>

        <p className="text-lg font-medium text-ink mb-2">{board.title}</p>

        <div className="flex items-center gap-3 text-xs text-ink-faint mb-5">
          <span>{board.memberEmail}</span>
          <span>·</span>
          <span>{board.regDate}</span>
          <span>·</span>
          <span>조회 {board.viewCount}</span>
          <span>·</span>
          <span>댓글 {board.commentCount}</span>
        </div>

        <p className="text-sm text-ink whitespace-pre-wrap leading-relaxed mb-4">
          {board.content}
        </p>

        {images.length > 0 && (
          <div className="grid grid-cols-2 gap-2 mb-6">
            {images.map((img) =>
              isVideoFile(img.imageUrl) ? (
                <video
                  key={img.imageId}
                  src={fileUrl(img.imageUrl)}
                  controls
                  className="w-full rounded-xl bg-black"
                />
              ) : (
                <img
                  key={img.imageId}
                  src={fileUrl(img.imageUrl)}
                  alt=""
                  className="w-full rounded-xl object-cover"
                />
              ),
            )}
          </div>
        )}

        {board.aiSummary && (
          <div className="bg-surface rounded-xl p-4 mb-6">
            <p className="text-xs font-medium text-ink-soft mb-1">
              AI 3줄 요약
            </p>
            <p className="text-xs text-ink-muted whitespace-pre-wrap">
              {board.aiSummary}
            </p>
          </div>
        )}

        {/* 좋아요 버튼 */}
        <div className="flex justify-center mb-6">
          <button
            type="button"
            onClick={onToggleLike}
            className={`flex items-center gap-2 h-10 px-6 rounded-full border text-sm font-medium transition-colors ${
              liked
                ? "bg-brand-light border-brand text-brand-accent"
                : "border-line-soft text-ink-soft hover:bg-cream"
            }`}
          >
            <svg
              viewBox="0 0 24 24"
              fill={liked ? "#D4537E" : "none"}
              stroke={liked ? "#D4537E" : "currentColor"}
              strokeWidth="1.8"
              className="w-4 h-4"
            >
              <path d="M19.5 12.572 12 20l-7.5-7.428a5 5 0 1 1 7.5-6.566 5 5 0 1 1 7.5 6.566Z" />
            </svg>
            좋아요 {board.likeCount}
          </button>
        </div>

        <CommentSection
          boardId={board.boardId}
          onCountChange={onCommentCountChange}
        />

        <div className="flex justify-end gap-2 mt-4">
          {isOwner && (
            <button
              type="button"
              onClick={onEdit}
              className="h-9 px-4 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
            >
              수정
            </button>
          )}
          {canHide && (
            <button
              type="button"
              onClick={onDelete}
              className="h-9 px-4 rounded-full border border-line-soft text-xs text-red-600 hover:bg-cream"
            >
              {isOwner ? "삭제" : "숨김 처리"}
            </button>
          )}
          <button
            type="button"
            onClick={onClose}
            className="h-9 px-4 rounded-full bg-brand text-white text-xs font-medium hover:bg-brand-dark"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};

export default DetailModal;
