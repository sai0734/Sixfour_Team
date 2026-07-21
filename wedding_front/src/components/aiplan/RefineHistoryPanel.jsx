import { useState } from "react";
import { getRefineHistory } from "../../api/aiPlanApi";

// 다듬기(리파인) 대화 기록 - 이 세션에서 지금까지 뭐라고 말했었는지 펼쳐서 다시 볼 수 있게.
// "되돌리기"는 한 단계만 되지만, 이건 그냥 조회용이라 몇 턴이든 훑어볼 수 있음.
// 기록 항목을 누르면 그 다듬기 작업이 반영된 결과 박스(위쪽 "다듬은 조합"·가격 박스)로
// 스크롤 이동한다 - 기록만 보고 실제 반영된 결과가 어디 있는지 매번 위로 스크롤해서 찾아야
// 했던 불편을 없애기 위함.
const scrollToComboSummary = () => {
  const el = document.getElementById("ai-plan-combo-summary");
  if (el) el.scrollIntoView({ behavior: "smooth", block: "center" });
};

const RefineHistoryPanel = ({ sessionId }) => {
  const [open, setOpen] = useState(false);
  const [turns, setTurns] = useState([]);
  const [loading, setLoading] = useState(false);

  const toggle = () => {
    const next = !open;
    setOpen(next);

    if (next && turns.length === 0) {
      setLoading(true);
      getRefineHistory(sessionId)
        .then((data) => setTurns(data))
        .catch((err) => console.error(err))
        .finally(() => setLoading(false));
    }
  };

  return (
    <div className="mt-3 text-center">
      <button
        type="button"
        onClick={toggle}
        className="text-xs font-medium text-ink-muted hover:text-ink-soft"
      >
        {open ? "다듬은 기록 숨기기 ▲" : "다듬은 기록 보기 ▼"}
      </button>
      {open && (
        <ul className="mt-2 space-y-1.5 rounded-xl border border-line bg-white p-3 text-left text-xs text-ink-soft">
          {loading && <li className="text-ink-faint">불러오는 중...</li>}
          {!loading && turns.length === 0 && (
            <li className="text-ink-faint">기록이 없어요.</li>
          )}
          {turns.map((turn) => (
            <li
              key={turn.turnNo}
              className="border-b border-line-soft pb-1.5 last:border-0 last:pb-0"
            >
              <button
                type="button"
                onClick={scrollToComboSummary}
                className="w-full rounded px-1 py-0.5 text-left hover:bg-blush-50 hover:text-brand-deep"
                title="이 작업이 반영된 결과로 이동"
              >
                <span className="mr-1.5 text-ink-faint">{turn.turnNo}.</span>
                {turn.message}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default RefineHistoryPanel;
