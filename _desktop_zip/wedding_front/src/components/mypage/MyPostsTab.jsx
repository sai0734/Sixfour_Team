import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getListByMember, getOne, putOne, deleteOne } from "../../api/boardApi";
import { checkLiked, likeOne, unlikeOne } from "../../api/boardLikeApi";
import { upload as uploadImages } from "../../api/boardImageApi";
import { getMyReviews, deleteReview } from "../../api/reviewApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import BoardFormModal, { BOARD_TYPE_LABELS } from "../board/BoardFormModal";
import DetailModal from "../board/DetailModal";
import useCustomLogin from "../../hooks/useCustomLogin";

const SUB_TABS = [
  { key: "community", label: "커뮤니티" },
  { key: "productReview", label: "답례품 리뷰" },
];

const MyPostsTab = () => {
  const { loginState } = useCustomLogin();
  const isAdmin = loginState.roleNames?.some((r) =>
    ["ADMIN", "ROLE_ADMIN"].includes(r),
  );

  const [searchParams, setSearchParams] = useSearchParams();

  // 답례품 상세로 갔다가 뒤로가기 눌러도 "내가 쓴 글 > 답례품 리뷰"로 돌아오도록
  // 서브탭 상태를 URL(?msub=)에 둠
  const subTab =
    searchParams.get("msub") === "productReview"
      ? "productReview"
      : "community";
  const setSubTab = (key) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set("msub", key);
        return next;
      },
      { replace: true },
    );
  };

  const [posts, setPosts] = useState([]);
  const [refresh, setRefresh] = useState(false);

  const [detailPost, setDetailPost] = useState(null);
  const [editTarget, setEditTarget] = useState(null);
  const [liked, setLiked] = useState(false);

  const [productReviews, setProductReviews] = useState([]);
  const [reviewRefresh, setReviewRefresh] = useState(false);

  useEffect(() => {
    if (!loginState.email) return;

    getListByMember(loginState.email).then((data) => setPosts(data));
  }, [loginState.email, refresh]);

  useEffect(() => {
    if (!loginState.email) return;

    getMyReviews()
      .then((data) => setProductReviews(data))
      .catch((e) => console.error(e));
  }, [loginState.email, reviewRefresh]);

  // 글 제목 클릭 -> 모달로 상세 열기 (게시판 리스트로 안 보내고 바로 여기서 수정/삭제)
  const openDetail = (boardId) => {
    getOne(boardId).then((data) => {
      setDetailPost(data);
      checkLiked(boardId, loginState.email).then((result) => setLiked(result));
    });
  };

  const handleToggleLike = () => {
    const action = liked
      ? unlikeOne(detailPost.boardId, loginState.email)
      : likeOne(detailPost.boardId, loginState.email);

    action
      .then(() => {
        const nextLiked = !liked;
        setLiked(nextLiked);
        setDetailPost((prev) => ({
          ...prev,
          likeCount: prev.likeCount + (nextLiked ? 1 : -1),
        }));
      })
      .catch((e) => console.error(e));
  };

  const handleCommentCountChange = (delta) => {
    setDetailPost((prev) => ({
      ...prev,
      commentCount: prev.commentCount + delta,
    }));
  };

  const handleEditSubmit = (formValues) => {
    const { files, ...boardData } = formValues;

    putOne({ ...editTarget, ...boardData })
      .then(() => {
        setEditTarget(null);
        setDetailPost(null);
        setRefresh((r) => !r);

        if (files && files.length > 0) {
          uploadImages(editTarget.boardId, files).catch((e) => {
            console.error(e);
            alert("수정은 됐지만, 이미지/동영상 업로드에는 실패했어요.");
          });
        }
      })
      .catch((e) => console.error(e));
  };

  const handleDelete = () => {
    if (!window.confirm("삭제하시겠습니까?")) return;

    deleteOne(detailPost.boardId)
      .then(() => {
        setDetailPost(null);
        setRefresh((r) => !r);
      })
      .catch((e) => console.error(e));
  };

  const handleDeleteReview = (pno, rno) => {
    if (!window.confirm("이 리뷰를 삭제하시겠습니까?")) return;

    deleteReview(pno, rno)
      .then(() => setReviewRefresh((r) => !r))
      .catch((e) => console.error(e));
  };

  if (!loginState.email) {
    return (
      <div className="p-10 text-center text-ink-faint">
        로그인 후 이용해주세요.
      </div>
    );
  }

  return (
    <>
      <div className="pb-20">
        <nav className="flex gap-6 text-sm font-medium border-b border-line mb-6">
          {SUB_TABS.map((tab) => (
            <span
              key={tab.key}
              onClick={() => setSubTab(tab.key)}
              className={`pb-3 border-b cursor-pointer ${
                subTab === tab.key
                  ? "text-brand border-brand"
                  : "text-ink-soft border-transparent hover:text-ink"
              }`}
            >
              {tab.label}
            </span>
          ))}
        </nav>

        {subTab === "community" && (
          <div>
            <p className="text-sm text-ink-muted mb-6">
              내가 쓴 글 {posts.length}개 (자유게시판·후기게시판)
            </p>

            {posts.length === 0 && (
              <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
                작성한 글이 없습니다.
              </div>
            )}

            <div className="flex flex-col gap-3">
              {posts.map((post) => (
                <div
                  key={post.boardId}
                  onClick={() => openDetail(post.boardId)}
                  className="bg-white rounded-2xl border border-line p-5 cursor-pointer hover:border-brand transition-colors"
                >
                  <div className="flex items-center gap-2 mb-1.5">
                    <span className="text-[11px] bg-brand-light text-brand-accent px-2.5 py-1 rounded-full font-medium">
                      {BOARD_TYPE_LABELS[post.boardType] || post.boardType}
                    </span>
                    <span className="text-[11px] text-ink-faint">
                      {post.regDate}
                    </span>
                  </div>

                  <p className="text-sm font-medium text-ink mb-2">
                    {post.title}
                  </p>

                  <div className="flex items-center gap-3 text-xs text-ink-faint">
                    <span>조회 {post.viewCount}</span>
                    <span>좋아요 {post.likeCount}</span>
                    <span>댓글 {post.commentCount}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {subTab === "productReview" && (
          <div>
            <p className="text-sm text-ink-muted mb-6">
              작성한 답례품 리뷰 {productReviews.length}개
            </p>

            {productReviews.length === 0 && (
              <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
                작성한 리뷰가 없습니다.
              </div>
            )}

            <div className="flex flex-col gap-3">
              {productReviews.map((review) => (
                <div
                  key={review.rno}
                  className="bg-white rounded-2xl border border-line p-5"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 mb-1.5">
                        {review.rating && (
                          <span className="text-[11px] bg-brand-light text-brand-accent px-2.5 py-1 rounded-full font-medium">
                            {"★".repeat(review.rating)}
                          </span>
                        )}
                        <span className="text-[11px] text-ink-faint">
                          {review.regDate?.toString().slice(0, 10)}
                        </span>
                      </div>

                      <Link
                        to={`/product/read/${review.pno}`}
                        className="text-sm font-medium text-ink hover:text-brand mb-1 block"
                      >
                        상품 보러가기 →
                      </Link>

                      <p className="text-sm text-ink-soft whitespace-pre-wrap">
                        {review.content}
                      </p>

                      {review.uploadFileNames?.length > 0 && (
                        <div className="flex gap-2 mt-3">
                          {review.uploadFileNames.map((fileName, i) => (
                            <img
                              key={i}
                              src={`${API_SERVER_HOST}/api/product/view/${fileName}`}
                              alt=""
                              className="w-16 h-16 rounded-lg object-cover"
                            />
                          ))}
                        </div>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={() => handleDeleteReview(review.pno, review.rno)}
                      className="h-8 px-4 rounded-full border border-line-soft text-xs text-red-600 hover:bg-cream shrink-0"
                    >
                      삭제
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {editTarget && (
        <BoardFormModal
          mode="edit"
          editTarget={editTarget}
          onSubmit={handleEditSubmit}
          onClose={() => setEditTarget(null)}
        />
      )}

      {detailPost && !editTarget && (
        <DetailModal
          board={detailPost}
          isOwner={detailPost.memberEmail === loginState.email}
          isAdmin={isAdmin}
          liked={liked}
          onToggleLike={handleToggleLike}
          onCommentCountChange={handleCommentCountChange}
          onEdit={() => setEditTarget(detailPost)}
          onDelete={handleDelete}
          onClose={() => setDetailPost(null)}
        />
      )}
    </>
  );
};

export default MyPostsTab;
