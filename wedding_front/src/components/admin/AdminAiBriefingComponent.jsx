import { useEffect, useState } from "react";
import {
  deleteAiBriefing,
  getAiBriefings,
  openAiBriefingPdf,
  triggerWeeklyBriefing,
} from "../../api/aiOpsApi";
import AdminLayout from "../../layouts/AdminLayout";
import ShopTapeLabel from "../product/ShopTapeLabel";
import OpenClawTriggerButton from "./OpenClawTriggerButton";

const formatDate = (value) => {
  if (!value) return "-";
  return String(value).slice(0, 16).replace("T", " ");
};

const AdminAiBriefingComponent = () => {
  const [briefings, setBriefings] = useState([]);
  const [fetching, setFetching] = useState(true);
  const [openingId, setOpeningId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  const fetchList = () => {
    setFetching(true);
    return getAiBriefings()
      .then((data) => setBriefings(data))
      .finally(() => setFetching(false));
  };

  // 폴링 전용 - 화면의 로딩 표시를 건드리지 않고 조용히 건수만 확인
  const peekCount = () => getAiBriefings().then((data) => data.length);

  useEffect(() => {
    fetchList();
  }, []);

  const handleOpenPdf = async (id) => {
    setOpeningId(id);
    try {
      await openAiBriefingPdf(id);
    } catch (err) {
      console.error(err);
      alert("PDF를 여는 데 실패했습니다.");
    } finally {
      setOpeningId(null);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("이 브리핑을 삭제할까요? PDF 파일도 함께 삭제됩니다.")) {
      return;
    }
    setDeletingId(id);
    try {
      await deleteAiBriefing(id);
      fetchList();
    } catch (err) {
      console.error(err);
      alert("삭제에 실패했습니다.");
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <AdminLayout>
      <div className="mb-6">
        <ShopTapeLabel className="mb-2.5">관리자</ShopTapeLabel>
        <p className="font-['Gowun_Batang'] text-2xl text-ink">AI 매니저 주간 브리핑</p>
        <p className="mt-1 text-sm text-ink-soft">
          OpenClaw가 매주 월요일 새벽에 만든 경영 인사이트 리포트입니다.
        </p>
      </div>

      <div className="mb-5 rounded-2xl bg-white p-4 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
        <OpenClawTriggerButton
          label="지금 바로 브리핑 생성하기"
          triggerFn={triggerWeeklyBriefing}
          onDone={fetchList}
          pollFn={peekCount}
          pollIntervalSeconds={5}
          estimatedSeconds={150}
        />
      </div>

      {fetching ? (
        <div className="rounded-2xl bg-white p-8 text-center text-sm text-ink-soft shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          불러오는 중...
        </div>
      ) : briefings.length === 0 ? (
        <div className="rounded-2xl bg-white p-8 text-center text-sm text-ink-soft shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          아직 생성된 브리핑이 없습니다. 다음 주 월요일 새벽에 처음 생성됩니다.
        </div>
      ) : (
        <div className="space-y-4">
          {briefings.map((item) => (
            <div
              key={item.id}
              className="rounded-2xl bg-white p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]"
            >
              <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                <span className="text-sm font-medium text-ink">{item.weekOf}</span>
                <span className="text-xs text-ink-faint">{formatDate(item.regDate)}</span>
              </div>

              <p className="mb-3 text-sm text-ink-soft">{item.summaryText}</p>

              <div className="mb-4 flex flex-wrap gap-2 text-xs">
                <span className="rounded-full bg-cream px-3 py-1 text-ink-soft">
                  재고 부족 {item.lowStockCount}건
                </span>
                <span className="rounded-full bg-cream px-3 py-1 text-ink-soft">
                  확인 필요 게시글 {item.flaggedPostCount}건
                </span>
                <span className="rounded-full bg-cream px-3 py-1 text-ink-soft">
                  사이트 이슈 {item.siteIssueCount}건
                </span>
              </div>

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => handleDelete(item.id)}
                  disabled={deletingId === item.id}
                  className="h-9 rounded-full border border-line-soft px-5 text-sm text-ink-soft transition hover:bg-cream disabled:opacity-50"
                >
                  {deletingId === item.id ? "삭제 중..." : "삭제"}
                </button>
                <button
                  type="button"
                  onClick={() => handleOpenPdf(item.id)}
                  disabled={openingId === item.id}
                  className="h-9 rounded-full bg-brand px-5 text-sm text-white transition hover:opacity-90 disabled:opacity-50"
                >
                  {openingId === item.id ? "여는 중..." : "PDF 열람"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </AdminLayout>
  );
};

export default AdminAiBriefingComponent;
