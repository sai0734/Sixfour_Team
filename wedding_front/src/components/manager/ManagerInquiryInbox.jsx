import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getCompanyInquiryRooms } from "../../api/companyInquiryApi";
import CompanyChatModal from "../chat/CompanyChatModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import useManagedCompany from "../../hooks/useManagedCompany";
import { subscribeInquiryTopic } from "../../util/inquiryWsClient";

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

// WS로 방 하나의 갱신 정보가 오면, 이미 목록에 있으면 덮어쓰고 없으면 새로 추가한 뒤
// 최근 대화순으로 다시 정렬한다
const mergeRoomUpdate = (prev, update) => {
  const exists = prev.some((room) => room.roomId === update.roomId);
  const next = exists
    ? prev.map((room) =>
        room.roomId === update.roomId ? { ...room, ...update } : room,
      )
    : [update, ...prev];

  return next.slice().sort((a, b) => {
    const aTime = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
    const bTime = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
    return bTime - aTime;
  });
};

// 매니저가 "나가기"한 방 id 목록 — 서버 데이터는 그대로 두고 이 업체 계정의 받은편지함
// 화면에서만 숨긴다 (회원이 새 메시지를 보내면 다시 나타남)
const HIDDEN_STORAGE_PREFIX = "wedding_manager_hidden_inquiry_rooms_";

const loadHiddenRoomIds = (cmno) => {
  if (!cmno) return new Set();
  try {
    const raw = localStorage.getItem(`${HIDDEN_STORAGE_PREFIX}${cmno}`);
    if (!raw) return new Set();
    const parsed = JSON.parse(raw);
    return new Set(Array.isArray(parsed) ? parsed : []);
  } catch {
    return new Set();
  }
};

const persistHiddenRoomIds = (cmno, ids) => {
  if (!cmno) return;
  localStorage.setItem(
    `${HIDDEN_STORAGE_PREFIX}${cmno}`,
    JSON.stringify([...ids]),
  );
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
  const [hiddenRoomIds, setHiddenRoomIds] = useState(() => new Set());

  // 담당 업체의 문의방 목록 조회 (최초 1회 - 이후 갱신은 WebSocket으로 받음)
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

    setHiddenRoomIds(loadHiddenRoomIds(company.cmno));
    loadRooms();

    const unsubscribe = subscribeInquiryTopic(
      `/topic/inquiries/company/${company.cmno}`,
      (roomUpdate) => {
        setRooms((prev) => mergeRoomUpdate(prev, roomUpdate));
        // 나갔던 방에 회원이 새 메시지를 보냈으면 숨김을 풀어서 다시 보이게 한다
        setHiddenRoomIds((prev) => {
          if (!prev.has(roomUpdate.roomId)) return prev;
          const next = new Set(prev);
          next.delete(roomUpdate.roomId);
          return next;
        });
      },
    );

    return unsubscribe;
  }, [company?.cmno, loadRooms]);

  useEffect(() => {
    if (!company?.cmno) return;
    persistHiddenRoomIds(company.cmno, hiddenRoomIds);
  }, [company?.cmno, hiddenRoomIds]);

  // 채팅창에서 "나가기" 눌렀을 때 — 서버 데이터는 그대로 두고 목록에서만 숨긴다
  const handleLeaveRoom = (roomId) => {
    setHiddenRoomIds((prev) => new Set(prev).add(roomId));
    setSelectedRoom((prev) => (prev?.roomId === roomId ? null : prev));
  };

  // 목록에서 바로 "나가기" — 채팅창을 열지 않고 그 방만 숨긴다
  const handleLeaveFromList = (e, roomId, memberEmail) => {
    e.stopPropagation();
    const confirmed = window.confirm(
      `${memberEmail || "이 회원"} 문의를 목록에서 나가시겠습니까?\n대화 내용은 서버에 남아있고, 새 메시지가 오면 다시 표시됩니다.`,
    );
    if (confirmed) {
      handleLeaveRoom(roomId);
    }
  };

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

  const visibleRooms = rooms.filter((room) => !hiddenRoomIds.has(room.roomId));

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
        {roomsLoading && visibleRooms.length === 0 ? (
          <p className="px-5 py-10 text-center text-sm text-ink-muted sm:px-6">
            문의 목록을 불러오는 중...
          </p>
        ) : visibleRooms.length === 0 ? (
          <p className="px-5 py-10 text-center text-sm text-ink-muted sm:px-6">
            아직 들어온 문의가 없습니다.
          </p>
        ) : (
          visibleRooms.map((room) => {
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
                  <div className="flex items-center gap-1.5">
                    {room.unread && (
                      <span
                        className="h-2 w-2 shrink-0 rounded-full bg-red-500"
                        aria-hidden="true"
                        title="읽지 않은 메시지"
                      />
                    )}
                    <p
                      className={`truncate text-sm text-ink ${
                        room.unread ? "font-semibold" : "font-medium"
                      }`}
                    >
                      {room.memberEmail || "알 수 없는 회원"}
                    </p>
                  </div>
                  <p className="mt-1 text-xs text-ink-muted">
                    {room.status === "CLOSED" ? "종료됨" : "진행 중"}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <div className="text-right">
                    <p className="text-xs text-ink-faint">
                      {formatLastMessageAt(room.lastMessageAt)}
                    </p>
                    <p className="mt-1 text-xs text-brand">답변하기 →</p>
                  </div>
                  <span
                    role="button"
                    tabIndex={0}
                    onClick={(e) =>
                      handleLeaveFromList(e, room.roomId, room.memberEmail)
                    }
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        handleLeaveFromList(e, room.roomId, room.memberEmail);
                      }
                    }}
                    className="shrink-0 rounded-full px-3 py-1.5 text-xs text-ink-muted transition hover:bg-blush-50 hover:text-brand"
                  >
                    나가기
                  </span>
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
          onLeave={() => handleLeaveRoom(selectedRoom.roomId)}
        />
      )}
    </div>
  );
};

export default ManagerInquiryInbox;
