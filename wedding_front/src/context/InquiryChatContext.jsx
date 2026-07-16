import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useSelector } from "react-redux";
import {
  getMyInquiryRooms,
  openInquiryRoom,
  sendInquiryMessage,
} from "../api/companyInquiryApi";
import {
  connectInquiryWs,
  disconnectInquiryWs,
  subscribeInquiryTopic,
} from "../util/inquiryWsClient";

const STORAGE_PREFIX = "wedding_inquiry_sessions_";

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
  // "문의하기"는 눌렀지만 아직 메시지를 한 번도 안 보낸 상태 - 서버에 방을 안 만들고 여기만 채워둠
  const [draftInquiry, setDraftInquiry] = useState(null);
  const [isListOpen, setIsListOpen] = useState(false);

  useEffect(() => {
    if (!email) {
      setSessions([]);
      setActiveRoomId(null);
      setIsListOpen(false);
      return;
    }

    const stored = loadStoredSessions(email);
    setSessions(stored);

    // 수정: localStorage엔 unread 정보가 없고, 그 사이(창 닫힘/재연결 중 등) 놓친 WS 갱신을
    // 따라잡을 방법이 없었음 - 로그인/새로고침 시 서버의 최신 방 상태로 한 번 동기화한다
    let cancelled = false;
    getMyInquiryRooms()
      .then((rooms) => {
        if (cancelled) return;
        setSessions((prev) => {
          const companyNameByRoomId = new Map(
            prev.map((session) => [session.roomId, session.companyName]),
          );
          return (Array.isArray(rooms) ? rooms : []).map((room) => ({
            roomId: room.roomId,
            cmno: room.cmno,
            companyName: companyNameByRoomId.get(room.roomId) ?? "",
            unread: Boolean(room.unread),
            lastMessageAt: room.lastMessageAt,
          }));
        });
      })
      .catch((err) => {
        console.error("내 문의방 목록 동기화 실패:", err);
      });

    return () => {
      cancelled = true;
    };
  }, [email]);

  useEffect(() => {
    if (!email) return;
    persistSessions(email, sessions);
  }, [email, sessions]);

  // 로그인 상태가 바뀌면 WebSocket 연결도 같이 시작/종료
  useEffect(() => {
    if (!email) {
      disconnectInquiryWs();
      return;
    }
    connectInquiryWs();
  }, [email]);

  // 내 문의방들의 안읽음/최근 대화 상태를 실시간으로 반영 (기존 20초 폴링 대체)
  useEffect(() => {
    if (!email) return;

    const unsubscribe = subscribeInquiryTopic(
      `/topic/inquiries/member/${email}`,
      (roomUpdate) => {
        setSessions((prev) => {
          let changed = false;
          const next = prev.map((session) => {
            if (session.roomId !== roomUpdate.roomId) return session;

            const nextUnread = Boolean(roomUpdate.unread);
            if (
              nextUnread === Boolean(session.unread) &&
              roomUpdate.lastMessageAt === session.lastMessageAt
            ) {
              return session;
            }

            changed = true;
            return {
              ...session,
              unread: nextUnread,
              lastMessageAt: roomUpdate.lastMessageAt,
            };
          });
          return changed ? next : prev;
        });
      },
    );

    return unsubscribe;
  }, [email]);

  // "문의하기" 클릭 - 이미 대화중인 업체면 그 방을 열고, 처음이면 draft만 열어둠(서버 호출 없음)
  const startInquiry = useCallback(
    (cmno, companyName) => {
      if (!email || !cmno) return false;

      const existing = sessions.find(
        (session) => session.cmno === Number(cmno),
      );
      if (existing) {
        setDraftInquiry(null);
        setActiveRoomId(existing.roomId);
        setIsListOpen(false);
        return true;
      }

      setDraftInquiry({ cmno: Number(cmno), companyName });
      setActiveRoomId(null);
      setIsListOpen(false);
      return true;
    },
    [email, sessions],
  );

  // draft 상태에서 첫 메시지를 실제로 보낼 때 - 방 생성 + 메시지 전송을 순서대로 하고,
  // 둘 다 성공해야 그때 sessions(목록)에 추가한다
  const startNewRoomWithMessage = useCallback(
    async (cmno, companyName, text) => {
      if (openingCmno === cmno) return null;

      setOpeningCmno(cmno);
      try {
        const room = await openInquiryRoom(cmno);
        await sendInquiryMessage(room.roomId, text);

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
        setDraftInquiry(null);
        return room.roomId;
      } catch (err) {
        console.error("문의방 생성/첫 메시지 전송 실패:", err);
        throw err;
      } finally {
        setOpeningCmno(null);
      }
    },
    [openingCmno],
  );

  // 목록에서 방을 선택 — 그 방을 열고 목록은 닫는다
  const openInquiry = useCallback((roomId) => {
    setDraftInquiry(null);
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
    setDraftInquiry(null);
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
      draftInquiry,
      isListOpen,
      closeInquiry,
      openList,
      closeList,
      startInquiry,
      startNewRoomWithMessage,
      openInquiry,
      minimizeInquiry,
      isOpening: openingCmno != null,
    }),
    [
      sessions,
      activeRoomId,
      draftInquiry,
      isListOpen,
      closeInquiry,
      openList,
      closeList,
      startInquiry,
      startNewRoomWithMessage,
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
