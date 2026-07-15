import React, { useCallback, useEffect, useRef, useState } from "react";
import { useSelector } from "react-redux";
import {
  getInquiryMessages,
  sendInquiryMessage,
} from "../../api/companyInquiryApi";

const POLL_INTERVAL_MS = 3000;

// 날짜 구분선 표시용 — 메시지 목록에서 날짜가 바뀔 때 사이에 넣는다
const formatDateSeparator = (regDate) => {
  if (!regDate) return "";
  const date = new Date(regDate);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
  });
};

// 두 시각이 같은 날짜인지 비교
const isSameDate = (a, b) => {
  if (!a || !b) return false;
  const dateA = new Date(a);
  const dateB = new Date(b);
  return (
    dateA.getFullYear() === dateB.getFullYear() &&
    dateA.getMonth() === dateB.getMonth() &&
    dateA.getDate() === dateB.getDate()
  );
};

const CompanyChatModal = ({
  companyName,
  roomId,
  onMinimize,
  onLeave,
  subtitle = "업체 문의",
}) => {
  const email = useSelector((state) => state.loginSlice?.email);

  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  const messageEndRef = useRef(null);
  const messageListRef = useRef(null);
  const onMinimizeRef = useRef(onMinimize);

  useEffect(() => {
    onMinimizeRef.current = onMinimize;
  }, [onMinimize]);

  // 메시지 목록 조회
  const loadMessages = useCallback(async () => {
    if (!roomId) return;

    try {
      const data = await getInquiryMessages(roomId);
      setMessages(data);
    } catch (err) {
      console.error("메시지 목록 조회 실패:", err);
    } finally {
      setLoading(false);
    }
  }, [roomId]);

  // 모달 열릴 때 메시지 로드 + 폴링 시작 - 탭이 백그라운드면 폴링 요청은 건너뛰고,
  // 다시 포그라운드로 돌아오면 즉시 한 번 갱신
  useEffect(() => {
    if (!roomId) return;

    setLoading(true);
    loadMessages();

    const timer = setInterval(() => {
      if (document.hidden) return;
      loadMessages();
    }, POLL_INTERVAL_MS);

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        loadMessages();
      }
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [roomId, loadMessages]);

  // 새 메시지 오면 맨 아래로 스크롤
  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // ESC 키 + 배경 스크롤 잠금
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        onMinimizeRef.current();
      }
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [roomId]);

  // 메시지 전송
  const handleSend = async () => {
    const text = input.trim();
    if (!text || sending) return;
    setSending(true);
    try {
      await sendInquiryMessage(roomId, text);
      setInput("");
      await loadMessages();
    } catch (err) {
      console.error("메시지 전송 실패:", err);
      alert("메시지 전송에 실패했습니다.");
    } finally {
      setSending(false);
    }
  };

  // Enter 키로 전송
  const handleKeyDownInput = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // 오버레이 클릭 시 최소화 (플로팅으로)
  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onMinimize();
    }
  };

  // 채팅방 나가기 — 목록에서 완전히 제거 (서버 대화 기록은 유지)
  const handleLeaveClick = () => {
    const confirmed = window.confirm(
      `${companyName} 문의 채팅을 나가시겠습니까?\n대화 내용은 남아있고, 나중에 다시 문의하면 이어서 볼 수 있습니다.`,
    );
    if (confirmed) {
      onLeave();
    }
  };

  // 내 메시지인지 판별 (말풍선 좌/우 구분)
  const isMine = (senderEmail) => senderEmail === email;

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-end justify-center bg-black/30 px-4 pb-6 sm:items-center sm:pb-8"
      onClick={handleOverlayClick}
    >
      <div
        className="flex h-[min(520px,80dvh)] w-full max-w-md flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between border-b border-line px-4 py-3">
          <div>
            <p className="text-sm font-semibold text-ink">{companyName}</p>
            <p className="text-xs text-ink-muted">{subtitle}</p>
          </div>
          <div className="flex items-center gap-1">
            {onLeave && (
              <button
                type="button"
                className="rounded-full px-3 py-1.5 text-xs text-ink-muted transition hover:bg-blush-50 hover:text-brand"
                onClick={handleLeaveClick}
              >
                나가기
              </button>
            )}
            <button
              type="button"
              className="flex h-8 w-8 items-center justify-center rounded-full text-ink-muted transition hover:bg-surface hover:text-ink"
              onClick={onMinimize}
              aria-label="채팅 최소화"
            >
              ✕
            </button>
          </div>
        </div>
        {/* 메시지 목록 */}
        <div
          ref={messageListRef}
          className="flex-1 space-y-3 overflow-y-auto bg-surface/40 px-4 py-4"
        >
          {loading ? (
            <p className="text-center text-sm text-ink-muted">불러오는 중...</p>
          ) : messages.length === 0 ? (
            <p className="text-center text-sm text-ink-muted">
              문의 내용을 남겨주세요.
            </p>
          ) : (
            messages.map((message, index) => {
              const previousMessage = messages[index - 1];
              const showDateSeparator =
                !previousMessage ||
                !isSameDate(previousMessage.regDate, message.regDate);

              return (
                <React.Fragment key={message.messageId}>
                  {showDateSeparator && (
                    <div className="flex justify-center py-1">
                      <span className="rounded-full bg-surface px-3 py-1 text-[11px] text-ink-muted">
                        {formatDateSeparator(message.regDate)}
                      </span>
                    </div>
                  )}
                  <div
                    className={`flex ${isMine(message.senderEmail) ? "justify-end" : "justify-start"}`}
                  >
                    <div
                      className={`max-w-[75%] rounded-2xl px-3.5 py-2 text-sm leading-relaxed ${
                        isMine(message.senderEmail)
                          ? "rounded-br-md bg-brand text-white"
                          : "rounded-bl-md border border-line bg-white text-ink"
                      }`}
                    >
                      <p className="whitespace-pre-wrap break-words">
                        {message.content}
                      </p>
                      <p
                        className={`mt-1 text-[10px] ${
                          isMine(message.senderEmail)
                            ? "text-white/70"
                            : "text-ink-muted"
                        }`}
                      >
                        {message.regDate
                          ? new Date(message.regDate).toLocaleTimeString(
                              "ko-KR",
                              {
                                hour: "2-digit",
                                minute: "2-digit",
                              },
                            )
                          : ""}
                      </p>
                    </div>
                  </div>
                </React.Fragment>
              );
            })
          )}
          <div ref={messageEndRef} />
        </div>
        {/* 입력창 */}
        <div className="flex gap-2 border-t border-line px-4 py-3">
          <input
            type="text"
            className="flex-1 rounded-full border border-line px-4 py-2.5 text-sm outline-none transition focus:border-brand"
            placeholder="메시지를 입력하세요"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDownInput}
            disabled={sending}
          />
          <button
            type="button"
            className="shrink-0 rounded-full bg-brand px-4 py-2.5 text-sm font-medium text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            onClick={handleSend}
            disabled={sending || !input.trim()}
          >
            전송
          </button>
        </div>
      </div>
    </div>
  );
};

export default CompanyChatModal;
