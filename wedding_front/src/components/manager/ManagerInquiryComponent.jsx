import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import {
  getManagerThreads,
  getManagerThread,
  sendManagerReply,
} from "../../api/inquiryApi";
import { getMyManagedCompany } from "../../api/companyApi";

const ManagerInquiryComponent = () => {
  const loginState = useSelector((state) => state.loginSlice);

  const [company, setCompany] = useState(null);
  const [threads, setThreads] = useState([]);
  const [selectedEmail, setSelectedEmail] = useState(null);
  const [messages, setMessages] = useState([]);
  const [reply, setReply] = useState("");
  const [loading, setLoading] = useState(true);

  const loadThreads = () => {
    getManagerThreads()
      .then((data) => setThreads(data))
      .catch((err) => console.error(err));
  };

  useEffect(() => {
    if (!loginState.email) return;

    getMyManagedCompany()
      .then((data) => setCompany(data.company))
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));

    loadThreads();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loginState.email]);

  const openThread = (memberEmail) => {
    setSelectedEmail(memberEmail);
    getManagerThread(memberEmail)
      .then((data) => setMessages(data))
      .catch((err) => console.error(err));
  };

  const handleSendReply = () => {
    if (!reply.trim() || !selectedEmail) return;

    sendManagerReply(selectedEmail, reply.trim())
      .then(() => {
        setReply("");
        openThread(selectedEmail);
        loadThreads();
      })
      .catch((err) => console.error(err));
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-sm text-slate-500">
        불러오는 중...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b border-slate-200 px-6 py-4">
        <h1 className="text-lg font-semibold text-slate-800">
          {company?.name || "업체"} 문의 관리
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          {loginState.email} 담당자 계정 — 이 계정은 문의 답변만 가능해요
        </p>
      </header>

      <div
        className="max-w-5xl mx-auto flex gap-4 p-6"
        style={{ height: "calc(100vh - 90px)" }}
      >
        {/* 문의자 목록 */}
        <aside className="w-64 shrink-0 bg-white rounded-xl border border-slate-200 overflow-y-auto">
          {threads.length === 0 && (
            <p className="p-4 text-xs text-slate-400">
              아직 들어온 문의가 없어요.
            </p>
          )}
          {threads.map((t) => (
            <button
              key={t.memberEmail}
              onClick={() => openThread(t.memberEmail)}
              className={`w-full text-left px-4 py-3 border-b border-slate-100 hover:bg-slate-50 ${
                selectedEmail === t.memberEmail ? "bg-blue-50" : ""
              }`}
            >
              <div className="text-sm font-medium text-slate-800 truncate">
                {t.memberEmail}
              </div>
              <div className="text-xs text-slate-500 truncate mt-0.5">
                {t.lastMessage}
              </div>
            </button>
          ))}
        </aside>

        {/* 대화창 */}
        <section className="flex-1 bg-white rounded-xl border border-slate-200 flex flex-col">
          {!selectedEmail ? (
            <div className="flex-1 flex items-center justify-center text-sm text-slate-400">
              왼쪽에서 문의자를 선택해주세요
            </div>
          ) : (
            <>
              <div className="px-4 py-3 border-b border-slate-200 text-sm font-medium text-slate-700">
                {selectedEmail}
              </div>
              <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {messages.map((m) => (
                  <div
                    key={m.id}
                    className={`flex ${m.senderType === "MANAGER" ? "justify-end" : "justify-start"}`}
                  >
                    <div
                      className={`max-w-[70%] px-4 py-2 rounded-2xl text-sm ${
                        m.senderType === "MANAGER"
                          ? "bg-blue-600 text-white"
                          : "bg-slate-100 text-slate-800"
                      }`}
                    >
                      {m.content}
                    </div>
                  </div>
                ))}
              </div>
              <div className="p-3 border-t border-slate-200 flex gap-2">
                <input
                  type="text"
                  value={reply}
                  onChange={(e) => setReply(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleSendReply()}
                  placeholder="답변을 입력하세요"
                  className="flex-1 px-4 py-2 rounded-full border border-slate-300 text-sm outline-none focus:border-blue-400"
                />
                <button
                  onClick={handleSendReply}
                  className="px-5 py-2 rounded-full bg-blue-600 text-white text-sm font-medium hover:bg-blue-700"
                >
                  전송
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
};

export default ManagerInquiryComponent;
