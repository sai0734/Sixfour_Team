const formatWon = (value) => {
  if (value === null || value === undefined) return "-";
  return `${Number(value).toLocaleString()}원`;
};

const SOURCE_LABEL = {
  PACKAGE: "패키지 할인가",
  INDIVIDUAL_COMBO: "개별 조합",
};

// 슬롯(홀/드레스/스튜디오/메이크업) 상태를 항상 보여주는 사이드 패널.
// 지금은 "선택됨" 고정 상태만 표시 - 확정/제외/재검토를 실제로 바꾸는 건
// 리파인 대화(6단계)에서 AiPlanSession과 연결한 다음에 붙일 예정.
const SidePanel = ({ combo }) => {
  const rows = [
    { label: "홀", name: combo.hallName },
    { label: "드레스", name: combo.dressName },
    { label: "스튜디오", name: combo.studioName },
    { label: "메이크업", name: combo.makeupName },
  ];

  return (
    <div className="mb-6 rounded-2xl border border-line bg-surface p-5">
      <p className="mb-3 text-xs font-medium text-ink-muted">현재 조합 현황</p>
      <ul className="space-y-2">
        {rows.map((row) => (
          <li
            key={row.label}
            className="flex items-center justify-between text-sm"
          >
            <span className="text-ink-soft">
              {row.label} · {row.name}
            </span>
            <span className="rounded-full bg-blush-100 px-2.5 py-0.5 text-xs font-medium text-brand-deep">
              선택됨
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
};

const ResultCards = ({ result }) => {
  if (!result) return null;

  const { candidates = [], message } = result;
  const soleCombo =
    candidates.length === 1 && candidates[0].sourceType === "INDIVIDUAL_COMBO"
      ? candidates[0]
      : null;

  return (
    <div>
      {message && (
        <div className="mb-5 rounded-xl border border-line bg-surface px-5 py-3 text-sm text-ink-soft">
          {message}
        </div>
      )}

      {soleCombo && <SidePanel combo={soleCombo} />}

      {candidates.length === 0 ? (
        <div className="rounded-2xl border border-line bg-white p-10 text-center text-ink-faint">
          추천할 수 있는 조합을 찾지 못했어요.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          {candidates.map((c, idx) => (
            <div
              key={c.pno ?? `combo-${idx}`}
              className="rounded-2xl border border-line bg-white p-6 shadow-sm"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="rounded-full bg-blush-100 px-3 py-1 text-xs font-medium text-brand-deep">
                  {SOURCE_LABEL[c.sourceType] || c.sourceType}
                </span>
                <span className="text-sm font-semibold text-ink">
                  {formatWon(c.packagePrice)}
                </span>
              </div>

              <h3 className="mb-1 font-display text-lg text-ink">{c.name}</h3>
              {c.description && (
                <p className="mb-3 text-xs text-ink-faint">{c.description}</p>
              )}

              <ul className="mb-3 space-y-1 text-sm text-ink-soft">
                <li>홀 · {c.hallName}</li>
                <li>드레스 · {c.dressName}</li>
                <li>스튜디오 · {c.studioName}</li>
                <li>메이크업 · {c.makeupName}</li>
              </ul>

              <p className="border-t border-line pt-3 text-xs text-ink-muted">
                {c.reason}
              </p>
            </div>
          ))}
        </div>
      )}

      <p className="mt-6 text-center text-xs text-ink-faint">
        표시된 가격은 참고용이며, 실제 금액은 예약 시 옵션 선택에서 확정돼요.
      </p>
    </div>
  );
};

export default ResultCards;
