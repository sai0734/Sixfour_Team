import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getCompanyInquiryRooms } from "../../api/companyInquiryApi";
import CompanyChatModal from "../chat/CompanyChatModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import useManagedCompany from "../../hooks/useManagedCompany";

const ROOM_POLL_INTERVAL_MS = 5000;

// 날짜 포맷 — 목록에서 마지막 대화 시각 표시용
const formatLastMessageAt = (value) => {
  if (!value) return "대화 없음";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "대화 없음";

  const now = new Date();
  const isToday =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();

  if (isToday) {
    return date.toLocaleTimeString("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  return date.toLocaleDateString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const ManagerInquiryInbox = () => {
  const navigate = useNavigate();
  const { loginState } = useCustomLogin();
  const { isManager, company, loading } = useManagedCompany({
    enabled: Boolean(loginState.email),
  });

  const [rooms, setRooms] = useState([]);
  const [roomsLoading, setRoomsLoading] = useState(false);
  const [selectedRoom, setSelectedRoom] = useState(null);

  // 담당 업체의 문의방 목록 조회 + 폴링
  const loadRooms = useCallback(async () => {
    if (!company?.cmno) return;

    setRoomsLoading(true);
    try {
      const data = await getCompanyInquiryRooms(company.cmno);
      setRooms(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("문의방 목록 조회 실패:", err);
    } finally {
      setRoomsLoading(false);
    }
  }, [company?.cmno]);

  useEffect(() => {
    if (!company?.cmno) return;

    loadRooms();
    const timer = setInterval(loadRooms, ROOM_POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [company?.cmno, loadRooms]);

  if (!loginState.email) {
    return (
      <div className="rounded-2xl border border-line bg-white p-8 text-center">
        <p className="text-sm text-ink-muted">
          로그인 후 업체페이지를 이용할 수 있습니다.
        </p>
        <button
          type="button"
          className="mt-4 rounded-full border border-brand px-5 py-2 text-sm text-brand transition hover:bg-blush-50"
          onClick={() => navigate("/auth/login")}
        >
          로그인하기
        </button>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-2xl border border-line bg-white p-8 text-center text-sm text-ink-muted">
        업체페이지를 불러오는 중...
      </div>
    );
  }

  if (!isManager || !company?.cmno) {
    return (
      <div className="rounded-2xl border border-line bg-white p-8 text-center">
        <p className="text-sm text-ink-muted">
          담당 업체로 지정된 계정만 업체페이지를 이용할 수 있습니다.
        </p>
        <button
          type="button"
          className="mt-4 rounded-full border border-line px-5 py-2 text-sm text-ink-soft transition hover:border-brand hover:text-brand"
          onClick={() => navigate("/mypage")}
        >
          마이페이지로
        </button>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-line bg-white overflow-hidden">
      {/* 헤더 */}
      <div className="border-b border-line px-5 py-4 sm:px-6">
        <p className="text-xs font-medium uppercase tracking-wide text-ink-faint">
          담당 업체
        </p>
        <h2 className="mt-1 font-['Gowun_Batang'] text-xl text-ink">
          {company.name}
        </h2>
        <p className="mt-1 text-xs text-ink-muted">
          회원 문의를 확인하고 답변할 수 있습니다.
        </p>
      </div>

      {/* 방 목록 */}
      <div className="divide-y divide-line">
        {roomsLoading && rooms.length === 0 ? (
          <p className="px-5 py-10 text-center text-sm text-ink-muted sm:px-6">
            문의 목록을 불러오는 중...
          </p>
        ) : rooms.length === 0 ? (
          <p className="px-5 py-10 text-center text-sm text-ink-muted sm:px-6">
            아직 들어온 문의가 없습니다.
          </p>
        ) : (
          rooms.map((room) => {
            const isSelected = selectedRoom?.roomId === room.roomId;
            return (
              <button
                key={room.roomId}
                type="button"
                onClick={() =>
                  setSelectedRoom({
                    roomId: room.roomId,
                    memberEmail: room.memberEmail,
                  })
                }
                className={`flex w-full items-center justify-between gap-4 px-5 py-4 text-left transition sm:px-6 ${
                  isSelected ? "bg-blush-50" : "hover:bg-surface/60"
                }`}
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-ink">
                    {room.memberEmail || "알 수 없는 회원"}
                  </p>
                  <p className="mt-1 text-xs text-ink-muted">
                    {room.status === "CLOSED" ? "종료됨" : "진행 중"}
                  </p>
                </div>
                <div className="shrink-0 text-right">
                  <p className="text-xs text-ink-faint">
                    {formatLastMessageAt(room.lastMessageAt)}
                  </p>
                  <p className="mt-1 text-xs text-brand">답변하기 →</p>
                </div>
              </button>
            );
          })
        )}
      </div>

      {/* 선택한 방 채팅 모달 */}
      {selectedRoom && (
        <CompanyChatModal
          companyName={selectedRoom.memberEmail}
          subtitle={`${company.name} 문의 답변`}
          roomId={selectedRoom.roomId}
          onMinimize={() => setSelectedRoom(null)}
        />
      )}
    </div>
  );
};

export default ManagerInquiryInbox;
