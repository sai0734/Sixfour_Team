import { useEffect, useMemo, useState } from "react";
import BasicMenu from "../../components/menus/BasicMenu";
import BoardTopTabs from "../../components/board/BoardTopTabs";
import BoardFilterSidebar from "../../components/board/BoardFilterSidebar";
import TapeLabel from "../../components/common/TapeLabel";
import CommunityHeaderArt from "../../components/common/CommunityHeaderArt";
import SearchSortBar from "../../components/board/SearchSortBar";
import PostCard from "../../components/board/PostCard";
import BoardFormModal from "../../components/board/BoardFormModal";
import DetailModal from "../../components/board/DetailModal";
import BoardPagination from "../../components/board/BoardPagination";
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

const VENDOR_CATEGORIES = ["홀", "스드메", "예복", "예물", "기타"];
const PAGE_SIZE = 10;

const RATING_OPTIONS = [
  { value: 5, label: "★★★★★" },
  { value: 4, label: "★★★★☆ 이상" },
  { value: 3, label: "★★★☆☆ 이상" },
  { value: 2, label: "★★☆☆☆ 이상" },
  { value: 1, label: "★☆☆☆☆ 이상" },
];

const ReviewBoardPage = () => {
  const { loginState } = useCustomLogin();
  const isAdmin = loginState.roleNames?.some((r) =>
    ["ADMIN", "ROLE_ADMIN"].includes(r),
  );

  const [posts, setPosts] = useState([]);
  const [activeCategory, setActiveCategory] = useState(null);
  const [activeRating, setActiveRating] = useState(null);
  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState("recent");
  const [refresh, setRefresh] = useState(false);
  const [page, setPage] = useState(1);

  const [modalOpen, setModalOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);

  const [detailPost, setDetailPost] = useState(null);
  const [liked, setLiked] = useState(false);

  useEffect(() => {
    getList("REVIEW").then((data) => setPosts(data));
  }, [refresh]);

  const visiblePosts = useMemo(() => {
    let result = posts;

    if (activeCategory) {
      result = result.filter((p) => p.category === activeCategory);
    }

    if (activeRating) {
      result = result.filter((p) => (p.rating || 0) >= activeRating);
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
  }, [posts, activeCategory, activeRating, keyword, sort]);

  useEffect(() => {
    setPage(1);
  }, [activeCategory, activeRating, keyword, sort]);

  const pagedPosts = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return visiblePosts.slice(start, start + PAGE_SIZE);
  }, [visiblePosts, page]);

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
        <section className="text-center pt-28 pb-6 bg-brand-light">
          <CommunityHeaderArt className="w-28 h-20 mx-auto mb-1" />
          <TapeLabel tone="white" className="mb-3">
            REVIEW BOARD
          </TapeLabel>
          <p className="font-serif text-3xl text-brand-deep mb-2">후기게시판</p>
          <p className="text-sm text-brand-accent">
            먼저 경험한 예비 부부들의 솔직한 후기
          </p>
        </section>

        <div className="max-w-[1140px] mx-auto px-6 pt-6">
          <BoardTopTabs active="REVIEW" />
        </div>

        <div className="max-w-[1140px] mx-auto px-6 flex">
          <BoardFilterSidebar
            groups={[
              {
                title: "업체 카테고리",
                options: VENDOR_CATEGORIES.map((c) => ({ value: c, label: c })),
                activeValue: activeCategory,
                onSelect: setActiveCategory,
                resetLabel: "전체",
              },
              {
                title: "별점",
                options: RATING_OPTIONS,
                activeValue: activeRating,
                onSelect: setActiveRating,
                resetLabel: "전체",
              },
            ]}
          />

          <main className="flex-1 py-8 min-w-0">
            <div className="flex items-center justify-between mb-4">
              <p className="text-sm text-ink-muted">
                {activeCategory || "전체"}
                {activeRating ? ` · ${activeRating}점 이상` : ""} ·{" "}
                {visiblePosts.length}건
              </p>
              <button
                type="button"
                onClick={() => setModalOpen(true)}
                className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
              >
                + 후기 작성
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
              {pagedPosts.map((post) => (
                <PostCard
                  key={post.boardId}
                  post={post}
                  onClick={() => openDetail(post.boardId)}
                />
              ))}
            </div>

            <BoardPagination
              currentPage={page}
              totalItems={visiblePosts.length}
              pageSize={PAGE_SIZE}
              onPageChange={setPage}
            />
          </main>
        </div>
      </div>

      {modalOpen && (
        <BoardFormModal
          mode="add"
          fixedType="REVIEW"
          categoryOptions={VENDOR_CATEGORIES}
          onSubmit={handleAdd}
          onClose={() => setModalOpen(false)}
        />
      )}

      {editTarget && (
        <BoardFormModal
          mode="edit"
          editTarget={editTarget}
          fixedType="REVIEW"
          categoryOptions={VENDOR_CATEGORIES}
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

export default ReviewBoardPage;
