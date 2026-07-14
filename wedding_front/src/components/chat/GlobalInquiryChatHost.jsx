import CompanyChatModal from "./CompanyChatModal";
import CompanyChatFloatingButton from "./CompanyChatFloatingButton";
import AiChatbotFloatingButton from "./AiChatbotFloatingButton";
import useInquiryChat from "../../context/InquiryChatContext";
import useCustomLogin from "../../hooks/useCustomLogin";

const GlobalInquiryChatHost = () => {
  const { loginState } = useCustomLogin();
  const { sessions, openInquiry, minimizeInquiry } = useInquiryChat();

  if (!loginState.email) {
    return null;
  }

  const minimizedSessions = sessions.filter(
    (session) => session.view === "minimized",
  );
  const openSessions = sessions.filter((session) => session.view === "open");

  return (
    <>
      {openSessions.map((session) => (
        <CompanyChatModal
          key={session.roomId}
          companyName={session.companyName}
          roomId={session.roomId}
          onMinimize={() => minimizeInquiry(session.roomId)}
        />
      ))}

      <div
        className="fixed bottom-6 right-6 z-[9998] flex flex-col-reverse items-end gap-3 pointer-events-none"
        aria-label="플로팅 채팅"
      >
        <AiChatbotFloatingButton />
        {minimizedSessions.map((session) => (
          <CompanyChatFloatingButton
            key={session.roomId}
            companyName={session.companyName}
            onOpen={() => openInquiry(session.roomId)}
          />
        ))}
      </div>
    </>
  );
};

export default GlobalInquiryChatHost;
