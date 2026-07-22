import { useEffect, useState } from "react";

// 관리자가 OpenClaw 작업(일간 점검 / 주간 브리핑)을 즉시 실행시키는 버튼.
// 실제 작업은 백그라운드에서 도는 거라 HTTP 응답만으로는 완료를 알 수 없다. 그래서:
// - pollFn이 있으면: 몇 초 간격으로 조용히 다시 조회해서 결과(건수 등)가 실제로 바뀌었는지 확인하고,
//   바뀌면 예상 시간이 안 지났어도 즉시 "완료"로 전환 + onDone으로 부모 목록을 새로고침시킨다.
// - 바뀐 걸 못 찾으면(예: 이번엔 새로 발견된 게 없는 경우): 예상 시간이 지나면 안내 문구로 전환.
const OpenClawTriggerButton = ({
  label,
  triggerFn,
  onDone,
  pollFn,
  pollIntervalSeconds = 5,
  estimatedSeconds = 90,
}) => {
  const [status, setStatus] = useState("idle"); // idle | running | done | done-early | error
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [baseline, setBaseline] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");

  const handleClick = async () => {
    setStatus("running");
    setSecondsLeft(estimatedSeconds);
    setBaseline(null);
    setErrorMessage("");

    try {
      if (pollFn) {
        const snapshot = await pollFn();
        setBaseline(snapshot);
      }
      await triggerFn();
    } catch (err) {
      console.error(err);
      if (err.response?.status === 409) {
        setErrorMessage("이미 진단이 실행 중입니다. 잠시 후 다시 시도해주세요.");
      } else {
        setErrorMessage("진단 실행에 실패했습니다. OpenClaw 게이트웨이가 켜져 있는지 확인해주세요.");
      }
      setStatus("error");
    }
  };

  // 카운트다운
  useEffect(() => {
    if (status !== "running") return undefined;

    if (secondsLeft <= 0) {
      setStatus("done");
      return undefined;
    }

    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [status, secondsLeft]);

  // 실제 완료 여부 폴링 (pollFn을 넘겼을 때만)
  useEffect(() => {
    if (status !== "running" || !pollFn || baseline === null) return undefined;

    const interval = setInterval(async () => {
      try {
        const snapshot = await pollFn();
        if (snapshot !== baseline) {
          setStatus("done-early");
          if (onDone) onDone();
        }
      } catch (err) {
        console.error(err);
      }
    }, pollIntervalSeconds * 1000);

    return () => clearInterval(interval);
  }, [status, pollFn, baseline, onDone, pollIntervalSeconds]);

  return (
    <div>
      <button
        type="button"
        onClick={handleClick}
        disabled={status === "running"}
        className="h-9 w-full rounded-full bg-brand-deep text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-60"
      >
        {status === "running" ? "실행 중..." : label}
      </button>

      {status === "running" && (
        <div className="mt-3 flex items-center gap-3 rounded-xl bg-cream px-3 py-3">
          <svg
            className="h-6 w-6 shrink-0 animate-spin text-brand-deep"
            viewBox="0 0 24 24"
            fill="none"
          >
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path
              className="opacity-90"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
            />
          </svg>
          <div className="text-[11px] leading-relaxed text-ink-soft">
            <p className="font-medium text-ink">지금 진단하고 있습니다...</p>
            <p>완료까지 약 {secondsLeft}초 남았어요.</p>
          </div>
        </div>
      )}

      {status === "done-early" && (
        <p className="mt-2 text-[11px] leading-relaxed text-brand-deep">
          진단이 완료되어 결과를 새로 불러왔습니다. ✅
        </p>
      )}

      {status === "done" && (
        <p className="mt-2 text-[11px] leading-relaxed text-ink-faint">
          진단이 끝났을 거예요. 새로고침해서 결과를 확인해보세요.
        </p>
      )}

      {status === "error" && (
        <p className="mt-2 text-[11px] leading-relaxed text-red-600">{errorMessage}</p>
      )}
    </div>
  );
};

export default OpenClawTriggerButton;
