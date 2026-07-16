import { useCallback, useRef, useState } from "react";
import { useSelector } from "react-redux";
import CompanyChatModal from "./CompanyChatModal";
import CompanyChatFloatingButton from "./CompanyChatFloatingButton";
import AiChatbotFloatingButton from "../aiChatbot/AiChatbotFloatingButton";
import AiChatbotModal from "../aiChatbot/AiChatbotModal";
import InquiryRoomListModal from "./InquiryRoomListModal";
import useInquiryChat from "../../context/InquiryChatContext";
import { sendChatMessage } from "../../api/chatbotApi";

let aiMessageIdCounter = 0;
const nextAiMessageId = () => {
  aiMessageIdCounter += 1;
  return aiMessageIdCounter;
};

// 백엔드도 AI 대화 문맥을 최근 20턴까지만 기억하니(HISTORY_LIMIT), 화면 이력도 그 이상 오래된 건
// 의미가 없어서 같이 정리한다 - 최근 20개 메시지(user+assistant 합산)만 유지
const MAX_AI_MESSAGES = 20;

const GlobalInquiryChatHost = () => {
  const email = useSelector((state) => state.loginSlice?.email);
  const roleNames = useSelector((state) => state.loginSlice?.roleNames);
  const {
    sessions,
    activeRoomId,
    draftInquiry,
    isListOpen,
    openInquiry,
    minimizeInquiry,
    closeInquiry,
    openList,
    closeList,
    startNewRoomWithMessage,
  } = useInquiryChat();

  const [isAiChatOpen, setIsAiChatOpen] = useState(false);
  const [aiMessages, setAiMessages] = useState([]);
  const [aiSending, setAiSending] = useState(false);
  // state는 반영되기까지 리렌더 왕복이 걸려서 그 틈에 중복 전송이 들어올 수 있음 -
  // ref는 즉시(동기) 갱신되므로 이걸로 중복 전송을 막는다
  const isSendingRef = useRef(false);

  // 매니저 전용 계정은 ROLE_USER가 없어서 챗봇 API가 403이라 버튼 자체를 숨김
  const canUseAiChat = roleNames?.some((roleName) =>
    ["USER", "ROLE_USER"].includes(roleName),
  );

  // 메시지를 추가하면서 MAX_AI_MESSAGES를 넘으면 오래된 것부터 잘라낸다
  const appendAiMessage = useCallback((message) => {
    setAiMessages((prev) => {
      const next = [...prev, message];
      return next.length > MAX_AI_MESSAGES
        ? next.slice(next.length - MAX_AI_MESSAGES)
        : next;
    });
  }, []);

  const handleSendAiMessage = useCallback(
    async (text) => {
      if (isSendingRef.current) return;
      isSendingRef.current = true;
      appendAiMessage({ id: nextAiMessageId(), role: "user", content: text });
      setAiSending(true);
      try {
        const { answer, references } = await sendChatMessage(text);
        appendAiMessage({
          id: nextAiMessageId(),
          role: "assistant",
          content: answer,
          references,
        });
      } catch (err) {
        console.error("AI 챗봇 응답 실패:", err);
        appendAiMessage({
          id: nextAiMessageId(),
          role: "assistant",
          content: "답변을 가져오지 못했어요. 잠시 후 다시 시도해주세요.",
        });
      } finally {
        setAiSending(false);
        isSendingRef.current = false;
      }
    },
    [appendAiMessage],
  );

  if (!email) {
    return null;
  }

  const activeSession = sessions.find(
    (session) => session.roomId === activeRoomId,
  );
  const hasUnread = sessions.some((session) => session.unread);

  // 문의한 업체가 1곳이면 바로 열고, 여러 곳이면 목록을 보여줌
  const handleIconClick = () => {
    if (sessions.length === 1) {
      openInquiry(sessions[0].roomId);
      return;
    }
    openList();
  };

  return (
    <>
      {/* 기존 방이 열린 경우뿐 아니라, 아직 메시지 안 보낸 draft 상태에서도 채팅창을 띄운다 */}
      {(activeSession || draftInquiry) && (
        <CompanyChatModal
          companyName={
            activeSession ? activeSession.companyName : draftInquiry.companyName
          }
          cmno={activeSession ? activeSession.cmno : draftInquiry.cmno}
          roomId={activeSession ? activeSession.roomId : null}
          onMinimize={minimizeInquiry}
          onLeave={
            activeSession ? () => closeInquiry(activeSession.roomId) : undefined
          }
          onFirstSend={
            !activeSession
              ? (text) =>
                  startNewRoomWithMessage(
                    draftInquiry.cmno,
                    draftInquiry.companyName,
                    text,
                  )
              : undefined
          }
        />
      )}

      {isListOpen && (
        <InquiryRoomListModal
          sessions={sessions}
          onSelect={openInquiry}
          onLeave={closeInquiry}
          onClose={closeList}
        />
      )}

      {isAiChatOpen && (
        <AiChatbotModal
          messages={aiMessages}
          onSend={handleSendAiMessage}
          sending={aiSending}
          onClose={() => setIsAiChatOpen(false)}
        />
      )}

      <div
        className="fixed bottom-6 right-6 z-[9998] flex flex-col-reverse items-end gap-3 pointer-events-none"
        aria-label="플로팅 채팅"
      >
        {canUseAiChat && !isAiChatOpen && (
          <AiChatbotFloatingButton onOpen={() => setIsAiChatOpen(true)} />
        )}
        {!activeSession &&
          !draftInquiry &&
          !isListOpen &&
          sessions.length > 0 && (
            <CompanyChatFloatingButton
              label="문의 채팅"
              unread={hasUnread}
              onOpen={handleIconClick}
            />
          )}
      </div>
    </>
  );
};

export default GlobalInquiryChatHost;
