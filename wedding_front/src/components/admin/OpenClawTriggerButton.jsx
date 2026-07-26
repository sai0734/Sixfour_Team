import { useEffect, useRef, useState } from "react";
import { cancelOpenClawJob } from "../../api/aiOpsApi";

// 변화가 한 번 감지된 뒤, 이만큼 연속으로 더 이상 변화가 없으면 그때 완료로 간주한다.
// 진단 스크립트가 이미지 점검(사이트 이상 징후)을 먼저 보고하고 게시글/리뷰 점검(확인 필요한
// 게시글)을 나중에 보고하는 구조라, 첫 변화 감지 즉시 폴링을 멈추면 나중에 보고되는 건들이
// 화면에 반영 안 되고 새로고침해야만 보이는 문제가 있었다 - 실제로 겪은 버그.
const QUIET_TICKS_TO_FINISH = 2;

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
  const [status, setStatus] = useState("idle"); // idle | running | overtime | done-early | error
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [baseline, setBaseline] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [cancelling, setCancelling] = useState(false);

  // 폴링 도중 계속 갱신되는 값들 - 매 tick마다 effect를 다시 구독시키지 않도록 ref로 관리
  const lastSeenRef = useRef(null);
  const changeDetectedRef = useRef(false);
  const quietTicksRef = useRef(0);

  const handleClick = async () => {
    setStatus("running");
    setSecondsLeft(estimatedSeconds);
    setBaseline(null);
    setErrorMessage("");
    changeDetectedRef.current = false;
    quietTicksRef.current = 0;

    try {
      if (pollFn) {
        const snapshot = await pollFn();
        setBaseline(snapshot);
        lastSeenRef.current = snapshot;
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

  // 작업 하나만 콕 집어 취소하는 방법이 없어서, 게이트웨이 자체를 재시작해서 강제로 멈춘다.
  // 그 순간 다른 작업이 같이 돌고 있었다면 그것도 같이 끊기므로 반드시 확인창을 거친다.
  const handleCancel = async () => {
    if (!window.confirm("정말 취소할까요? OpenClaw 게이트웨이 자체를 재시작해서 멈추는 거라, 지금 동시에 도는 다른 작업(일간/주간)이 있다면 그것도 같이 중단됩니다.")) {
      return;
    }
    setCancelling(true);
    try {
      await cancelOpenClawJob();
      setStatus("idle");
      setSecondsLeft(0);
      setErrorMessage("");
    } catch (err) {
      console.error(err);
      alert("취소(게이트웨이 재시작)에 실패했습니다.");
    } finally {
      setCancelling(false);
    }
  };

  // 카운트다운 — 예상 시간이 다 돼도 실제로 끝났다는 뜻이 아니라서,
  // 폴링을 멈추지 않고 "예상보다 오래 걸리는 중" 상태로만 넘어감
  useEffect(() => {
    if (status !== "running") return undefined;

    if (secondsLeft <= 0) {
      setStatus("overtime");
      return undefined;
    }

    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [status, secondsLeft]);

  // 실제 완료 여부 폴링 (pollFn을 넘겼을 때만) — 예상 시간이 지나도(overtime)
  // pollFn이 실제 변화를 감지할 때까지 계속 돌아야 진짜 완료를 알 수 있음
  useEffect(() => {
    if ((status !== "running" && status !== "overtime") || !pollFn || baseline === null) {
      return undefined;
    }

    const interval = setInterval(async () => {
      try {
        const snapshot = await pollFn();
        if (snapshot !== lastSeenRef.current) {
          // 새 변화 감지 - 진단 스크립트가 아직 더 보고할 수 있으니 여기서 바로 새로고침하지
          // 않는다. 사이트 이상징후 쪽이 먼저 저장되고 게시글 쪽이 나중에 저장되는 구조라,
          // 감지될 때마다 매번 새로고침하면 두 섹션이 따로따로 순차적으로 나타나 보임 -
          // 실제로 이 문제가 있었음. 조용한 구간에 도달했을 때 한 번만 같이 새로고침한다.
          lastSeenRef.current = snapshot;
          changeDetectedRef.current = true;
          quietTicksRef.current = 0;
        } else if (changeDetectedRef.current) {
          quietTicksRef.current += 1;
          if (quietTicksRef.current >= QUIET_TICKS_TO_FINISH) {
            setStatus("done-early");
            if (onDone) onDone();
          }
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
        disabled={status === "running" || status === "overtime"}
        className="h-9 w-full rounded-full bg-brand-deep text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-60"
      >
        {status === "running" || status === "overtime" ? "실행 중..." : label}
      </button>

      {(status === "running" || status === "overtime") && (
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
          <div className="min-w-0 flex-1 text-[11px] leading-relaxed text-ink-soft">
            <p className="font-medium text-ink">지금 진단하고 있습니다...</p>
            {status === "running" ? (
              <p>완료까지 약 {secondsLeft}초 남았어요.</p>
            ) : pollFn ? (
              <p>예상보다 오래 걸리고 있어요. 계속 확인 중이니 조금만 기다려주세요.</p>
            ) : (
              <p>진단이 끝났을 거예요. 새로고침해서 결과를 확인해보세요.</p>
            )}
          </div>
          <button
            type="button"
            onClick={handleCancel}
            disabled={cancelling}
            className="h-7 shrink-0 rounded-full border border-red-300 px-3 text-[11px] text-red-600 transition hover:bg-red-50 disabled:opacity-50"
          >
            {cancelling ? "취소 중..." : "취소"}
          </button>
        </div>
      )}

      {status === "done-early" && (
        <p className="mt-2 text-[11px] leading-relaxed text-brand-deep">
          진단이 완료되어 결과를 새로 불러왔습니다. ✅
        </p>
      )}

      {status === "error" && (
        <p className="mt-2 text-[11px] leading-relaxed text-red-600">{errorMessage}</p>
      )}
    </div>
  );
};

export default OpenClawTriggerButton;
