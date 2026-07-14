import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getMyInquiryThread, sendInquiryMessage } from "../../api/inquiryApi";

const CompanyInquiryWidget = ({ cmno }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const { moveToLogin } = useCustomLogin();

  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(false);

  const loadThread = () => {
    if (!loginState.email) return;
    getMyInquiryThread(cmno)
      .then((data) => setMessages(data))
      .catch((err) => console.error(err));
  };

  useEffect(() => {
    loadThread();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cmno, loginState.email]);

  const handleSend = () => {
    if (!loginState.email) {
      if (
        window.confirm(
          "로그인 후 문의할 수 있어요. 로그인 화면으로 이동할까요?",
        )
      ) {
        moveToLogin();
      }
      return;
    }

    if (!content.trim()) return;

    setLoading(true);
    sendInquiryMessage(cmno, content.trim())
      .then(() => {
        setContent("");
        loadThread();
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  return (
    <div className="mt-5 rounded-lg border border-slate-200 bg-white p-5">
      <h3 className="mb-3 text-base font-semibold">업체에 문의하기</h3>

      {messages.length > 0 && (
        <div className="mb-4 max-h-64 overflow-y-auto space-y-2 rounded-md bg-slate-50 p-3">
          {messages.map((m) => (
            <div
              key={m.id}
              className={`flex ${m.senderType === "MEMBER" ? "justify-end" : "justify-start"}`}
            >
              <div
                className={`max-w-[75%] rounded-2xl px-3 py-1.5 text-sm ${
                  m.senderType === "MEMBER"
                    ? "bg-blue-600 text-white"
                    : "bg-white border border-slate-200 text-slate-700"
                }`}
              >
                {m.content}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="flex gap-2">
        <input
          type="text"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSend()}
          placeholder="궁금한 점을 물어보세요"
          className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-400"
        />
        <button
          onClick={handleSend}
          disabled={loading}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          문의하기
        </button>
      </div>
    </div>
  );
};

export default CompanyInquiryWidget;
