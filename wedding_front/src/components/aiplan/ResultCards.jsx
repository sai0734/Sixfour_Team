import { useState } from "react";
import { getCompanyImageUrl } from "../../api/companyApi";

const formatWon = (value) => {
  if (value === null || value === undefined) return "-";
  return `${Number(value).toLocaleString()}원`;
};

const SOURCE_LABEL = {
  PACKAGE: "패키지 할인가",
  INDIVIDUAL_COMBO: "개별 조합",
  AI_COMBO: "AI 추천",
  AI_FALLBACK: "개별 조합",
  SESSION_COMBO: "다듬은 조합",
};

// 슬롯(홀/드레스/스튜디오/메이크업) 상태를 항상 보여주는 사이드 패널.
const SidePanel = ({ combo }) => {
  const rows = [
    { label: "홀", name: combo.hallName },
    { label: "드레스", name: combo.dressName },
    { label: "스튜디오", name: combo.studioName },
    { label: "메이크업", name: combo.makeupName },
  ].filter((row) => row.name);

  if (rows.length === 0) return null;

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

// 이미지가 없거나 깨지면 브라우저 깨진 아이콘 대신 "이미지 없음" placeholder를 보여줌.
// thumbnail=false로 원본 화질 요청 + aspect-square w-full이라 카드 폭에 맞춰 자동으로 커짐.
const SlotThumb = ({ fileName, alt }) => {
  const [hasError, setHasError] = useState(false);
  const url = fileName ? getCompanyImageUrl(fileName, false) : "";

  if (!url || hasError) {
    return (
      <div className="flex aspect-square w-full items-center justify-center rounded-xl bg-slate-50 text-xs text-slate-400">
        이미지 없음
      </div>
    );
  }

  return (
    <img
      src={url}
      alt={alt}
      onError={() => setHasError(true)}
      className="aspect-square w-full rounded-xl object-cover"
    />
  );
};

// 이 슬롯이 리파인 대화에서 EXCLUDED로 빠졌으면 name이 null로 옴 - 그럴 땐 아예 렌더링 안 함.
const SlotCard = ({ label, name, imageUrl, reason }) => {
  if (!name) return null;

  return (
    <div>
      <SlotThumb fileName={imageUrl} alt={name} />
      <p className="mt-2 truncate text-sm text-ink-soft">
        {label} · {name}
      </p>
      {reason && <span className="block text-xs text-ink-faint">{reason}</span>}
    </div>
  );
};

const ResultCards = ({ result }) => {
  if (!result) return null;

  const { candidates = [], message } = result;
  const soleCombo =
    candidates.length === 1 &&
    ["INDIVIDUAL_COMBO", "AI_COMBO", "AI_FALLBACK", "SESSION_COMBO"].includes(
      candidates[0].sourceType,
    )
      ? candidates[0]
      : null;

  // 조합이 1개일 때(다듬은 조합/AI조합 등)는 위의 "현재 조합 현황" 박스와 폭을 맞추기 위해
  // md:grid-cols-2를 안 씀 - 안 그러면 카드가 절반 폭에 갇히고 오른쪽에 빈 칸이 생김.
  // 패키지가 여러 개 뜨는 빠르게 모드에서만 2열로 나열함.
  const gridClass = soleCombo
    ? "grid grid-cols-1 gap-5"
    : "grid grid-cols-1 gap-5 md:grid-cols-2";

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
        <div className={gridClass}>
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

              <div className="mb-3 grid grid-cols-2 gap-3 text-sm text-ink-soft">
                <SlotCard
                  label="홀"
                  name={c.hallName}
                  imageUrl={c.hallImageUrl}
                  reason={c.hallReason}
                />
                <SlotCard
                  label="드레스"
                  name={c.dressName}
                  imageUrl={c.dressImageUrl}
                  reason={c.dressReason}
                />
                <SlotCard
                  label="스튜디오"
                  name={c.studioName}
                  imageUrl={c.studioImageUrl}
                  reason={c.studioReason}
                />
                <SlotCard
                  label="메이크업"
                  name={c.makeupName}
                  imageUrl={c.makeupImageUrl}
                  reason={c.makeupReason}
                />
              </div>

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
