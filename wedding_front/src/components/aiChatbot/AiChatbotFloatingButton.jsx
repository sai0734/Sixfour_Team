import React from "react";

const AiChatbotFloatingButton = ({ onOpen }) => {
  return (
    <button
      type="button"
      className="pointer-events-auto flex h-14 w-14 items-center justify-center rounded-full border border-line bg-white text-2xl text-brand shadow-lg transition hover:scale-105 hover:opacity-90 active:scale-95"
      onClick={onOpen}
      aria-label="AI 웨딩 상담"
      title="AI 웨딩 상담"
    >
      🤖
    </button>
  );
};

export default AiChatbotFloatingButton;
