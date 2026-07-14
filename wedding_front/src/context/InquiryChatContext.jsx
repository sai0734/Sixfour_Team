import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useSelector } from "react-redux";
import { openInquiryRoom } from "../api/companyInquiryApi";

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
        (session) =>
          session?.roomId && session?.cmno && session?.companyName,
      )
      .map((session) => ({
        roomId: Number(session.roomId),
        cmno: Number(session.cmno),
        companyName: String(session.companyName),
        view: "minimized",
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

  useEffect(() => {
    if (!email) {
      setSessions([]);
      return;
    }
    setSessions(loadStoredSessions(email));
  }, [email]);

  useEffect(() => {
    if (!email) return;
    persistSessions(email, sessions);
  }, [email, sessions]);

  const startInquiry = useCallback(
    async (cmno, companyName) => {
      if (!email || !cmno) return false;

      const existing = sessions.find((session) => session.cmno === Number(cmno));
      if (existing) {
        setSessions((prev) =>
          prev.map((session) =>
            session.roomId === existing.roomId
              ? { ...session, view: "open" }
              : session,
          ),
        );
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
              view: "open",
            },
          ];
        });
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

  const openInquiry = useCallback((roomId) => {
    setSessions((prev) =>
      prev.map((session) =>
        session.roomId === roomId ? { ...session, view: "open" } : session,
      ),
    );
  }, []);

  const minimizeInquiry = useCallback((roomId) => {
    setSessions((prev) =>
      prev.map((session) =>
        session.roomId === roomId ? { ...session, view: "minimized" } : session,
      ),
    );
  }, []);

  const value = useMemo(
    () => ({
      sessions,
      startInquiry,
      openInquiry,
      minimizeInquiry,
      isOpening: openingCmno != null,
    }),
    [sessions, startInquiry, openInquiry, minimizeInquiry, openingCmno],
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
