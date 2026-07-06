import { useEffect, useMemo, useState } from "react";
import BasicMenu from "../../components/menus/BasicMenu";
import BoardTopTabs from "../../components/board/BoardTopTabs";
import BoardFilterSidebar from "../../components/board/BoardFilterSidebar";
import SearchSortBar from "../../components/board/SearchSortBar";
import PostCard from "../../components/board/PostCard";
import BoardFormModal from "../../components/board/BoardFormModal";
import DetailModal from "../../components/board/DetailModal";
import {
  getList,
  getOne,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/boardApi";
import { upload as uploadImages } from "../../api/boardImageApi";
import { checkLiked, likeOne, unlikeOne } from "../../api/boardLikeApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const FREE_CATEGORIES = ["웨딩준비", "업체정보", "잡담", "꿀팁"];

const FreeBoardPage = () => {
  const { loginState } = useCustomLogin();
  const isAdmin = loginState.roleNames?.some((r) =>
    ["ADMIN", "ROLE_ADMIN"].includes(r),
  );

  const [posts, setPosts] = useState([]);
  const [activeCategory, setActiveCategory] = useState(null);
  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState("recent");
  const [refresh, setRefresh] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);

  const [detailPost, setDetailPost] = useState(null);
  const [liked, setLiked] = useState(false);

  useEffect(() => {
    getList("FREE").then((data) => setPosts(data));
  }, [refresh]);

  const visiblePosts = useMemo(() => {
    let result = posts;

    if (activeCategory) {
      result = result.filter((p) => p.category === activeCategory);
    }

    if (keyword.trim()) {
      const kw = keyword.trim().toLowerCase();
      result = result.filter(
        (p) =>
          p.title.toLowerCase().includes(kw) ||
          (p.content && p.content.toLowerCase().includes(kw)),
      );
    }

    result = [...result].sort((a, b) =>
      sort === "popular"
        ? b.likeCount - a.likeCount
        : new Date(b.regDate) - new Date(a.regDate),
    );

    return result;
  }, [posts, activeCategory, keyword, sort]);

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
    const { files, ...boardData } = formValues;

    postAdd({ ...boardData, memberEmail: loginState.email })
      .then((res) => {
        if (files && files.length > 0) {
          return uploadImages(res.boardId, files);
        }
      })
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
        <section className="text-center pt-12 pb-6 bg-brand-light">
          <p className="text-xs tracking-[0.15em] text-brand-accent mb-2.5">
            FREE BOARD
          </p>
          <p className="font-serif text-3xl text-brand-deep mb-2">자유게시판</p>
          <p className="text-sm text-brand-accent">
            예비 부부들의 자유로운 이야기
          </p>
        </section>

        <div className="max-w-[1140px] mx-auto px-6 pt-6">
          <BoardTopTabs active="FREE" />
        </div>

        <div className="max-w-[1140px] mx-auto px-6 flex">
          <BoardFilterSidebar
            groups={[
              {
                title: "카테고리",
                options: FREE_CATEGORIES.map((c) => ({ value: c, label: c })),
                activeValue: activeCategory,
                onSelect: setActiveCategory,
                resetLabel: "전체글",
              },
            ]}
          />

          <main className="flex-1 py-8 min-w-0">
            <div className="flex items-center justify-between mb-4">
              <p className="text-sm text-ink-muted">
                {activeCategory || "전체글"} · {visiblePosts.length}건
              </p>
              <button
                type="button"
                onClick={() => setModalOpen(true)}
                className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
              >
                + 글쓰기
              </button>
            </div>

            <SearchSortBar
              keyword={keyword}
              onKeywordChange={setKeyword}
              sort={sort}
              onSortChange={setSort}
            />

            {visiblePosts.length === 0 && (
              <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
                게시글이 없습니다.
              </div>
            )}

            <div className="flex flex-col gap-3">
              {visiblePosts.map((post) => (
                <PostCard
                  key={post.boardId}
                  post={post}
                  onClick={() => openDetail(post.boardId)}
                />
              ))}
            </div>
          </main>
        </div>
      </div>

      {modalOpen && (
        <BoardFormModal
          mode="add"
          fixedType="FREE"
          categoryOptions={FREE_CATEGORIES}
          onSubmit={handleAdd}
          onClose={() => setModalOpen(false)}
        />
      )}

      {editTarget && (
        <BoardFormModal
          mode="edit"
          editTarget={editTarget}
          fixedType="FREE"
          categoryOptions={FREE_CATEGORIES}
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

export default FreeBoardPage;
