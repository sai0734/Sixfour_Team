import React, { useCallback, useEffect, useRef, useState } from "react";
import { useSelector } from "react-redux";
import {
  getInquiryMessages,
  markInquiryRead,
  sendInquiryMessage,
} from "../../api/companyInquiryApi";
import { getOne, getCompanyImageUrl } from "../../api/companyApi";
import { subscribeInquiryTopic } from "../../util/inquiryWsClient";

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
  cmno,
  roomId,
  onMinimize,
  onLeave,
  onFirstSend,
  subtitle = "업체 문의",
}) => {
  const email = useSelector((state) => state.loginSlice?.email);

  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  // 지금 누구랑 대화 중인지 한눈에 보이게 헤더에 띄울 업체 대표사진 (cmno가 있을 때만 - 매니저 화면은 상대가
  // 회원이라 cmno를 안 넘기므로 자동으로 사진 없이 이메일만 표시됨)
  const [companyImage, setCompanyImage] = useState(null);

  const messageEndRef = useRef(null);
  const messageListRef = useRef(null);
  const onMinimizeRef = useRef(onMinimize);
  // 방을 처음 열었을 때(과거 이력 로드)는 애니메이션 없이 바로 맨 아래로,
  // 그 이후 실시간으로 새 메시지가 올 때만 부드럽게 스크롤하기 위한 플래그
  const hasScrolledRef = useRef(false);

  useEffect(() => {
    onMinimizeRef.current = onMinimize;
  }, [onMinimize]);

  // 헤더에 띄울 업체 대표사진 조회
  useEffect(() => {
    if (!cmno) {
      setCompanyImage(null);
      return;
    }

    let cancelled = false;
    getOne(cmno)
      .then((company) => {
        if (cancelled) return;
        setCompanyImage(
          company?.mainImage ? getCompanyImageUrl(company.mainImage, true) : null,
        );
      })
      .catch(() => {
        if (!cancelled) setCompanyImage(null);
      });

    return () => {
      cancelled = true;
    };
  }, [cmno]);

  // 메시지 목록 조회 (최초 진입 시 과거 대화 이력을 불러오는 용도 - 실시간 수신은 WS가 담당)
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

  // 모달 열릴 때 과거 이력을 한 번 불러오고, 이후 새 메시지는 WebSocket 구독으로 실시간 수신.
  // roomId가 아직 없는 draft 상태(방 생성 전)면 불러올 게 없으니 로딩만 끄고 대기
  useEffect(() => {
    hasScrolledRef.current = false;

    if (!roomId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    loadMessages();

    const unsubscribe = subscribeInquiryTopic(
      `/topic/inquiries/room/${roomId}`,
      (event) => {
        if (event.type === "MESSAGE") {
          const incomingMessage = event.message;
          setMessages((prev) => {
            // 같은 메시지가 중복으로 들어오는 경우 방지 (재연결 등으로 인한 안전장치)
            if (prev.some((m) => m.messageId === incomingMessage.messageId)) {
              return prev;
            }
            return [...prev, incomingMessage];
          });

          // 상대가 보낸 메시지가 창을 열어둔 채로 실시간으로 왔으므로 곧바로 읽음 처리한다.
          // (안 하면 창을 닫을 때 방금 다 읽은 방인데도 안읽음 뱃지가 떠버림)
          if (incomingMessage.senderEmail !== email) {
            markInquiryRead(roomId).catch((err) => {
              console.error("읽음 처리 실패:", err);
            });
          }
        } else if (event.type === "READ") {
          // 상대가 내 메시지를 읽었다는 신호 - 내가 보낸 메시지들의 읽음 표시를 갱신
          if (event.readerEmail !== email) {
            setMessages((prev) =>
              prev.map((m) =>
                m.senderEmail === email && !m.readByRecipient
                  ? { ...m, readByRecipient: true }
                  : m,
              ),
            );
          }
        }
      },
    );

    return unsubscribe;
  }, [roomId, loadMessages, email]);

  // 메시지 목록이 바뀔 때 맨 아래로 스크롤 — 방을 막 연 최초 1회는 즉시 이동(auto),
  // 그 이후 실시간으로 도착하는 새 메시지는 부드럽게(smooth) 스크롤한다
  useEffect(() => {
    if (messages.length === 0) return;

    const behavior = hasScrolledRef.current ? "smooth" : "auto";
    messageEndRef.current?.scrollIntoView({ behavior });
    hasScrolledRef.current = true;
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
  // roomId가 없으면(draft) 첫 메시지 전송 시점에 방 생성부터 해야 하므로 onFirstSend로 위임
  const handleSend = async () => {
    const text = input.trim();
    if (!text || sending) return;
    setSending(true);
    try {
      if (!roomId) {
        await onFirstSend(text);
        setInput("");
      } else {
        // 전송 성공하면 서버가 같은 방 토픽으로 브로드캐스트해줘서 이 화면에도 자동 반영되므로,
        // 예전처럼 loadMessages()를 다시 부를 필요가 없어짐
        await sendInquiryMessage(roomId, text);
        setInput("");
      }
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

  // 채팅방 나가기 — 목록에서만 숨김 (서버 대화 기록은 유지, 상대가 새 메시지를 보내면 다시 표시됨)
  const handleLeaveClick = () => {
    const confirmed = window.confirm(
      `${companyName} 문의 채팅을 목록에서 나가시겠습니까?\n대화 내용은 서버에 남아있고, 새 메시지가 오면 다시 표시됩니다.`,
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
        {/* 헤더 — 지금 누구와 대화 중인지 사진+이름으로 한눈에 보이게 */}
        <div className="flex items-center justify-between border-b border-line px-4 py-3">
          <div className="flex min-w-0 items-center gap-2.5">
            {cmno && (
              <div className="h-9 w-9 shrink-0 overflow-hidden rounded-full bg-surface">
                {companyImage ? (
                  <img
                    src={companyImage}
                    alt={companyName}
                    className="h-full w-full object-cover"
                    onError={() => setCompanyImage(null)}
                  />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-sm text-ink-faint">
                    {companyName?.[0] || "🏢"}
                  </div>
                )}
              </div>
            )}
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink">{companyName}</p>
              <p className="text-xs text-ink-muted">{subtitle}</p>
            </div>
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
                        className={`mt-1 flex items-center gap-1 text-[10px] ${
                          isMine(message.senderEmail)
                            ? "justify-end text-white/70"
                            : "text-ink-muted"
                        }`}
                      >
                        {isMine(message.senderEmail) && (
                          <span>{message.readByRecipient ? "읽음" : "안읽음"}</span>
                        )}
                        <span>
                          {message.regDate
                            ? new Date(message.regDate).toLocaleTimeString(
                                "ko-KR",
                                {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                },
                              )
                            : ""}
                        </span>
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
