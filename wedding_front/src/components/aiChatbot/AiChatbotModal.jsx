import React, { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { getCompanyImageUrl } from "../../api/companyApi";

// 브라우저 지원 여부는 모듈 로드 시 한 번만 확인 (크롬 계열만 지원, 나머지는 마이크 버튼 자체를 숨김)
const SpeechRecognitionClass =
  typeof window !== "undefined"
    ? window.SpeechRecognition || window.webkitSpeechRecognition
    : null;

// AI 답변에 딸려온 카드(업체/드레스아이템/답례품 등) - 누르면 해당 상세페이지로 이동
const ReferenceCards = ({ references }) => {
  if (!references || references.length === 0) return null;

  return (
    <div className="mt-2 flex max-w-[90%] gap-2 overflow-x-auto pb-1">
      {references.map((ref) => (
        <Link
          key={`${ref.type}-${ref.id}`}
          to={ref.link}
          className="flex w-28 shrink-0 flex-col overflow-hidden rounded-xl border border-line bg-white shadow-sm transition hover:shadow-md"
        >
          <div className="h-20 w-full bg-surface">
            {ref.imageUrl ? (
              <img
                src={getCompanyImageUrl(ref.imageUrl)}
                alt={ref.name}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-2xl text-ink-faint">
                🏢
              </div>
            )}
          </div>
          <div className="px-2 py-1.5">
            <p className="truncate text-xs font-medium text-ink">{ref.name}</p>
            <p className="truncate text-[10px] text-ink-muted">
              {ref.priceLabel}
            </p>
          </div>
        </Link>
      ))}
    </div>
  );
};

const AiChatbotModal = ({ messages, onSend, sending, onClose }) => {
  const [input, setInput] = useState("");
  const [isListening, setIsListening] = useState(false);
  const messageEndRef = useRef(null);
  const recognitionRef = useRef(null);
  const hasScrolledOnceRef = useRef(false);

  // 모달을 열 때는 이력이 길어도 애니메이션 없이 즉시 맨 아래로 이동시키고,
  // 이미 열려있는 상태에서 새 메시지가 올 때만 부드럽게 스크롤
  useEffect(() => {
    if (!hasScrolledOnceRef.current) {
      messageEndRef.current?.scrollIntoView({ behavior: "auto" });
      hasScrolledOnceRef.current = true;
      return;
    }
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

  // 모달이 닫힐 때 녹음 중이면 같이 정리
  useEffect(() => {
    return () => {
      recognitionRef.current?.stop();
    };
  }, []);

  // 마이크 토글 - 인식 끝나면 입력창에 텍스트만 채워넣고 전송은 사용자가 직접 누르게 함
  const toggleListening = () => {
    if (!SpeechRecognitionClass) return;

    if (isListening) {
      recognitionRef.current?.stop();
      return;
    }

    const recognition = new SpeechRecognitionClass();
    recognition.lang = "ko-KR";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onresult = (event) => {
      const transcript = event.results[0]?.[0]?.transcript ?? "";
      if (transcript) {
        setInput((prev) =>
          prev.trim() ? `${prev.trim()} ${transcript}` : transcript,
        );
      }
    };

    recognition.onerror = () => {
      setIsListening(false);
    };

    recognition.onend = () => {
      setIsListening(false);
    };

    recognitionRef.current = recognition;
    recognition.start();
    setIsListening(true);
  };

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
                className={`flex flex-col ${message.role === "user" ? "items-end" : "items-start"}`}
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
                {message.role === "assistant" && (
                  <ReferenceCards references={message.references} />
                )}
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
          {SpeechRecognitionClass && (
            <button
              type="button"
              className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full border transition ${
                isListening
                  ? "animate-pulse border-red-400 bg-red-50 text-red-500"
                  : "border-line text-ink-muted hover:bg-surface hover:text-ink"
              }`}
              onClick={toggleListening}
              disabled={sending}
              aria-label={isListening ? "음성 인식 중지" : "음성으로 입력"}
              title={isListening ? "음성 인식 중지" : "음성으로 입력"}
            >
              🎤
            </button>
          )}
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
