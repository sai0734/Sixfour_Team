import { useEffect, useState } from "react";
import BasicMenu from "../../components/menus/BasicMenu";
import BoardTopTabs from "../../components/board/BoardTopTabs";
import BoardFilterSidebar from "../../components/board/BoardFilterSidebar";
import FaqItem from "../../components/faq/FaqItem";
import { getList } from "../../api/faqApi";

const CATEGORY_OPTIONS = [
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
  const [categoryFilter, setCategoryFilter] = useState(null);
  const [openIds, setOpenIds] = useState(new Set());

  useEffect(() => {
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
              },
            ]}
          />

          <main className="flex-1 py-8 min-w-0 pb-20">
            <p className="text-sm text-ink-muted mb-4">
              {categoryFilter || "전체"} · {faqs.length}건
            </p>

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
