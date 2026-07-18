import { useCallback, useEffect, useRef, useState } from "react";
import { useSelector } from "react-redux";
import CompanyChatModal from "./CompanyChatModal";
import CompanyChatFloatingButton from "./CompanyChatFloatingButton";
import AiChatbotFloatingButton from "../aiChatbot/AiChatbotFloatingButton";
import AiChatbotModal from "../aiChatbot/AiChatbotModal";
import {
  getCompanyCategoryGuideMessage,
  getIntentGuideMessage,
} from "../aiChatbot/aiChatbotIntents";
import InquiryRoomListModal from "./InquiryRoomListModal";
import useInquiryChat from "../../context/InquiryChatContext";
import { sendChatMessage, sendDressPhoto } from "../../api/chatbotApi";

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
  // "MENU" = 버튼 메뉴 노출 중, null = 자유롭게 질문하기 선택됨, 그 외 문자열 = 버튼으로 고정된 intent
  const [selectedIntent, setSelectedIntent] = useState("MENU");
  // "업체 찾기" 안에서 카테고리 버튼으로 고정한 값 - "CATEGORY_MENU"면 아직 선택 전
  const [selectedCompanyCategory, setSelectedCompanyCategory] =
    useState("CATEGORY_MENU");
  // state는 반영되기까지 리렌더 왕복이 걸려서 그 틈에 중복 전송이 들어올 수 있음 -
  // ref는 즉시(동기) 갱신되므로 이걸로 중복 전송을 막는다
  const isSendingRef = useRef(false);
  const prevEmailRef = useRef(email);

  // 로그아웃하거나 다른 계정으로 전환되면(email이 바뀌면), 이전 계정의 AI 챗봇 이력이
  // 화면에 남아있지 않도록 초기화한다
  useEffect(() => {
    if (prevEmailRef.current !== email) {
      setAiMessages([]);
      setSelectedIntent("MENU");
      setSelectedCompanyCategory("CATEGORY_MENU");
      setIsAiChatOpen(false);
    }
    prevEmailRef.current = email;
  }, [email]);

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
        // "MENU" 상태에서 사용자가 그냥 타이핑해버린 경우까지 대비해, 버튼을 안 골랐으면 자유질문(null)으로 보낸다
        const apiIntent = selectedIntent === "MENU" ? null : selectedIntent;
        // "업체 찾기" 중 카테고리 버튼을 아직 안 골랐으면(CATEGORY_MENU) 카테고리 없이 보낸다
        const companyCategory =
          selectedIntent === "SEARCH_COMPANIES" &&
          selectedCompanyCategory !== "CATEGORY_MENU"
            ? selectedCompanyCategory
            : null;
        const { answer, references } = await sendChatMessage(
          text,
          apiIntent,
          companyCategory,
        );
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
    [appendAiMessage, selectedIntent, selectedCompanyCategory],
  );

  // 버튼 메뉴 선택 - intent를 바꾸는 동시에, 서버 호출 없이 그 intent에 맞는 안내 문구를 바로 채팅창에 보여준다
  const handleSelectIntent = useCallback(
    (intentKey) => {
      setSelectedIntent(intentKey);
      // "업체 찾기"를 다시 선택하면 카테고리도 처음부터 다시 고르게 한다
      setSelectedCompanyCategory("CATEGORY_MENU");
      appendAiMessage({
        id: nextAiMessageId(),
        role: "assistant",
        content: getIntentGuideMessage(intentKey),
      });
    },
    [appendAiMessage],
  );

  // "업체 찾기" 안에서 카테고리 버튼 선택 - 그 세션 내내 이 카테고리로 고정된다
  const handleSelectCompanyCategory = useCallback(
    (categoryKey) => {
      setSelectedCompanyCategory(categoryKey);
      appendAiMessage({
        id: nextAiMessageId(),
        role: "assistant",
        content: getCompanyCategoryGuideMessage(categoryKey),
      });
    },
    [appendAiMessage],
  );

  // 드레스 사진 전송 - 기존 텍스트 대화와 별개 흐름(사진 → 스타일 분석 → 유사 아이템 추천)
  const handleSendAiPhoto = useCallback(
    async (file) => {
      if (isSendingRef.current) return;
      isSendingRef.current = true;
      // 서버 응답을 기다리지 않고, 로컬 파일 URL로 바로 화면에 사진을 보여준다
      appendAiMessage({
        id: nextAiMessageId(),
        role: "user",
        imageUrl: URL.createObjectURL(file),
      });
      setAiSending(true);
      try {
        const { answer, references } = await sendDressPhoto(file);
        appendAiMessage({
          id: nextAiMessageId(),
          role: "assistant",
          content: answer,
          references,
        });
      } catch (err) {
        console.error("AI 드레스 사진 추천 실패:", err);
        appendAiMessage({
          id: nextAiMessageId(),
          role: "assistant",
          content: "사진을 분석하지 못했어요. 잠시 후 다시 시도해주세요.",
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
          onSendPhoto={handleSendAiPhoto}
          sending={aiSending}
          onClose={() => {
            setIsAiChatOpen(false);
            // 닫았다 다시 열면 항상 버튼 메뉴부터 다시 보여준다 (하던 대화 모드를 이어가지 않음)
            setSelectedIntent("MENU");
            setSelectedCompanyCategory("CATEGORY_MENU");
          }}
          selectedIntent={selectedIntent}
          onSelectIntent={handleSelectIntent}
          onResetIntent={() => {
            setSelectedIntent("MENU");
            setSelectedCompanyCategory("CATEGORY_MENU");
          }}
          selectedCompanyCategory={selectedCompanyCategory}
          onSelectCompanyCategory={handleSelectCompanyCategory}
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
