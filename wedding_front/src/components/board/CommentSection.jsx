import { useEffect, useState } from "react";
import {
  getListByBoard,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/commentApi";
import {
  upload as uploadCommentImages,
  listByComment,
} from "../../api/commentImageApi";
import { fileUrl, isVideoFile } from "../../api/boardImageApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const CommentSection = ({ boardId, onCountChange }) => {
  const { loginState } = useCustomLogin();

  const [comments, setComments] = useState([]);
  const [imagesMap, setImagesMap] = useState({});
  const [refresh, setRefresh] = useState(false);

  const [newContent, setNewContent] = useState("");
  const [newFiles, setNewFiles] = useState([]);
  const [replyTargetId, setReplyTargetId] = useState(null);
  const [replyContent, setReplyContent] = useState("");
  const [replyFiles, setReplyFiles] = useState([]);
  const [editTargetId, setEditTargetId] = useState(null);
  const [editContent, setEditContent] = useState("");

  useEffect(() => {
    if (!boardId) return;

    getListByBoard(boardId).then((data) => {
      setComments(data);

      Promise.all(
        data.map((c) =>
          listByComment(c.commentId).then((imgs) => [c.commentId, imgs]),
        ),
      ).then((pairs) => {
        setImagesMap(Object.fromEntries(pairs));
      });
    });
  }, [boardId, refresh]);

  const topLevel = comments.filter((c) => !c.parentId);
  const repliesOf = (parentId) =>
    comments.filter((c) => c.parentId === parentId);

  const handleAddComment = () => {
    if (!newContent.trim()) return;

    postAdd({
      boardId,
      memberEmail: loginState.email,
      parentId: null,
      content: newContent,
    })
      .then((res) => {
        setNewContent("");
        setRefresh((r) => !r);
        onCountChange && onCountChange(1);

        if (newFiles.length > 0) {
          uploadCommentImages(res.commentId, newFiles).catch((e) =>
            console.error(e),
          );
        }
        setNewFiles([]);
      })
      .catch((e) => console.error(e));
  };

  const handleAddReply = (parentId) => {
    if (!replyContent.trim()) return;

    postAdd({
      boardId,
      memberEmail: loginState.email,
      parentId,
      content: replyContent,
    })
      .then((res) => {
        setReplyTargetId(null);
        setReplyContent("");
        setRefresh((r) => !r);
        onCountChange && onCountChange(1);

        if (replyFiles.length > 0) {
          uploadCommentImages(res.commentId, replyFiles).catch((e) =>
            console.error(e),
          );
        }
        setReplyFiles([]);
      })
      .catch((e) => console.error(e));
  };

  const startEdit = (comment) => {
    setEditTargetId(comment.commentId);
    setEditContent(comment.content);
  };

  const handleSaveEdit = (comment) => {
    putOne({ ...comment, content: editContent })
      .then(() => {
        setEditTargetId(null);
        setRefresh((r) => !r);
      })
      .catch((e) => console.error(e));
  };

  const handleDelete = (commentId) => {
    if (!window.confirm("댓글을 삭제하시겠습니까?")) return;

    deleteOne(commentId)
      .then(() => {
        setRefresh((r) => !r);
        onCountChange && onCountChange(-1);
      })
      .catch((e) => console.error(e));
  };

  const renderMedia = (commentId) => {
    const images = imagesMap[commentId];
    if (!images || images.length === 0) return null;

    return (
      <div className="grid grid-cols-3 gap-1.5 mt-2 mb-1">
        {images.map((img) =>
          isVideoFile(img.imageUrl) ? (
            <video
              key={img.imageId}
              src={fileUrl(img.imageUrl)}
              controls
              className="w-full rounded-lg bg-black"
            />
          ) : (
            <img
              key={img.imageId}
              src={fileUrl(img.imageUrl)}
              alt=""
              className="w-full rounded-lg object-cover aspect-square"
            />
          ),
        )}
      </div>
    );
  };

  const renderComment = (comment, isReply = false) => {
    const isOwner = comment.memberEmail === loginState.email;
    const isEditing = editTargetId === comment.commentId;

    return (
      <div key={comment.commentId} className={isReply ? "ml-8 mt-2" : "mt-3"}>
        <div
          className={
            isReply
              ? "bg-white border border-line rounded-xl px-4 py-3 border-l-[3px] border-l-brand"
              : "bg-cream rounded-xl px-4 py-3"
          }
        >
          <div className="flex items-center justify-between mb-1">
            <span className="text-xs font-medium text-ink flex items-center gap-1">
              {isReply && (
                <span className="text-brand text-sm leading-none">↳</span>
              )}
              {comment.deleted ? "" : comment.memberEmail}
              {isReply && !comment.deleted && (
                <span className="text-[10px] text-brand-accent bg-brand-light px-1.5 py-0.5 rounded-full ml-1">
                  답글
                </span>
              )}
            </span>
            <span className="text-[11px] text-ink-faint">
              {comment.regDate}
            </span>
          </div>

          {comment.deleted ? (
            <p className="text-xs text-ink-faint italic">삭제된 댓글입니다.</p>
          ) : isEditing ? (
            <div className="flex flex-col gap-2">
              <textarea
                className="text-xs px-3 py-2 rounded-lg border border-line-soft resize-none min-h-[60px]"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setEditTargetId(null)}
                  className="text-[11px] text-ink-soft"
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={() => handleSaveEdit(comment)}
                  className="text-[11px] text-brand font-medium"
                >
                  저장
                </button>
              </div>
            </div>
          ) : (
            <>
              <p className="text-xs text-ink whitespace-pre-wrap">
                {comment.content}
              </p>
              {renderMedia(comment.commentId)}
            </>
          )}

          {!comment.deleted && !isEditing && (
            <div className="flex items-center gap-3 mt-2">
              {!isReply && (
                <button
                  type="button"
                  onClick={() =>
                    setReplyTargetId(
                      replyTargetId === comment.commentId
                        ? null
                        : comment.commentId,
                    )
                  }
                  className="text-[11px] text-ink-muted hover:text-brand"
                >
                  답글
                </button>
              )}
              {isOwner && (
                <>
                  <button
                    type="button"
                    onClick={() => startEdit(comment)}
                    className="text-[11px] text-ink-muted hover:text-brand"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(comment.commentId)}
                    className="text-[11px] text-ink-muted hover:text-red-600"
                  >
                    삭제
                  </button>
                </>
              )}
            </div>
          )}
        </div>

        {/* 답글 입력창 */}
        {replyTargetId === comment.commentId && (
          <div className="ml-8 mt-2 flex flex-col gap-2">
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="답글을 입력하세요"
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                className="flex-1 h-9 px-3 rounded-lg border border-line-soft text-xs focus:outline-none focus:border-brand"
              />
              <button
                type="button"
                onClick={() => handleAddReply(comment.commentId)}
                className="h-9 px-4 rounded-lg bg-brand text-white text-xs font-medium hover:bg-brand-dark"
              >
                등록
              </button>
            </div>
            <input
              type="file"
              accept="image/*,video/*"
              multiple
              onChange={(e) => setReplyFiles(Array.from(e.target.files || []))}
              className="text-[11px] text-ink-faint file:mr-2 file:h-7 file:px-3 file:rounded-full file:border-0 file:bg-surface file:text-[11px] file:text-ink-soft"
            />
          </div>
        )}

        {/* 대댓글들 */}
        {repliesOf(comment.commentId).map((reply) =>
          renderComment(reply, true),
        )}
      </div>
    );
  };

  if (!loginState.email) {
    return null;
  }

  return (
    <div className="mt-6 pt-6 border-t border-line">
      <p className="text-sm font-medium text-ink mb-3">
        댓글 {comments.filter((c) => !c.deleted).length}
      </p>

      <div className="flex flex-col gap-2 mb-4">
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="댓글을 입력하세요"
            value={newContent}
            onChange={(e) => setNewContent(e.target.value)}
            className="flex-1 h-10 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
          />
          <button
            type="button"
            onClick={handleAddComment}
            className="h-10 px-5 rounded-lg bg-brand text-white text-sm font-medium hover:bg-brand-dark"
          >
            등록
          </button>
        </div>
        <input
          type="file"
          accept="image/*,video/*"
          multiple
          onChange={(e) => setNewFiles(Array.from(e.target.files || []))}
          className="text-[11px] text-ink-faint file:mr-2 file:h-7 file:px-3 file:rounded-full file:border-0 file:bg-surface file:text-[11px] file:text-ink-soft"
        />
      </div>

      {topLevel.length === 0 ? (
        <p className="text-xs text-ink-faint text-center py-6">
          첫 댓글을 남겨보세요.
        </p>
      ) : (
        topLevel.map((comment) => renderComment(comment))
      )}
    </div>
  );
};

export default CommentSection;
