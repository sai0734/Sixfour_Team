import React, { useEffect, useState } from "react";
import { openInquiryRoom } from "../../api/companyInquiryApi";
import CompanyChatModal from "./CompanyChatModal";
import CompanyChatFloatingButton from "./CompanyChatFloatingButton";

const CompanyInquiryChat = ({ cmno, companyName, openRequest }) => {
  const [roomId, setRoomId] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  const [opening, setOpening] = useState(false);

  // 문의하기 클릭 시 — 방 생성/조회 후 모달 열기
  const startInquiry = async () => {
    if (!cmno || opening) return;
    setOpening(true);
    try {
      const room = await openInquiryRoom(cmno);
      setRoomId(room.roomId);
      setIsModalOpen(true);
      setIsMinimized(false);
    } catch (err) {
      console.error("문의방 열기 실패:", err);
      alert("문의 채팅을 시작할 수 없습니다. 다시 시도해주세요.");
    } finally {
      setOpening(false);
    }
  };

  // 부모에서 openRequest가 바뀔 때마다 문의 시작
  useEffect(() => {
    if (!openRequest) return;
    startInquiry();
  }, [openRequest]);

  // 모달 최소화 — 플로팅 버튼으로 전환
  const handleMinimize = () => {
    setIsModalOpen(false);
    setIsMinimized(true);
  };

  // 플로팅 클릭 — 모달 다시 열기
  const handleOpenFromFloating = () => {
    setIsModalOpen(true);
    setIsMinimized(false);
  };

  // 아직 방이 없으면 아무것도 안 그림
  if (!roomId) {
    return null;
  }

  return (
    <>
      {isModalOpen && (
        <CompanyChatModal
          companyName={companyName}
          roomId={roomId}
          onMinimize={handleMinimize}
        />
      )}
      {!isModalOpen && isMinimized && (
        <CompanyChatFloatingButton
          companyName={companyName}
          onOpen={handleOpenFromFloating}
        />
      )}
    </>
  );
};

export default CompanyInquiryChat;
