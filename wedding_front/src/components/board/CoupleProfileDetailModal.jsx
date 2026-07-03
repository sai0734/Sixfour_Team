const CoupleProfileDetailModal = ({ profile, matchScore, onClose }) => {
  if (!profile) return null;

  const initials = profile.memberEmail?.[0]?.toUpperCase() || "?";

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-12 h-12 rounded-full bg-brand-light text-brand-accent flex items-center justify-center font-medium">
            {initials}
          </div>
          <div className="flex-1">
            <p className="text-sm font-medium text-ink">
              {profile.memberEmail}
            </p>
            {profile.weddingDate && (
              <p className="text-xs text-ink-faint">
                {profile.weddingDate} 결혼
              </p>
            )}
          </div>
          {matchScore !== null && (
            <span className="text-sm font-medium text-brand">
              매칭도 {matchScore}%
            </span>
          )}
        </div>

        <div className="flex flex-wrap gap-2 mb-4">
          {profile.region && (
            <span className="text-[11px] bg-surface text-ink-muted px-2.5 py-1 rounded-full">
              {profile.region}
            </span>
          )}
          {profile.budgetMin != null && profile.budgetMax != null && (
            <span className="text-[11px] bg-surface text-ink-muted px-2.5 py-1 rounded-full">
              {profile.budgetMin.toLocaleString()}~
              {profile.budgetMax.toLocaleString()}만원
            </span>
          )}
          {profile.weddingStyle && (
            <span className="text-[11px] bg-surface text-ink-muted px-2.5 py-1 rounded-full">
              {profile.weddingStyle}
            </span>
          )}
        </div>

        <p className="text-sm text-ink whitespace-pre-wrap leading-relaxed mb-6">
          {profile.bio || "자기소개가 아직 없습니다."}
        </p>

        <div className="flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="h-9 px-4 rounded-full bg-brand text-white text-xs font-medium hover:bg-brand-dark"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};

export default CoupleProfileDetailModal;
