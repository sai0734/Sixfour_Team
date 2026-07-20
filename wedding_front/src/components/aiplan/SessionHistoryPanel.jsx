import { useState } from "react";

// 추천 회차 사이드바 - 회차 클릭하면 그 세션으로 이동, 연필 아이콘 누르면 이름 수정,
// × 누르면 목록에서 삭제(서버 세션은 그대로 둠), 상단 버튼으로 새 추천 바로 시작.
const SessionHistoryPanel = ({
  history,
  activeSessionId,
  onSelect,
  onRename,
  onRemove,
  onStartNew,
  disabled,
}) => {
  const [editingId, setEditingId] = useState(null);
  const [draft, setDraft] = useState("");

  if (!history || history.length === 0) {
    return null;
  }

  const startEdit = (entry) => {
    setEditingId(entry.sessionId);
    setDraft(entry.label);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setDraft("");
  };

  const commitEdit = (entry) => {
    const label = draft.trim() || entry.label;
    onRename(entry.sessionId, label);
    setEditingId(null);
  };

  return (
    <nav className="flex max-h-[calc(100vh-3rem)] flex-col rounded-2xl border border-line bg-white p-4">
      <div className="mb-3 flex items-center justify-between">
        <p className="text-xs font-medium text-ink-muted">추천 기록</p>
        <button
          type="button"
          onClick={onStartNew}
          className="text-xs font-medium text-brand-deep hover:underline"
        >
          + 새로 추천받기
        </button>
      </div>
      <ul className="min-h-0 space-y-1 overflow-y-auto">
        {history.map((entry) => {
          const isActive = String(activeSessionId) === entry.sessionId;
          const isEditing = editingId === entry.sessionId;

          if (isEditing) {
            return (
              <li key={entry.sessionId}>
                <input
                  autoFocus
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") commitEdit(entry);
                    if (e.key === "Escape") cancelEdit();
                  }}
                  onBlur={() => commitEdit(entry)}
                  className="w-full rounded-lg border border-line px-2 py-1.5 text-sm outline-none focus:border-brand-dark"
                />
              </li>
            );
          }

          const summary =
            entry.budgetManwon || entry.region
              ? `예산 ${entry.budgetManwon ? `${Number(entry.budgetManwon).toLocaleString()}만원` : "미입력"} · ${entry.region || "지역무관"}`
              : undefined;

          return (
            <li
              key={entry.sessionId}
              title={summary}
              className={`flex items-center justify-between rounded-lg px-2 py-1.5 text-sm ${
                isActive
                  ? "bg-blush-100 font-medium text-brand-deep"
                  : "text-ink-soft hover:bg-surface"
              }`}
            >
              <button
                type="button"
                onClick={() => onSelect(entry.sessionId)}
                disabled={disabled}
                className="flex-1 truncate text-left disabled:cursor-not-allowed disabled:opacity-60"
              >
                {entry.label}
              </button>
              <button
                type="button"
                onClick={() => startEdit(entry)}
                aria-label="이름 수정"
                className="ml-2 shrink-0 text-ink-faint hover:text-ink-soft"
              >
                ✎
              </button>
              <button
                type="button"
                onClick={() => onRemove(entry.sessionId)}
                aria-label="기록에서 삭제"
                className="ml-1 shrink-0 text-ink-faint hover:text-[#B23B3B]"
              >
                ×
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
};

export default SessionHistoryPanel;
