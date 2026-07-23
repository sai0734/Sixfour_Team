import React, { useEffect, useRef, useState } from "react";
import { getOne, getCompanyImageUrl } from "../../api/companyApi";
import { showConfirm } from "../../util/globalConfirm";

// 마지막 대화 시각 포맷 — 오늘이면 시간만, 아니면 날짜
const formatLastMessageAt = (value) => {
  if (!value) return "대화 없음";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "대화 없음";

  const now = new Date();
  const isToday =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();

  if (isToday) {
    return date.toLocaleTimeString("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  return date.toLocaleDateString("ko-KR", {
    month: "short",
    day: "numeric",
  });
};

const InquiryRoomListModal = ({ sessions, onSelect, onLeave, onClose }) => {
  // 업체별 대표사진 — cmno 하나당 한 번만 조회해서 캐싱
  const [companyImages, setCompanyImages] = useState({});
  const fetchedCmnosRef = useRef(new Set());

  useEffect(() => {
    sessions.forEach((session) => {
      const { cmno } = session;
      if (cmno == null || fetchedCmnosRef.current.has(cmno)) return;
      fetchedCmnosRef.current.add(cmno);

      getOne(cmno)
        .then((company) => {
          setCompanyImages((prev) => ({
            ...prev,
            [cmno]: company?.mainImage
              ? getCompanyImageUrl(company.mainImage, true)
              : null,
          }));
        })
        .catch(() => {
          setCompanyImages((prev) => ({ ...prev, [cmno]: null }));
        });
    });
  }, [sessions]);

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const handleLeave = async (e, roomId, companyName) => {
    e.stopPropagation();
    const confirmed = await showConfirm(
      `${companyName} 문의 채팅을 목록에서 나가시겠습니까?\n대화 내용은 남아있고, 나중에 다시 문의하면 이어서 볼 수 있습니다.`,
    );
    if (confirmed) {
      onLeave(roomId);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-end justify-center bg-black/30 px-4 pb-6 sm:items-center sm:pb-8"
      onClick={handleOverlayClick}
    >
      <div
        className="flex h-[min(480px,80dvh)] w-full max-w-md flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between border-b border-line px-4 py-3">
          <p className="text-sm font-semibold text-ink">문의 채팅</p>
          <button
            type="button"
            className="flex h-8 w-8 items-center justify-center rounded-full text-ink-muted transition hover:bg-surface hover:text-ink"
            onClick={onClose}
            aria-label="목록 닫기"
          >
            ✕
          </button>
        </div>

        {/* 업체별 문의방 목록 */}
        <div className="flex-1 divide-y divide-line overflow-y-auto">
          {sessions.length === 0 ? (
            <p className="px-5 py-10 text-center text-sm text-ink-muted">
              문의한 업체가 없습니다.
            </p>
          ) : (
            sessions.map((session) => (
              <button
                key={session.roomId}
                type="button"
                onClick={() => onSelect(session.roomId)}
                className="flex w-full items-center gap-3 px-5 py-4 text-left transition hover:bg-surface/60"
              >
                {/* 업체 썸네일 */}
                <div className="h-11 w-11 shrink-0 overflow-hidden rounded-full bg-surface">
                  {companyImages[session.cmno] ? (
                    <img
                      src={companyImages[session.cmno]}
                      alt={session.companyName}
                      className="h-full w-full object-cover"
                      onError={() =>
                        setCompanyImages((prev) => ({
                          ...prev,
                          [session.cmno]: null,
                        }))
                      }
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center text-sm text-ink-faint">
                      {session.companyName?.[0] || "🏢"}
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-1.5">
                    {session.unread && (
                      <span
                        className="h-2 w-2 shrink-0 rounded-full bg-red-500"
                        aria-hidden="true"
                      />
                    )}
                    <p className="truncate text-sm font-medium text-ink">
                      {session.companyName}
                    </p>
                  </div>
                  <p className="mt-1 text-xs text-ink-muted">
                    {formatLastMessageAt(session.lastMessageAt)}
                  </p>
                </div>
                <span
                  role="button"
                  tabIndex={0}
                  onClick={(e) =>
                    handleLeave(e, session.roomId, session.companyName)
                  }
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      handleLeave(e, session.roomId, session.companyName);
                    }
                  }}
                  className="shrink-0 rounded-full px-3 py-1.5 text-xs text-ink-muted transition hover:bg-blush-50 hover:text-brand"
                >
                  나가기
                </span>
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default InquiryRoomListModal;
