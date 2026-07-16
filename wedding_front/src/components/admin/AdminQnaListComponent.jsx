import { useEffect, useState } from "react";
import { createSearchParams, useNavigate, useSearchParams } from "react-router-dom";
import { getUnansweredQnaList, postQnaReply } from "../../api/qnaApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import PageComponent from "../common/PageComponent";
import AdminLayout from "../../layouts/AdminLayout";
import ShopTapeLabel from "../product/ShopTapeLabel";

const initState = {
  dtoList: [],
  pageNumList: [],
  totalCount: 0,
  current: 0,
};

const formatDate = (value) => {
  if (!value) return "-";
  return String(value).slice(0, 16).replace("T", " ");
};

const AdminQnaListComponent = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const page = Number(searchParams.get("page")) || 1;
  const size = Number(searchParams.get("size")) || 10;

  const moveToList = (pageParam) => {
    const pageNum = pageParam?.page ?? page;
    const sizeNum = pageParam?.size ?? size;

    navigate({
      pathname: "/admin/qna",
      search: createSearchParams({ page: pageNum, size: sizeNum }).toString(),
    });
  };

  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(true);
  const [replyDrafts, setReplyDrafts] = useState({});
  const [submittingQno, setSubmittingQno] = useState(null);

  useEffect(() => {
    if (!searchParams.get("page")) {
      navigate(
        {
          pathname: "/admin/qna",
          search: createSearchParams({ page: 1, size }).toString(),
        },
        { replace: true },
      );
    }
  }, [navigate, searchParams, size]);

  const fetchList = () => {
    setFetching(true);
    getUnansweredQnaList({ page, size })
      .then((data) => setServerData(data))
      .finally(() => setFetching(false));
  };

  useEffect(() => {
    fetchList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size]);

  const handleDraftChange = (qno, value) => {
    setReplyDrafts((prev) => ({ ...prev, [qno]: value }));
  };

  const handleSubmitReply = async (item) => {
    const content = (replyDrafts[item.qno] || "").trim();
    if (!content) {
      alert("답변 내용을 입력해주세요.");
      return;
    }

    setSubmittingQno(item.qno);
    try {
      await postQnaReply(item.pno, item.qno, content);
      setReplyDrafts((prev) => {
        const next = { ...prev };
        delete next[item.qno];
        return next;
      });
      fetchList();
    } catch (err) {
      console.error(err);
      alert("답변 등록에 실패했습니다.");
    } finally {
      setSubmittingQno(null);
    }
  };

  return (
    <AdminLayout>
      <div className="mb-6">
        <ShopTapeLabel className="mb-2.5">관리자</ShopTapeLabel>
        <p className="font-['Gowun_Batang'] text-2xl text-ink">답변 안 된 상품 Q&A</p>
        <p className="mt-1 text-sm text-ink-soft">
          총 {serverData.totalCount}건의 질문이 답변을 기다리고 있습니다.
        </p>
      </div>

      {fetching ? (
        <div className="rounded-2xl bg-white p-8 text-center text-sm text-ink-soft shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          불러오는 중...
        </div>
      ) : serverData.dtoList.length === 0 ? (
        <div className="rounded-2xl bg-white p-8 text-center text-sm text-ink-soft shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          답변 대기 중인 질문이 없습니다. 🎉
        </div>
      ) : (
        <div className="space-y-4">
          {serverData.dtoList.map((item) => (
            <div
              key={item.qno}
              className="rounded-2xl bg-white p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]"
            >
              <div className="mb-3 flex items-start gap-3">
                <div className="h-14 w-14 shrink-0 overflow-hidden rounded-lg bg-cream">
                  {item.thumbnail ? (
                    <img
                      alt={item.pname}
                      className="h-full w-full object-cover"
                      src={`${API_SERVER_HOST}/api/product/view/s_${item.thumbnail}`}
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center text-[10px] text-ink-faint">
                      No Img
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <button
                      type="button"
                      onClick={() => navigate(`/product/read/${item.pno}`)}
                      className="text-sm font-medium text-brand-deep hover:underline"
                    >
                      {item.pname}
                    </button>
                    <span className="text-xs text-ink-faint">{formatDate(item.regDate)}</span>
                  </div>
                  <div className="mt-1 text-xs text-ink-soft">
                    {item.nickname} ({item.memberEmail})
                  </div>
                </div>
              </div>

              <div className="mb-4 rounded-lg bg-cream px-4 py-3 text-sm text-ink">
                {item.content}
              </div>

              <div className="flex flex-col gap-2 sm:flex-row">
                <textarea
                  value={replyDrafts[item.qno] || ""}
                  onChange={(e) => handleDraftChange(item.qno, e.target.value)}
                  placeholder="답변을 입력하세요"
                  rows={2}
                  className="flex-1 resize-none rounded-lg border border-line-soft px-3 py-2 text-sm focus:outline-none focus:border-brand"
                />
                <button
                  type="button"
                  onClick={() => handleSubmitReply(item)}
                  disabled={submittingQno === item.qno}
                  className="h-9 shrink-0 self-end rounded-full bg-brand px-5 text-sm text-white transition hover:opacity-90 disabled:opacity-50 sm:self-auto"
                >
                  {submittingQno === item.qno ? "등록 중..." : "답변 등록"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <PageComponent serverData={serverData} movePage={moveToList} />
    </AdminLayout>
  );
};

export default AdminQnaListComponent;
