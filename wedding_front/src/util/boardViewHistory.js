const BOARD_VIEW_HISTORY_PREFIX = "wedding_board_view_history";
const MAX_HISTORY_COUNT = 100;

const getStorageKey = (memberEmail) => {
  return memberEmail
    ? `${BOARD_VIEW_HISTORY_PREFIX}_${memberEmail}`
    : `${BOARD_VIEW_HISTORY_PREFIX}_guest`;
};

export const getViewedBoardIds = (memberEmail) => {
  try {
    const savedValue = localStorage.getItem(getStorageKey(memberEmail));
    const parsedValue = savedValue ? JSON.parse(savedValue) : [];

    if (!Array.isArray(parsedValue)) {
      return [];
    }

    return parsedValue
      .map((id) => Number(id))
      .filter((id) => Number.isFinite(id));
  } catch (e) {
    console.error(e);
    return [];
  }
};

export const saveViewedBoard = (boardId, memberEmail) => {
  if (!boardId) return [];

  const numericBoardId = Number(boardId);
  if (!Number.isFinite(numericBoardId)) return [];

  const previousIds = getViewedBoardIds(memberEmail);
  const nextIds = [
    numericBoardId,
    ...previousIds.filter((id) => id !== numericBoardId),
  ].slice(0, MAX_HISTORY_COUNT);

  try {
    localStorage.setItem(getStorageKey(memberEmail), JSON.stringify(nextIds));
  } catch (e) {
    console.error(e);
  }

  return nextIds;
};
