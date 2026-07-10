import { useEffect, useState } from "react";
import { checkLiked, likeOne, unlikeOne } from "../../api/faqLikeApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const FaqItem = ({ faq, open, onToggle, onLikeChange }) => {
  const { loginState } = useCustomLogin();

  const [liked, setLiked] = useState(false);

  useEffect(() => {
    if (!loginState.email) return;

    checkLiked(faq.faqId, loginState.email).then((result) => setLiked(result));
  }, [faq.faqId, loginState.email]);

  const handleToggleLike = (e) => {
    e.stopPropagation();

    const action = liked
      ? unlikeOne(faq.faqId, loginState.email)
      : likeOne(faq.faqId, loginState.email);

    action
      .then(() => {
        setLiked(!liked);
        onLikeChange(faq.faqId, liked ? -1 : 1);
      })
      .catch((e) => console.error(e));
  };

  return (
    <div className="bg-white rounded-2xl border border-line overflow-hidden">
      <div
        onClick={onToggle}
        className="flex items-center gap-3 px-5 py-4 cursor-pointer"
      >
        <span className="text-[11px] bg-brand-light text-brand-accent px-2.5 py-1 rounded-full font-medium shrink-0">
          {faq.category}
        </span>
        <p className="flex-1 text-sm font-medium text-ink">Q. {faq.question}</p>
        <span className="text-ink-faint text-xs shrink-0">
          {open ? "▲" : "▼"}
        </span>
      </div>

      {open && (
        <div className="px-5 pb-4">
          <p className="text-sm text-ink-soft whitespace-pre-wrap leading-relaxed border-t border-line pt-4">
            A. {faq.answer}
          </p>

          <div className="flex justify-end mt-3">
            <button
              type="button"
              onClick={handleToggleLike}
              className={`flex items-center gap-1.5 h-8 px-4 rounded-full border text-xs font-medium transition-colors ${
                liked
                  ? "bg-brand-light border-brand text-brand-accent"
                  : "border-line-soft text-ink-soft hover:bg-cream"
              }`}
            >
              👍 도움됐어요 {faq.likeCount}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default FaqItem;
