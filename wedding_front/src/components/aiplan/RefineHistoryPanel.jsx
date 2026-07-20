import { useState } from "react";
import { getRefineHistory } from "../../api/aiPlanApi";

// 다듬기(리파인) 대화 기록 - 이 세션에서 지금까지 뭐라고 말했었는지 펼쳐서 다시 볼 수 있게.
// "되돌리기"는 한 단계만 되지만, 이건 그냥 조회용이라 몇 턴이든 훑어볼 수 있음.
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
              <span className="mr-1.5 text-ink-faint">{turn.turnNo}.</span>
              {turn.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default RefineHistoryPanel;
