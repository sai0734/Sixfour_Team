import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useSelector } from "react-redux";
import { getMyInquiryRooms, openInquiryRoom } from "../api/companyInquiryApi";

const STORAGE_PREFIX = "wedding_inquiry_sessions_";
const UNREAD_POLL_INTERVAL_MS = 20000;

const InquiryChatContext = createContext(null);

const loadStoredSessions = (email) => {
  if (!email) return [];

  try {
    const raw = localStorage.getItem(`${STORAGE_PREFIX}${email}`);
    if (!raw) return [];

    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];

    return parsed
      .filter(
        (session) => session?.roomId && session?.cmno && session?.companyName,
      )
      .map((session) => ({
        roomId: Number(session.roomId),
        cmno: Number(session.cmno),
        companyName: String(session.companyName),
      }));
  } catch {
    return [];
  }
};

const persistSessions = (email, sessions) => {
  if (!email) return;

  const payload = sessions.map(({ roomId, cmno, companyName }) => ({
    roomId,
    cmno,
    companyName,
  }));

  localStorage.setItem(`${STORAGE_PREFIX}${email}`, JSON.stringify(payload));
};

export const InquiryChatProvider = ({ children }) => {
  const email = useSelector((state) => state.loginSlice?.email);
  const [sessions, setSessions] = useState([]);
  const [openingCmno, setOpeningCmno] = useState(null);
  const [activeRoomId, setActiveRoomId] = useState(null);
  const [isListOpen, setIsListOpen] = useState(false);

  useEffect(() => {
    if (!email) {
      setSessions([]);
      setActiveRoomId(null);
      setIsListOpen(false);
      return;
    }
    setSessions(loadStoredSessions(email));
  }, [email]);

  useEffect(() => {
    if (!email) return;
    persistSessions(email, sessions);
  }, [email, sessions]);

  useEffect(() => {
    if (!email) return;
    if (sessions.length === 0) return;
    if (activeRoomId != null) return;

    const pollUnread = async () => {
      if (document.hidden) return;

      try {
        const rooms = await getMyInquiryRooms();
        setSessions((prev) => {
          let changed = false;
          const next = prev.map((session) => {
            const match = rooms.find((room) => room.roomId === session.roomId);
            if (!match) return session;

            const nextUnread = Boolean(match.unread);
            if (
              nextUnread === Boolean(session.unread) &&
              match.lastMessageAt === session.lastMessageAt
            ) {
              return session;
            }

            changed = true;
            return {
              ...session,
              unread: nextUnread,
              lastMessageAt: match.lastMessageAt,
            };
          });
          return changed ? next : prev;
        });
      } catch (err) {
        console.error("문의방 안읽음 상태 조회 실패:", err);
      }
    };

    pollUnread();
    const timer = setInterval(pollUnread, UNREAD_POLL_INTERVAL_MS);

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        pollUnread();
      }
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [email, sessions.length, activeRoomId]);

  const startInquiry = useCallback(
    async (cmno, companyName) => {
      if (!email || !cmno) return false;

      const existing = sessions.find(
        (session) => session.cmno === Number(cmno),
      );
      if (existing) {
        setActiveRoomId(existing.roomId);
        setIsListOpen(false);
        return true;
      }

      if (openingCmno === cmno) return false;

      setOpeningCmno(cmno);
      try {
        const room = await openInquiryRoom(cmno);
        setSessions((prev) => {
          const next = prev.filter((session) => session.roomId !== room.roomId);
          return [
            ...next,
            {
              roomId: room.roomId,
              cmno: Number(cmno),
              companyName,
            },
          ];
        });
        setActiveRoomId(room.roomId);
        setIsListOpen(false);
        return true;
      } catch (err) {
        console.error("문의방 열기 실패:", err);
        alert("문의 채팅을 시작할 수 없습니다. 다시 시도해주세요.");
        return false;
      } finally {
        setOpeningCmno(null);
      }
    },
    [email, openingCmno, sessions],
  );

  // 목록에서 방을 선택 — 그 방을 열고 목록은 닫는다
  const openInquiry = useCallback((roomId) => {
    setActiveRoomId(roomId);
    setIsListOpen(false);
    setSessions((prev) =>
      prev.map((session) =>
        session.roomId === roomId ? { ...session, unread: false } : session,
      ),
    );
  }, []);

  const minimizeInquiry = useCallback(() => {
    setActiveRoomId(null);
  }, []);

  const closeInquiry = useCallback((roomId) => {
    setSessions((prev) => prev.filter((session) => session.roomId !== roomId));
    setActiveRoomId((prev) => (prev === roomId ? null : prev));
  }, []);

  const openList = useCallback(() => {
    setIsListOpen(true);
  }, []);

  const closeList = useCallback(() => {
    setIsListOpen(false);
  }, []);

  const value = useMemo(
    () => ({
      sessions,
      activeRoomId,
      isListOpen,
      closeInquiry,
      openList,
      closeList,
      startInquiry,
      openInquiry,
      minimizeInquiry,
      isOpening: openingCmno != null,
    }),
    [
      sessions,
      activeRoomId,
      isListOpen,
      closeInquiry,
      openList,
      closeList,
      startInquiry,
      openInquiry,
      minimizeInquiry,
      openingCmno,
    ],
  );

  return (
    <InquiryChatContext.Provider value={value}>
      {children}
    </InquiryChatContext.Provider>
  );
};

const useInquiryChat = () => {
  const context = useContext(InquiryChatContext);
  if (!context) {
    throw new Error("useInquiryChat must be used within InquiryChatProvider");
  }
  return context;
};

export default useInquiryChat;
