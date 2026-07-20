import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getCompanyImageUrl, getOne } from "../../api/companyApi";
import { buildCompanyOptions } from "../../util/companyOptionBuilder";
import {
  checkCompanyWish,
  addCompanyWish,
  removeCompanyWish,
} from "../../api/companywishApi";

// MakeupPackageType(백엔드 enum) -> companyOptionBuilder.buildCompanyOptions()가 만드는 옵션 key.
// buildCompanyOptions는 예약 페이지와 동일한 로직이라, 여기 매핑만 맞으면 예약가와 100% 같은 값이 나온다.
const MAKEUP_TYPE_TO_OPTION_KEY = {
  HAIR: "makeup-single-hair",
  MAKEUP: "makeup-single-makeup",
  NAIL: "makeup-single-nail",
  HAIR_MAKEUP: "makeup-pair-hair-makeup",
  HAIR_NAIL: "makeup-pair-hair-nail",
  MAKEUP_NAIL: "makeup-pair-makeup-nail",
  FULL: "makeup-triple-hair-makeup-nail",
};

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

// 슬롯(홀/드레스/스튜디오/메이크업) 상태를 항상 보여주는 사이드 패널. 제외된 카테고리도
// "제외됨"으로 표시하고 "다시 찾기"로 되돌릴 수 있게 함 (예전엔 이름 없는 행을 그냥 숨겨서
// 뭐가 빠졌는지, 되돌릴 방법이 있는지도 안 보였음).
// "확정" 버튼을 누르면 그 즉시 서버에 CONFIRMED로 반영되고, 그 다음부터는
// 리파인(다듬기) 자유발화가 이 슬롯을 절대 건드리지 못한다 (백엔드 가드).
const SidePanel = ({ combo, sessionId, onSlotAction }) => {
  const [pending, setPending] = useState(null);

  const rows = [
    { key: "HALL", label: "홀", name: combo.hallName, status: combo.hallStatus },
    { key: "DRESS", label: "드레스", name: combo.dressName, status: combo.dressStatus },
    { key: "STUDIO", label: "스튜디오", name: combo.studioName, status: combo.studioStatus },
    { key: "MAKEUP", label: "메이크업", name: combo.makeupName, status: combo.makeupStatus },
  ].filter((row) => row.name || row.status === "EXCLUDED");

  if (rows.length === 0) return null;

  const handleClick = async (row) => {
    if (!sessionId || !onSlotAction) return;
    const nextAction =
      row.status === "EXCLUDED" ? "RECONSIDER" : row.status === "CONFIRMED" ? "UNLOCK" : "CONFIRM";

    setPending(row.key);
    try {
      await onSlotAction(row.key, nextAction);
    } finally {
      setPending(null);
    }
  };

  return (
    <div className="mb-6 rounded-2xl border border-line bg-surface p-5">
      <p className="mb-3 text-xs font-medium text-ink-muted">현재 조합 현황</p>
      <ul className="space-y-2">
        {rows.map((row) => {
          const isConfirmed = row.status === "CONFIRMED";
          const isExcluded = row.status === "EXCLUDED";
          const disabled = !sessionId || pending === row.key;

          return (
            <li
              key={row.label}
              className="flex items-center justify-between text-sm"
            >
              <span className={isExcluded ? "text-ink-faint" : "text-ink-soft"}>
                {row.label} · {isExcluded ? "제외됨" : row.name}
              </span>
              <button
                type="button"
                onClick={() => handleClick(row)}
                disabled={disabled}
                className={`rounded-full px-2.5 py-0.5 text-xs font-medium transition disabled:opacity-60 ${
                  isConfirmed
                    ? "bg-brand text-white hover:bg-brand-dark"
                    : "bg-blush-100 text-brand-deep hover:bg-blush-200"
                }`}
              >
                {pending === row.key
                  ? "처리중..."
                  : isExcluded
                    ? "다시 찾기"
                    : isConfirmed
                      ? "확정됨 · 해제"
                      : "확정하기"}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
};

// 이미지가 없거나 깨지면 브라우저 깨진 아이콘 대신 "이미지 없음" placeholder를 보여줌.
// thumbnail=false로 원본 화질 요청 + aspect-square w-full이라 카드 폭에 맞춰 자동으로 커짐.
// cmno가 있으면 사진도 이름과 동일하게 상세페이지(/companies/read/:cmno) 링크 - 예약은 사진/이름
// 클릭이 아니라 아래 체크박스 패널("선택한 업체 예약 진행")이 유일한 진입점이라, 사진 클릭에서
// 예약으로 바로 보내면 오히려 헷갈려서 상세보기로 통일함.
const SlotThumb = ({ fileName, alt, cmno }) => {
  const [hasError, setHasError] = useState(false);
  const url = fileName ? getCompanyImageUrl(fileName, false) : "";

  const image =
    !url || hasError ? (
      <div className="flex aspect-square w-full items-center justify-center rounded-xl bg-slate-50 text-xs text-slate-400">
        이미지 없음
      </div>
    ) : (
      <img
        src={url}
        alt={alt}
        onError={() => setHasError(true)}
        className="aspect-square w-full rounded-xl object-cover"
      />
    );

  if (!cmno) {
    return image;
  }

  return (
    <a
      href={`/companies/read/${cmno}`}
      target="_blank"
      rel="noopener noreferrer"
      className="block overflow-hidden rounded-xl"
    >
      {image}
    </a>
  );
};

// 업체 상세페이지에 이미 있는 찜(♥) 기능 그대로 재사용 - 지금까진 카드에서 찜하려면 상세페이지까지
// 들어가야 했음. 비로그인이면 addCompanyWish/checkCompanyWish가 401을 내는데, 그건 그냥 빈 하트로
// 두고 클릭 시에만 안내한다 (페이지 전체를 에러로 만들지 않기 위해 조용히 무시).
const FavoriteButton = ({ cmno }) => {
  const [liked, setLiked] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    checkCompanyWish(cmno)
      .then((data) => {
        if (!cancelled) setLiked(typeof data === "boolean" ? data : Boolean(data?.liked));
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [cmno]);

  const handleClick = async () => {
    if (loading) return;
    setLoading(true);
    try {
      if (liked) {
        await removeCompanyWish(cmno);
        setLiked(false);
      } else {
        await addCompanyWish(cmno);
        setLiked(true);
      }
    } catch (err) {
      console.error(err);
      alert("로그인 후 찜하기를 이용할 수 있어요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={loading}
      aria-label={liked ? "찜 해제" : "찜하기"}
      className={`absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full text-sm shadow-sm transition disabled:opacity-60 ${
        liked ? "bg-white text-rose-500" : "bg-white/80 text-ink-faint hover:text-rose-400"
      }`}
    >
      {liked ? "♥" : "♡"}
    </button>
  );
};

// 이 슬롯이 리파인 대화에서 EXCLUDED로 빠졌으면 name이 null로 옴 - 그럴 땐 아예 렌더링 안 함.
// cmno가 있으면 업체명은 상세페이지(/companies/read/:cmno)로 새 탭 링크.
// price는 카테고리별 참고 가격(Company.priceAvg) - "홀은 얼마, 드레스는 얼마" 요청으로 추가.
// optionName은 업체명과 별개로 "구체적으로 뭘 추천했는지" - 홀은 연회장 이름(hallRoomName),
// 드레스는 옵션(아이템) 이름(dressOptionName). 스튜디오/메이크업은 아직 이 단위 데이터가 없어서 null.
// packageType(메이크업 패키지 취향)이 있으면 그 업체의 실제 옵션가를 다시 계산해서 보여준다 -
// 없으면(홀/드레스/스튜디오, 또는 취향 없이 고른 메이크업) price prop을 그대로 씀.
// 조회 실패/매칭 실패 시에도 조용히 price prop으로 폴백 - 카드 렌더링을 막지 않는다.
const SlotCard = ({ label, cmno, name, optionName, imageUrl, reason, price, packageType }) => {
  const [resolvedPrice, setResolvedPrice] = useState(null);

  useEffect(() => {
    if (!cmno || !packageType) {
      setResolvedPrice(null);
      return;
    }
    let cancelled = false;
    getOne(cmno)
      .then((company) => {
        if (cancelled) return;
        const options = buildCompanyOptions(company);
        const optionKey = MAKEUP_TYPE_TO_OPTION_KEY[packageType];
        const matched = options.find((opt) => opt.key === optionKey);
        setResolvedPrice(matched ? matched.price : null);
      })
      .catch(() => {
        if (!cancelled) setResolvedPrice(null);
      });
    return () => {
      cancelled = true;
    };
  }, [cmno, packageType]);

  if (!name) return null;

  const text = `${label} · ${name}`;
  const displayPrice = resolvedPrice != null ? resolvedPrice : price;

  return (
    <div className="relative">
      <SlotThumb fileName={imageUrl} alt={name} cmno={cmno} />
      {cmno && <FavoriteButton cmno={cmno} />}
      {cmno ? (
        <a
          href={`/companies/read/${cmno}`}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-2 block truncate text-sm text-ink-soft hover:text-brand-deep hover:underline"
        >
          {text}
        </a>
      ) : (
        <p className="mt-2 truncate text-sm text-ink-soft">{text}</p>
      )}
      {optionName && (
        <span className="block truncate text-xs text-ink-soft">{optionName}</span>
      )}
      {displayPrice != null && (
        <span className="block text-xs font-medium text-ink-muted">{formatWon(displayPrice)}</span>
      )}
      {reason && <span className="block text-xs text-ink-faint">{reason}</span>}
    </div>
  );
};

const SLOT_DEFS = [
  { key: "hall", label: "홀" },
  { key: "dress", label: "드레스" },
  { key: "studio", label: "스튜디오" },
  { key: "makeup", label: "메이크업" },
];

// 조합에서 실제로 예약 가능한(=EXCLUDED 아닌) 슬롯만 {cmno, label, name} 형태로 뽑음
const reservableSlots = (combo) =>
  SLOT_DEFS.map(({ key, label }) => ({
    cmno: combo[`${key}Cmno`],
    label,
    name: combo[`${key}Name`],
  })).filter((slot) => slot.cmno);

const ResultCards = ({ result, onSlotAction }) => {
  const navigate = useNavigate();

  // 예약 체크박스 선택 상태 - 조합의 예약 가능 슬롯 구성이 바뀌면(다듬기로 카테고리가
  // 빠지거나 새 결과로 바뀌면) "전부 선택"으로 리셋한다. fingerprint로 변화를 감지해서
  // 렌더 중에 바로 조정한다(리액트에서 권장하는 "렌더 중 상태 조정" 패턴).
  const [selectedCmnos, setSelectedCmnos] = useState(() => new Set());
  const [selectionFingerprint, setSelectionFingerprint] = useState(null);

  const candidates = result?.candidates ?? [];
  const message = result?.message;
  const sessionId = result?.sessionId;
  const soleCombo =
    candidates.length === 1 &&
    ["INDIVIDUAL_COMBO", "AI_COMBO", "AI_FALLBACK", "SESSION_COMBO"].includes(
      candidates[0].sourceType,
    )
      ? candidates[0]
      : null;

  const slots = soleCombo ? reservableSlots(soleCombo) : [];
  const fingerprint = slots.map((s) => s.cmno).join(",");

  if (soleCombo && fingerprint !== selectionFingerprint) {
    setSelectionFingerprint(fingerprint);
    setSelectedCmnos(new Set(slots.map((s) => s.cmno)));
  }

  if (!result) return null;

  const toggleSlot = (cmno) => {
    setSelectedCmnos((prev) => {
      const next = new Set(prev);
      if (next.has(cmno)) {
        next.delete(cmno);
      } else {
        next.add(cmno);
      }
      return next;
    });
  };

  // 체크된 업체만 큐에 담아 첫 업체 예약 페이지로 보낸다 - 그 업체 예약이 끝나면
  // ReservationReserveComponent가 알아서 다음 큐 항목으로 이어준다 (flow=aiplan 파라미터).
  const handleReserveCombo = () => {
    const cmnos = slots.filter((s) => selectedCmnos.has(s.cmno)).map((s) => s.cmno);
    if (cmnos.length === 0) return;

    const [first, ...rest] = cmnos;
    const params = new URLSearchParams({
      flow: "aiplan",
      flowIndex: "1",
      flowTotal: String(cmnos.length),
      queue: rest.join(","),
    });
    if (sessionId) params.set("returnSessionId", sessionId);

    navigate(`/companies/reserve/${first}?${params.toString()}`);
  };

  // 조합이 1개일 때(다듬은 조합/AI조합 등)는 위의 "현재 조합 현황" 박스와 폭을 맞추기 위해
  // md:grid-cols-2를 안 씀 - 안 그러면 카드가 절반 폭에 갇히고 오른쪽에 빈 칸이 생김.
  // 패키지가 여러 개 뜨는 빠르게 모드에서만 2열로 나열함.
  const gridClass = soleCombo
    ? "grid grid-cols-1 gap-5"
    : "grid grid-cols-1 gap-5 md:grid-cols-2";

  return (
    <div>
      {result?.weddingDate && (
        <p className="mb-4 text-center text-sm text-ink-muted">
          결혼 예정일 · {result.weddingDate}
        </p>
      )}

      {message && (
        <div className="mb-5 rounded-xl border border-line bg-surface px-5 py-3 text-sm text-ink-soft">
          {message}
        </div>
      )}

      {soleCombo && (
        <SidePanel combo={soleCombo} sessionId={sessionId} onSlotAction={onSlotAction} />
      )}

      {soleCombo && slots.length > 0 && (
        <div className="mb-6 rounded-2xl border border-brand-dark bg-blush-50 p-5">
          <p className="mb-3 text-center text-sm text-ink-soft">
            예약 진행할 업체를 골라주세요
          </p>
          <ul className="mb-4 space-y-1">
            {slots.map((slot) => (
              <li key={slot.cmno}>
                <label className="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-ink-soft hover:bg-white/60">
                  <input
                    type="checkbox"
                    checked={selectedCmnos.has(slot.cmno)}
                    onChange={() => toggleSlot(slot.cmno)}
                    className="h-4 w-4 rounded border-line accent-brand-dark"
                  />
                  <span>
                    {slot.label} · {slot.name}
                  </span>
                </label>
              </li>
            ))}
          </ul>
          <div className="flex flex-col items-center gap-2">
            <button
              type="button"
              onClick={handleReserveCombo}
              disabled={selectedCmnos.size === 0}
              className="h-11 rounded-full bg-brand px-6 text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-50"
            >
              선택한 업체 예약 진행 ({selectedCmnos.size}곳)
            </button>
            <p className="text-xs text-ink-faint">
              업체별로 순서대로 예약 페이지로 안내해드려요. 로그인이 필요해요.
            </p>
          </div>
        </div>
      )}

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
                  cmno={c.hallCmno}
                  name={c.hallName}
                  optionName={c.hallRoomName}
                  imageUrl={c.hallImageUrl}
                  reason={c.hallReason}
                  price={c.hallPrice}
                />
                <SlotCard
                  label="드레스"
                  cmno={c.dressCmno}
                  name={c.dressName}
                  optionName={c.dressOptionName}
                  imageUrl={c.dressImageUrl}
                  reason={c.dressReason}
                  price={c.dressPrice}
                />
                <SlotCard
                  label="스튜디오"
                  cmno={c.studioCmno}
                  name={c.studioName}
                  imageUrl={c.studioImageUrl}
                  reason={c.studioReason}
                  price={c.studioPrice}
                />
                <SlotCard
                  label="메이크업"
                  cmno={c.makeupCmno}
                  name={c.makeupName}
                  imageUrl={c.makeupImageUrl}
                  reason={c.makeupReason}
                  price={c.makeupPrice}
                  packageType={c.makeupPackageType}
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
