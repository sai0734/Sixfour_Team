import { useEffect, useState } from "react";
import BasicMenu from "../../components/menus/BasicMenu";
import BoardTopTabs from "../../components/board/BoardTopTabs";
import BoardFilterSidebar from "../../components/board/BoardFilterSidebar";
import FaqItem from "../../components/faq/FaqItem";
import { getList } from "../../api/faqApi";

// 실제 카테고리 값이 아니라, "좋아요(도움됐어요) 많은 순 10개"를 보여주는 특수 탭.
// 서버에 별도 카테고리로 저장돼 있는 게 아니라 프론트에서 전체를 받아 정렬 후 상위 10개만 자름.
const RECOMMENDED = "추천";

const CATEGORY_OPTIONS = [
  RECOMMENDED,
  "회원·로그인",
  "예산",
  "업체·예약",
  "일정",
  "답례품 쇼핑몰",
  "커뮤니티",
  "기타",
];

const FaqPage = () => {
  const [faqs, setFaqs] = useState([]);
  // FAQ 페이지 진입 시 "추천"(도움됐어요 많은 순 10개)을 바로 보여준다.
  const [categoryFilter, setCategoryFilter] = useState(RECOMMENDED);
  const [openIds, setOpenIds] = useState(new Set());

  useEffect(() => {
    if (categoryFilter === RECOMMENDED) {
      // 카테고리 제한 없이 전체를 받아서 likeCount 기준 상위 10개만 보여준다.
      getList(null).then((data) => {
        const sorted = [...data].sort(
          (a, b) => (b.likeCount || 0) - (a.likeCount || 0),
        );
        setFaqs(sorted.slice(0, 10));
      });
      return;
    }

    getList(categoryFilter).then((data) => setFaqs(data));
  }, [categoryFilter]);

  const toggleOpen = (faqId) => {
    setOpenIds((prev) => {
      const next = new Set(prev);
      if (next.has(faqId)) {
        next.delete(faqId);
      } else {
        next.add(faqId);
      }
      return next;
    });
  };

  const handleLikeChange = (faqId, delta) => {
    setFaqs((prev) =>
      prev.map((f) =>
        f.faqId === faqId ? { ...f, likeCount: f.likeCount + delta } : f,
      ),
    );
  };

  return (
    <>
      <BasicMenu />

      <div className="bg-cream min-h-screen">
        <section className="text-center pt-12 pb-6 bg-brand-light">
          <p className="text-xs tracking-[0.15em] text-brand-accent mb-2.5">
            FREQUENTLY ASKED QUESTIONS
          </p>
          <p className="font-serif text-3xl text-brand-deep mb-2">
            자주 묻는 질문
          </p>
          <p className="text-sm text-brand-accent">
            궁금한 점을 먼저 확인해보세요
          </p>
        </section>

        <div className="max-w-[1140px] mx-auto px-6 pt-6">
          <BoardTopTabs active="FAQ" />
        </div>

        <div className="max-w-[1140px] mx-auto px-6 flex">
          <BoardFilterSidebar
            groups={[
              {
                title: "카테고리",
                options: CATEGORY_OPTIONS.map((c) => ({ value: c, label: c })),
                activeValue: categoryFilter,
                onSelect: setCategoryFilter,
                hideReset: true,
              },
            ]}
          />

          <main className="flex-1 py-8 min-w-0 pb-20">
            <p className="text-sm text-ink-muted mb-1">
              {categoryFilter} · {faqs.length}건
            </p>
            {categoryFilter === RECOMMENDED && (
              <p className="text-xs text-ink-faint mb-3">
                도움됐어요가 많은 순으로 최대 10개까지 보여줘요
              </p>
            )}

            {faqs.length === 0 && (
              <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
                등록된 FAQ가 없습니다.
              </div>
            )}

            <div className="flex flex-col gap-3">
              {faqs.map((faq) => (
                <FaqItem
                  key={faq.faqId}
                  faq={faq}
                  open={openIds.has(faq.faqId)}
                  onToggle={() => toggleOpen(faq.faqId)}
                  onLikeChange={handleLikeChange}
                />
              ))}
            </div>
          </main>
        </div>
      </div>
    </>
  );
};

export default FaqPage;
