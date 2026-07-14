import React from "react";

const CompanyChatFloatingButton = ({ companyName, onOpen }) => {
  return (
    <button
      type="button"
      className="pointer-events-auto flex h-14 w-14 items-center justify-center rounded-full bg-brand text-2xl text-white shadow-lg transition hover:scale-105 hover:opacity-90 active:scale-95"
      onClick={onOpen}
      aria-label={`${companyName} 문의 채팅 열기`}
      title={`${companyName} 문의`}
    >
      💬
    </button>
  );
};

export default CompanyChatFloatingButton;
