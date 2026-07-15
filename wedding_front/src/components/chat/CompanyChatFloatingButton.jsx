import React from "react";

// 업체 1개 전용 아이콘 → "문의 채팅" 진입점 아이콘 1개로 역할 변경 (companyName → label)
const CompanyChatFloatingButton = ({ label = "문의 채팅", unread, onOpen }) => {
  return (
    <button
      type="button"
      className="pointer-events-auto relative flex h-14 w-14 items-center justify-center rounded-full bg-brand text-2xl text-white shadow-lg transition hover:scale-105 hover:opacity-90 active:scale-95"
      onClick={onOpen}
      aria-label={`${label} 열기${unread ? " (읽지 않은 메시지 있음)" : ""}`}
      title={label}
    >
      💬
      {unread && (
        <span
          className="absolute -top-0.5 -right-0.5 h-3.5 w-3.5 rounded-full border-2 border-white bg-red-500"
          aria-hidden="true"
        />
      )}
    </button>
  );
};

export default CompanyChatFloatingButton;
