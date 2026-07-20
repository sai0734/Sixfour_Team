// AI 웨딩플랜 - 회차별 추천 기록(사이드바)을 브라우저에 남겨두는 용도.
// 세션 자체는 서버(AiPlanSession)에 있지만, "1번째 추천/2번째 추천" 같은 회차 목록과
// 사용자가 붙인 이름은 순전히 이 브라우저에서만 의미 있는 라벨이라 로컬에만 저장한다.
const HISTORY_KEY = "aiplan_session_history";

const readHistory = () => {
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const writeHistory = (history) => {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
};

export const getSessionHistory = () => readHistory();

// 새 세션이 생기면 회차 목록 맨 뒤에 추가. 이미 아는 세션(다듬기/확정/되돌리기로 재사용된 것)이면
// 아무것도 안 바꾸고 그대로 돌려줌 - 새 "회차"는 오직 새 세션이 만들어질 때만 생긴다.
// meta(예산/지역)는 사이드바에서 마우스 올렸을 때 보여줄 요약용 - 없어도 동작엔 지장 없음.
export const addSessionHistoryEntry = (sessionId, meta = {}) => {
  const history = readHistory();
  const id = String(sessionId);

  if (history.some((entry) => entry.sessionId === id)) {
    return history;
  }

  const next = [
    ...history,
    {
      sessionId: id,
      label: `${history.length + 1}번째 추천`,
      budgetManwon: meta.budgetManwon || null,
      region: meta.region || null,
    },
  ];
  writeHistory(next);
  return next;
};

export const renameSessionHistoryEntry = (sessionId, label) => {
  const history = readHistory();
  const next = history.map((entry) =>
    entry.sessionId === String(sessionId) ? { ...entry, label } : entry,
  );
  writeHistory(next);
  return next;
};

// 서버 세션은 그대로 두고, 이 브라우저의 회차 목록에서만 지운다 (기록 정리용)
export const removeSessionHistoryEntry = (sessionId) => {
  const history = readHistory();
  const next = history.filter((entry) => entry.sessionId !== String(sessionId));
  writeHistory(next);
  return next;
};
