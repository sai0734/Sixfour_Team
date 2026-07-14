import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  getInquiryMessages,
  sendInquiryMessage,
} from "../../api/companyInquiryApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const POLL_INTERVAL_MS = 3000;

const CompanyChatModal = ({ companyName, roomId, onMinimize }) => {
  const { loginState } = useCustomLogin();

  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  const messageEndRef = useRef(null);
  const messageListRef = useRef(null);

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

  // 모달 열릴 때 메시지 로드 + 폴링 시작
  useEffect(() => {
    if (!roomId) return;

    setLoading(true);
    loadMessages();

    const timer = setInterval(() => {
      loadMessages();
    }, POLL_INTERVAL_MS);

    return () => clearInterval(timer);
  }, [roomId, loadMessages]);

  // 새 메시지 오면 맨 아래로 스크롤
  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messges]);

  // ESC 키 + 배경 스크롤 잠금
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        onMinimize();
      }
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onMinimize]);

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

  // Esnter 키로 전송
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

  // 내 메시지인지 판별 (말풍선 좌/우 구분)
  const isMine = (senderEmail) => senderEmail === loginState.email;

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
            <p className="text-xs text-ink-muted">업체 문의</p>
          </div>
          <button
            type="button"
            className="flex h-8 w-8 items-center justify-center rounded-full text-ink-muted transition hover:bg-surface hover:text-ink"
            onClick={onMinimize}
            aria-label="채팅 최소화"
          >
            ✕
          </button>
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
            messages.map((message) => (
              <div
                key={message.messageId}
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
                      ? new Date(message.regDate).toLocaleTimeString("ko-KR", {
                          hour: "2-digit",
                          minute: "2-digit",
                        })
                      : ""}
                  </p>
                </div>
              </div>
            ))
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
