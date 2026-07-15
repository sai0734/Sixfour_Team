import React, { useEffect, useRef, useState } from "react";

const AiChatbotModal = ({ messages, onSend, sending, onClose }) => {
  const [input, setInput] = useState("");
  const messageEndRef = useRef(null);

  // 새 메시지 오면 맨 아래로 스크롤
  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, sending]);

  // ESC 키 + 배경 스크롤 잠금
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  const handleSend = () => {
    const text = input.trim();
    if (!text || sending) return;
    setInput("");
    onSend(text);
  };

  const handleKeyDownInput = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

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
            <p className="text-sm font-semibold text-ink">AI 웨딩 상담</p>
            <p className="text-xs text-ink-muted">
              결혼 준비 관련 질문에 답해드려요
            </p>
          </div>
          <button
            type="button"
            className="flex h-8 w-8 items-center justify-center rounded-full text-ink-muted transition hover:bg-surface hover:text-ink"
            onClick={onClose}
            aria-label="채팅 닫기"
          >
            ✕
          </button>
        </div>

        {/* 메시지 목록 */}
        <div className="flex-1 space-y-3 overflow-y-auto bg-surface/40 px-4 py-4">
          {messages.length === 0 ? (
            <p className="text-center text-sm text-ink-muted">
              드레스나 웨딩홀 등 궁금한 걸 물어보세요.
            </p>
          ) : (
            messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`max-w-[75%] whitespace-pre-wrap break-words rounded-2xl px-3.5 py-2 text-sm leading-relaxed ${
                    message.role === "user"
                      ? "rounded-br-md bg-brand text-white"
                      : "rounded-bl-md border border-line bg-white text-ink"
                  }`}
                >
                  {message.content}
                </div>
              </div>
            ))
          )}
          {sending && (
            <div className="flex justify-start">
              <div className="max-w-[75%] rounded-2xl rounded-bl-md border border-line bg-white px-3.5 py-2 text-sm text-ink-muted">
                답변 작성 중...
              </div>
            </div>
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

export default AiChatbotModal;
