import { BOARD_TYPE_LABELS } from "./BoardFormModal";

const PostCard = ({ post, onClick, showTypeBadge = false }) => {
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

      <p className="text-sm font-medium text-ink mb-2">{post.title}</p>

      <div className="flex items-center justify-between text-xs text-ink-faint">
        <span>{post.memberEmail}</span>
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
