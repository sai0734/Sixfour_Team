import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getOne as getBoardOne } from "../../api/boardApi";
import {
  getFlaggedPostCount,
  getFlaggedPosts,
  getSiteHealthIssueCount,
  getSiteHealthIssues,
  resolveFlaggedPost,
  resolveSiteHealthIssue,
  triggerDailyCheck,
} from "../../api/aiOpsApi";
import OpenClawTriggerButton from "./OpenClawTriggerButton";

// 일간 체크가 이제 이미지 깨짐 하나만 보고하도록 단순화됨 (콘솔에러/이미지미등록 구분 없음)
const ISSUE_TYPE_LABEL = {
  IMAGE_BROKEN: "이미지 깨짐",
};

// 관리자 대시보드 옆에서 스크롤을 따라다니는 플로팅 패널.
// OpenClaw가 매일 새벽 발견한 "사이트 이상 징후" / "확인 필요한 게시글"을 바로 보여주고 처리한다.
const AdminAlertPanel = () => {
  const navigate = useNavigate();

  const [siteIssues, setSiteIssues] = useState([]);
  const [flaggedPosts, setFlaggedPosts] = useState([]);
  const [siteIssueCount, setSiteIssueCount] = useState(0);
  const [flaggedPostCount, setFlaggedPostCount] = useState(0);
  const [fetching, setFetching] = useState(true);
  const [resolvingKey, setResolvingKey] = useState(null);

  const fetchAll = () => {
    setFetching(true);
    Promise.all([
      getSiteHealthIssues(),
      getFlaggedPosts(),
      getSiteHealthIssueCount(),
      getFlaggedPostCount(),
    ])
      .then(([issues, posts, issueCount, postCount]) => {
        setSiteIssues(issues);
        setFlaggedPosts(posts);
        setSiteIssueCount(issueCount);
        setFlaggedPostCount(postCount);
      })
      .catch((err) => console.error(err))
      .finally(() => setFetching(false));
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const handleResolveIssue = async (id) => {
    setResolvingKey(`issue-${id}`);
    try {
      await resolveSiteHealthIssue(id);
      fetchAll();
    } catch (err) {
      console.error(err);
      alert("처리 완료 표시에 실패했습니다.");
    } finally {
      setResolvingKey(null);
    }
  };

  const handleResolvePost = async (id) => {
    setResolvingKey(`post-${id}`);
    try {
      await resolveFlaggedPost(id);
      fetchAll();
    } catch (err) {
      console.error(err);
      alert("처리 완료 표시에 실패했습니다.");
    } finally {
      setResolvingKey(null);
    }
  };

  // 사이트 이상 징후는 pageUrl을 그대로 가지고 있으니 새 탭으로 바로 열어줌
  const handleOpenIssue = (item) => {
    window.open(item.pageUrl, "_blank", "noopener,noreferrer");
  };

  // 확인 필요한 게시글은 boardId만 있어서, 자유/후기 게시판 중 어디 소속인지 조회한 뒤
  // 해당 게시판 페이지로 이동시키면서 ?openId=로 상세를 바로 열게 한다
  const handleOpenPost = async (item) => {
    try {
      const board = await getBoardOne(item.boardId);
      const path = board.boardType === "REVIEW" ? "/board/review" : "/board/free";
      navigate(`${path}?openId=${item.boardId}`);
    } catch (err) {
      console.error(err);
      alert("게시글을 찾을 수 없습니다. 이미 삭제되었을 수 있습니다.");
    }
  };

  const totalCount = siteIssueCount + flaggedPostCount;

  return (
    <div className="w-full rounded-2xl bg-white p-4 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-ink">OpenClaw 알림</h3>
        <span className="rounded-full bg-brand-light px-2.5 py-1 text-xs text-brand-deep">
          {fetching ? "확인 중" : `${totalCount}건`}
        </span>
      </div>

      <AlertSection title="사이트 이상 징후">
        {fetching ? (
          <EmptyText>불러오는 중...</EmptyText>
        ) : siteIssues.length === 0 ? (
          <EmptyText>이상 없음 🎉</EmptyText>
        ) : (
          <>
            {siteIssues.map((item) => (
              <AlertCard
                key={item.id}
                badge={ISSUE_TYPE_LABEL[item.issueType] || item.issueType}
                title={item.pageUrl}
                detail={item.detail}
                resolving={resolvingKey === `issue-${item.id}`}
                onResolve={() => handleResolveIssue(item.id)}
                onOpen={() => handleOpenIssue(item)}
              />
            ))}
            {siteIssueCount > siteIssues.length ? (
              <MoreNote count={siteIssueCount - siteIssues.length} />
            ) : null}
          </>
        )}
      </AlertSection>

      <AlertSection title="확인 필요한 게시글">
        {fetching ? (
          <EmptyText>불러오는 중...</EmptyText>
        ) : flaggedPosts.length === 0 ? (
          <EmptyText>이상 없음 🎉</EmptyText>
        ) : (
          <>
            {flaggedPosts.map((item) => (
              <AlertCard
                key={item.id}
                badge={`게시글 #${item.boardId}`}
                detail={item.reason}
                resolving={resolvingKey === `post-${item.id}`}
                onResolve={() => handleResolvePost(item.id)}
                onOpen={() => handleOpenPost(item)}
              />
            ))}
            {flaggedPostCount > flaggedPosts.length ? (
              <MoreNote count={flaggedPostCount - flaggedPosts.length} />
            ) : null}
          </>
        )}
      </AlertSection>

      <div className="mt-4 border-t border-line-soft pt-3">
        <OpenClawTriggerButton
          label="지금 바로 진단하기"
          triggerFn={triggerDailyCheck}
          onDone={fetchAll}
          pollFn={async () => {
            const [issueCount, postCount] = await Promise.all([
              getSiteHealthIssueCount(),
              getFlaggedPostCount(),
            ]);
            return issueCount + postCount;
          }}
          pollIntervalSeconds={5}
          estimatedSeconds={180}
        />
      </div>
    </div>
  );
};

const AlertSection = ({ title, children }) => (
  <div className="mb-4 last:mb-0">
    <p className="mb-2 text-xs font-medium text-ink-faint">{title}</p>
    <div className="max-h-72 space-y-2 overflow-y-auto pr-1">{children}</div>
  </div>
);

const AlertCard = ({ badge, title, detail, resolving, onResolve, onOpen }) => (
  <div
    onClick={onOpen || undefined}
    className={`rounded-lg bg-cream p-3 transition ${onOpen ? "cursor-pointer hover:bg-cream/70" : ""}`}
  >
    <div className="mb-1 flex items-center justify-between gap-2">
      <span className="rounded-full bg-white px-2 py-0.5 text-[11px] font-medium text-brand-deep">
        {badge}
      </span>
    </div>
    {title ? <p className="truncate text-xs font-medium text-ink">{title}</p> : null}
    <p className="mt-1 text-xs text-ink-soft">{detail}</p>
    <div className="mt-2 flex justify-end">
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          onResolve();
        }}
        disabled={resolving}
        className="h-7 rounded-full bg-brand px-3 text-[11px] text-white transition hover:opacity-90 disabled:opacity-50"
      >
        {resolving ? "처리 중..." : "처리 완료"}
      </button>
    </div>
  </div>
);

const MoreNote = ({ count }) => (
  <p className="px-1 text-center text-[11px] text-ink-faint">외 {count}건 더 있음 (최근순 5건만 표시)</p>
);

const EmptyText = ({ children }) => (
  <div className="rounded-lg bg-cream px-3 py-4 text-center text-xs text-ink-faint">
    {children}
  </div>
);

export default AdminAlertPanel;
