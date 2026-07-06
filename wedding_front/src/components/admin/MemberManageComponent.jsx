import { useEffect, useState } from "react";
import {
  getMemberList,
  reactivateMember,
  setDormantMember,
  suspendMember,
} from "../../api/adminMemberApi";
import PageComponent from "../common/PageComponent";

const initState = {
  dtoList: [],
  pageNumList: [],
  pageRequestDTO: { page: 1, size: 10 },
  prev: false,
  next: false,
  totalCount: 0,
  current: 1,
};

const statusLabel = {
  ACTIVE: "정상",
  BLACKLIST: "정지",
  DORMANT: "휴면",
};

const statusColor = {
  ACTIVE: "bg-emerald-50 text-emerald-700 border border-emerald-200",
  BLACKLIST: "bg-red-50 text-red-700 border border-red-200",
  DORMANT: "bg-slate-100 text-slate-600 border border-slate-200",
};

const formatDate = (value) => {
  if (!value) return "-";
  return String(value).slice(0, 16).replace("T", " ");
};

const MemberManageComponent = () => {
  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState("");

  const [keywordInput, setKeywordInput] = useState("");

  const [queryParam, setQueryParam] = useState({
    page: 1,
    size: 10,
    keyword: "",
    status: "",
  });

  const [suspendTarget, setSuspendTarget] = useState(null);
  const [suspendReason, setSuspendReason] = useState("");
  const [suspendDays, setSuspendDays] = useState("7");

  const [actionEmail, setActionEmail] = useState(null);

  const fetchList = (param) => {
    setFetching(true);
    setError("");

    getMemberList(param)
      .then((data) => {
        setServerData({ ...initState, ...data });
      })
      .catch((err) => {
        console.error(err);
        setError("회원 목록을 불러오지 못했습니다.");
      })
      .finally(() => setFetching(false));
  };

  useEffect(() => {
    fetchList(queryParam);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryParam]);

  const movePage = (pageParam) => {
    setQueryParam((prev) => ({ ...prev, ...pageParam }));
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setQueryParam((prev) => ({ ...prev, page: 1, keyword: keywordInput }));
  };

  const handleStatusFilter = (status) => {
    setQueryParam((prev) => ({ ...prev, page: 1, status }));
  };

  const openSuspendModal = (member) => {
    setSuspendTarget(member);
    setSuspendReason("");
    setSuspendDays("7");
  };

  const closeSuspendModal = () => setSuspendTarget(null);

  const confirmSuspend = async () => {
    if (!suspendReason.trim()) {
      alert("정지 사유를 입력해주세요.");
      return;
    }

    const days = suspendDays === "" ? null : Number(suspendDays);

    try {
      setActionEmail(suspendTarget.email);
      await suspendMember(suspendTarget.email, suspendReason.trim(), days);
      closeSuspendModal();
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      alert("정지 처리 중 오류가 발생했습니다.");
    } finally {
      setActionEmail(null);
    }
  };

  const handleDormant = async (member) => {
    if (
      !window.confirm(
        `${member.nickname}(${member.email}) 님을 휴면 계정으로 전환할까요?`,
      )
    ) {
      return;
    }

    try {
      setActionEmail(member.email);
      await setDormantMember(member.email);
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      alert("휴면 전환 중 오류가 발생했습니다.");
    } finally {
      setActionEmail(null);
    }
  };

  const handleReactivate = async (member) => {
    if (
      !window.confirm(
        `${member.nickname}(${member.email}) 님을 정상 상태로 되돌릴까요?`,
      )
    ) {
      return;
    }

    try {
      setActionEmail(member.email);
      await reactivateMember(member.email);
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      alert("처리 중 오류가 발생했습니다.");
    } finally {
      setActionEmail(null);
    }
  };

  return (
    <section className="mx-auto max-w-6xl p-4 text-slate-800">
      <div className="mb-6">
        <h2 className="text-xl font-semibold">회원 관리</h2>
        <p className="mt-1 text-sm text-slate-500">
          전체 회원 {serverData.totalCount}명 · 닉네임/이메일 검색과 상태 필터로
          좁혀볼 수 있어요.
        </p>
      </div>

      {error ? (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      ) : null}

      {/* 검색 + 필터 */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <form onSubmit={handleSearch} className="flex gap-2">
          <input
            type="text"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            placeholder="닉네임 또는 이메일 검색"
            className="w-64 rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-400"
          />
          <button
            type="submit"
            className="rounded-md bg-slate-800 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
          >
            검색
          </button>
        </form>

        <div className="flex gap-2">
          {[
            { value: "", label: "전체" },
            { value: "ACTIVE", label: "정상" },
            { value: "BLACKLIST", label: "정지" },
            { value: "DORMANT", label: "휴면" },
          ].map((opt) => (
            <button
              key={opt.value || "ALL"}
              onClick={() => handleStatusFilter(opt.value)}
              className={`rounded-full px-3 py-1.5 text-sm font-medium transition ${
                queryParam.status === opt.value
                  ? "bg-blue-600 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* 테이블 */}
      <div className="overflow-x-auto rounded-lg border border-slate-200">
        <table className="w-full min-w-[820px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">닉네임</th>
              <th className="px-4 py-3">이메일</th>
              <th className="px-4 py-3">가입일</th>
              <th className="px-4 py-3">최근 로그인</th>
              <th className="px-4 py-3">포인트</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3">관리</th>
            </tr>
          </thead>
          <tbody>
            {fetching ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-4 py-8 text-center text-slate-400"
                >
                  불러오는 중...
                </td>
              </tr>
            ) : serverData.dtoList.length === 0 ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-4 py-8 text-center text-slate-400"
                >
                  조건에 맞는 회원이 없습니다.
                </td>
              </tr>
            ) : (
              serverData.dtoList.map((member) => (
                <tr key={member.email} className="border-t border-slate-100">
                  <td className="px-4 py-3 font-medium">{member.nickname}</td>
                  <td className="px-4 py-3 text-slate-600">{member.email}</td>
                  <td className="px-4 py-3 text-slate-600">
                    {formatDate(member.regDate)}
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {formatDate(member.lastLoginAt)}
                  </td>
                  {/* 포인트 테이블이 아직 없어서 임시로 - 표시 (담당자 확인 필요) */}
                  <td className="px-4 py-3 text-slate-400">-</td>
                  <td className="px-4 py-3">
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs font-medium ${statusColor[member.status]}`}
                    >
                      {statusLabel[member.status] || member.status}
                    </span>
                    {member.status === "BLACKLIST" && member.suspendReason ? (
                      <div className="mt-1 text-xs text-slate-400">
                        {member.suspendReason}
                        {member.suspendUntil
                          ? ` · ~${formatDate(member.suspendUntil)}`
                          : " · 영구정지"}
                      </div>
                    ) : null}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1.5">
                      {member.status !== "BLACKLIST" && (
                        <button
                          disabled={actionEmail === member.email}
                          onClick={() => openSuspendModal(member)}
                          className="rounded-md bg-red-50 px-2.5 py-1 text-xs font-medium text-red-600 hover:bg-red-100 disabled:opacity-50"
                        >
                          정지
                        </button>
                      )}
                      {member.status === "ACTIVE" && (
                        <button
                          disabled={actionEmail === member.email}
                          onClick={() => handleDormant(member)}
                          className="rounded-md bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600 hover:bg-slate-200 disabled:opacity-50"
                        >
                          휴면 전환
                        </button>
                      )}
                      {(member.status === "BLACKLIST" ||
                        member.status === "DORMANT") && (
                        <button
                          disabled={actionEmail === member.email}
                          onClick={() => handleReactivate(member)}
                          className="rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100 disabled:opacity-50"
                        >
                          정상 복귀
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <PageComponent serverData={serverData} movePage={movePage} />

      {/* 정지 처리 모달 */}
      {suspendTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-sm rounded-lg bg-white p-5 shadow-lg">
            <h3 className="mb-1 text-lg font-semibold">활동 정지 처리</h3>
            <p className="mb-4 text-sm text-slate-500">
              {suspendTarget.nickname} ({suspendTarget.email})
            </p>

            <label className="mb-1 block text-xs font-medium text-slate-600">
              정지 사유 *
            </label>
            <textarea
              value={suspendReason}
              onChange={(e) => setSuspendReason(e.target.value)}
              rows={3}
              placeholder="신고 누적, 이용약관 위반 등"
              className="mb-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-400"
            />

            <label className="mb-1 block text-xs font-medium text-slate-600">
              정지 기간(일)
            </label>
            <input
              type="number"
              min="0"
              value={suspendDays}
              onChange={(e) => setSuspendDays(e.target.value)}
              placeholder="비워두면 영구 정지"
              className="mb-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-400"
            />
            <p className="mb-4 text-xs text-slate-400">
              비워두거나 0을 입력하면 영구 정지로 처리돼요.
            </p>

            <div className="flex justify-end gap-2">
              <button
                onClick={closeSuspendModal}
                className="rounded-md px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
              >
                취소
              </button>
              <button
                onClick={confirmSuspend}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
              >
                정지 처리
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
};

export default MemberManageComponent;
