// AI 없이 규칙 기반으로 매칭도(%) 계산: 지역/스타일/예산겹침/날짜근접도 가중합
export const calcMatchScore = (mine, other) => {
  if (!mine || !other) return null;

  let score = 0;

  if (mine.region && mine.region === other.region) {
    score += 30;
  }

  if (mine.weddingStyle && mine.weddingStyle === other.weddingStyle) {
    score += 30;
  }

  if (
    mine.budgetMin != null &&
    mine.budgetMax != null &&
    other.budgetMin != null &&
    other.budgetMax != null
  ) {
    const overlap =
      Math.min(mine.budgetMax, other.budgetMax) -
      Math.max(mine.budgetMin, other.budgetMin);
    if (overlap > 0) {
      score += 25;
    }
  }

  if (mine.weddingDate && other.weddingDate) {
    const diffDays =
      Math.abs(new Date(mine.weddingDate) - new Date(other.weddingDate)) /
      (1000 * 60 * 60 * 24);
    if (diffDays <= 90) {
      score += 15;
    }
  }

  return Math.min(100, score);
};
