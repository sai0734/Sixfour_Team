import { useEffect, useState } from "react";
import BasicMenu from "../../components/menus/BasicMenu";
import BoardTopTabs from "../../components/board/BoardTopTabs";
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
import { checkLiked, likeOne, unlikeOne } from "../../api/boardLikeApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const HubPage = () => {
  const { loginState } = useCustomLogin();

  const [posts, setPosts] = useState([]);
  const [bestPosts, setBestPosts] = useState([]);
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

  const openDetail = (boardId) => {
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
    postAdd({ ...formValues, memberEmail: loginState.email })
      .then(() => {
        setModalOpen(false);
        setRefresh((r) => !r);
      })
      .catch((e) => console.error(e));
  };

  const handleEditSubmit = (formValues) => {
    putOne({ ...editTarget, ...formValues })
      .then(() => {
        setEditTarget(null);
        setDetailPost(null);
        setRefresh((r) => !r);
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
        <section className="text-center pt-14 pb-7 bg-brand-light">
          <p className="text-xs tracking-[0.15em] text-brand-accent mb-2.5">
            WEDDING COMMUNITY
          </p>
          <p className="font-serif text-3xl text-brand-deep mb-2">
            웨딩 커뮤니티
          </p>
          <p className="text-sm text-brand-accent">
            예비 부부들의 생생한 후기와 정보를 나눠요
          </p>
        </section>

        <div className="max-w-[1140px] mx-auto px-6 pt-6">
          <div className="flex items-center justify-between">
            <BoardTopTabs active="ALL" />

            <button
              type="button"
              onClick={() => setModalOpen(true)}
              className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark mb-3 shrink-0"
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
                <div className="grid grid-cols-3 gap-4">
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

            {/* 최근 게시글 (전체 타입 혼합) */}
            <p className="text-sm font-medium text-ink mb-3">📝 최근 게시글</p>

            {posts.length === 0 && (
              <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
                게시글이 없습니다.
              </div>
            )}

            <div className="flex flex-col gap-3">
              {posts.map((post) => (
                <PostCard
                  key={post.boardId}
                  post={post}
                  showTypeBadge
                  onClick={() => openDetail(post.boardId)}
                />
              ))}
            </div>
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
