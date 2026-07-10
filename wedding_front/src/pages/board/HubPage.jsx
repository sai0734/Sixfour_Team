import { useEffect, useMemo, useState } from "react";
import BasicMenu from "../../components/menus/BasicMenu";
import BoardTopTabs from "../../components/board/BoardTopTabs";
import TapeLabel from "../../components/common/TapeLabel";
import PostCard from "../../components/board/PostCard";
import BoardFormModal, {
  BOARD_TYPE_LABELS,
} from "../../components/board/BoardFormModal";
import DetailModal from "../../components/board/DetailModal";
import {
  getList,
  getBest,
  getOne,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/boardApi";
import { upload as uploadImages } from "../../api/boardImageApi";
import { checkLiked, likeOne, unlikeOne } from "../../api/boardLikeApi";
import {
  getViewedBoardIds,
  saveViewedBoard,
} from "../../util/boardViewHistory";
import useCustomLogin from "../../hooks/useCustomLogin";

const RECENT_POSTS_PAGE_SIZE = 5;

const getPageNumbers = (currentPage, totalPages) => {
  if (totalPages <= 0) return [];

  const visibleCount = Math.min(5, totalPages);
  let start = currentPage <= 3 ? 1 : currentPage - 2;
  let end = start + visibleCount - 1;

  if (end > totalPages) {
    end = totalPages;
    start = Math.max(1, end - visibleCount + 1);
  }

  return Array.from({ length: end - start + 1 }, (_, idx) => start + idx);
};

const HubPage = () => {
  const { loginState } = useCustomLogin();
  const isAdmin = loginState.roleNames?.some((r) =>
    ["ADMIN", "ROLE_ADMIN"].includes(r),
  );

  const [posts, setPosts] = useState([]);
  const [bestPosts, setBestPosts] = useState([]);
  const [viewedBoardIds, setViewedBoardIds] = useState(() =>
    getViewedBoardIds(loginState.email),
  );
  const [currentPage, setCurrentPage] = useState(1);
  const [refresh, setRefresh] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [detailPost, setDetailPost] = useState(null);
  const [liked, setLiked] = useState(false);

  useEffect(() => {
    getBest().then((data) => setBestPosts(data));
  }, [refresh]);

  useEffect(() => {
    getList().then((data) => setPosts(data));
  }, [refresh]);

  useEffect(() => {
    setViewedBoardIds(getViewedBoardIds(loginState.email));
    setCurrentPage(1);
  }, [loginState.email]);

  const viewedPosts = useMemo(() => {
    if (viewedBoardIds.length === 0) return [];

    return posts
      .filter((post) => viewedBoardIds.includes(Number(post.boardId)))
      .sort(
        (a, b) =>
          viewedBoardIds.indexOf(Number(a.boardId)) -
          viewedBoardIds.indexOf(Number(b.boardId)),
      );
  }, [posts, viewedBoardIds]);

  const totalPages = Math.ceil(viewedPosts.length / RECENT_POSTS_PAGE_SIZE);
  const pageNumbers = getPageNumbers(currentPage, totalPages);
  const pagedPosts = viewedPosts.slice(
    (currentPage - 1) * RECENT_POSTS_PAGE_SIZE,
    currentPage * RECENT_POSTS_PAGE_SIZE,
  );

  useEffect(() => {
    if (totalPages > 0 && currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const openDetail = (boardId) => {
    const nextViewedBoardIds = saveViewedBoard(boardId, loginState.email);
    setViewedBoardIds(nextViewedBoardIds);
    setCurrentPage(1);

    getOne(boardId).then((data) => {
      setDetailPost(data);
      checkLiked(boardId, loginState.email).then((result) => setLiked(result));
      setRefresh((r) => !r);
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
        setRefresh((r) => !r);
      })
      .catch((e) => console.error(e));
  };

  const handleCommentCountChange = (delta) => {
    setDetailPost((prev) => ({
      ...prev,
      commentCount: prev.commentCount + delta,
    }));
    setRefresh((r) => !r);
  };

  const handleAdd = (formValues) => {
    const { files, ...boardData } = formValues;

    postAdd({
      ...boardData,
      memberEmail: loginState.email,
      nickname: loginState.nickname,
    })
      .then((res) => {
        setModalOpen(false);
        setRefresh((r) => !r);

        if (files && files.length > 0) {
          uploadImages(res.boardId, files).catch((e) => {
            console.error(e);
            alert(
              "글은 등록됐지만, 이미지/동영상 업로드에는 실패했어요. (BoardImage 백엔드가 적용/재시작 됐는지 확인해주세요)",
            );
          });
        }
      })
      .catch((e) => console.error(e));
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

  return (
    <>
      <BasicMenu />

      <div className="bg-cream min-h-screen">
        <section
          className="relative bg-cover bg-center px-5 pt-24 pb-8 text-center md:px-8 md:pt-28 md:pb-10 lg:px-[60px]"
          // TODO: 커뮤니티 허브 전용 사진 준비되면 /community-hub-hero.jpg 같은 걸로 교체
          // (지금은 준비관리에 넣은 사진을 그대로 임시로 재사용)
          style={{ backgroundImage: "url('/prep-hero.jpg')" }}
        >
          <div className="absolute inset-0 bg-black/45" />

          <div className="relative z-10">
            <TapeLabel tone="white" className="mb-3">
              WEDDING COMMUNITY
            </TapeLabel>
            <p className="mb-2 font-['Gowun_Batang'] text-2xl text-white md:text-3xl">
              웨딩 커뮤니티
            </p>
            <p className="text-sm text-white/85">
              예비 부부들의 생생한 후기와 정보를 나눠요
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-[1140px] px-5 pt-6 md:px-8 lg:px-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <BoardTopTabs active="ALL" />

            <button
              type="button"
              onClick={() => setModalOpen(true)}
              className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark mb-3 shrink-0 self-start sm:self-auto"
            >
              + 글쓰기
            </button>
          </div>

          <div className="pt-2 pb-20">
            {/* 베스트 게시글 */}
            {bestPosts.length > 0 && (
              <div className="mt-6 mb-8">
                <p className="text-sm font-medium text-ink mb-3">
                  🔥 베스트 게시글
                </p>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                  {bestPosts.map((post, idx) => (
                    <div
                      key={post.boardId}
                      onClick={() => openDetail(post.boardId)}
                      className="bg-white rounded-2xl border border-line p-5 cursor-pointer hover:border-brand transition-colors"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-xs font-medium text-brand">
                          {idx + 1}위
                        </span>
                        <span className="text-[11px] bg-brand-light text-brand-accent px-2 py-0.5 rounded-full font-medium">
                          {BOARD_TYPE_LABELS[post.boardType]}
                        </span>
                      </div>
                      <p className="text-sm font-medium text-ink mb-3 line-clamp-2">
                        {post.title}
                      </p>
                      <div className="flex items-center gap-3 text-xs text-ink-faint">
                        <span>♡ {post.likeCount}</span>
                        <span>💬 {post.commentCount}</span>
                        <span>조회 {post.viewCount}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 최근 본 게시글 */}
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm font-medium text-ink">📝 최근 본 게시글</p>
              <p className="text-xs text-ink-faint">
                직접 클릭한 게시글만 표시 · {viewedPosts.length}건
              </p>
            </div>

            {viewedPosts.length === 0 && (
              <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
                아직 직접 열어본 게시글이 없습니다. 자유게시판이나
                후기게시판에서 게시글을 클릭하면 여기에 표시됩니다.
              </div>
            )}

            <div className="flex flex-col gap-3">
              {pagedPosts.map((post) => (
                <PostCard
                  key={post.boardId}
                  post={post}
                  showTypeBadge
                  onClick={() => openDetail(post.boardId)}
                />
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex justify-center items-center gap-2 mt-8">
                {pageNumbers.map((pageNo) => (
                  <button
                    key={pageNo}
                    type="button"
                    onClick={() => setCurrentPage(pageNo)}
                    className={`w-9 h-9 rounded-full text-sm border transition-colors ${
                      currentPage === pageNo
                        ? "bg-brand text-white border-brand"
                        : "bg-white text-ink-muted border-line hover:border-brand hover:text-brand"
                    }`}
                  >
                    {pageNo}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {modalOpen && (
        <BoardFormModal
          mode="add"
          onSubmit={handleAdd}
          onClose={() => setModalOpen(false)}
        />
      )}

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

export default HubPage;
