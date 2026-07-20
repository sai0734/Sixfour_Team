import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import {
  changeMemberRole,
  getAdminList,
  getMemberList,
  reactivateMember,
  suspendMember,
} from "../../api/adminMemberApi";
import {
  getList as getCompanyList,
  assignCompanyManager,
  unassignCompanyManager,
  getManagedCompanyByEmail,
  getManagedCompanies,
} from "../../api/companyApi";
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
  WITHDRAWN: "탈퇴",
};

const statusColor = {
  ACTIVE: "bg-emerald-50 text-emerald-700 border border-emerald-200",
  BLACKLIST: "bg-red-50 text-red-700 border border-red-200",
  DORMANT: "bg-slate-100 text-slate-600 border border-slate-200",
  WITHDRAWN: "bg-slate-200 text-slate-500 border border-slate-300",
};

const formatDate = (value) => {
  if (!value) return "-";
  return String(value).slice(0, 16).replace("T", " ");
};

const MemberManageComponent = () => {
  const currentEmail = useSelector((state) => state.loginSlice?.email);

  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState("");

  // 검색창에 입력 중인 값 (아직 조회에 반영 안 됨)
  const [keywordInput, setKeywordInput] = useState("");

  // 실제 조회에 사용되는 조건
  const [queryParam, setQueryParam] = useState({
    page: 1,
    size: 10,
    keyword: "",
    status: "",
  });

  // 정지 처리 모달 대상 (null이면 닫힘)
  const [suspendTarget, setSuspendTarget] = useState(null);
  const [suspendReason, setSuspendReason] = useState("");
  const [suspendDays, setSuspendDays] = useState("7");

  const [actionEmail, setActionEmail] = useState(null); // 처리중인 행 (버튼 중복 클릭 방지)

  // 업체 담당자 임명 모달
  const [managerTarget, setManagerTarget] = useState(null); // null이면 닫힘
  const [managerCurrent, setManagerCurrent] = useState(null); // 이미 담당 중인 업체(있으면)
  const [companyOptions, setCompanyOptions] = useState([]);
  const [selectedCmno, setSelectedCmno] = useState("");
  const [managerLoading, setManagerLoading] = useState(false);

  // 회원 목록 / 담당자 목록 / 관리자 목록 탭
  const [activeTab, setActiveTab] = useState("members"); // "members" | "managers" | "admins"
  const [managerList, setManagerList] = useState([]);
  const [managerListLoading, setManagerListLoading] = useState(false);
  const [adminList, setAdminList] = useState([]);
  const [adminListLoading, setAdminListLoading] = useState(false);

  const fetchManagerList = () => {
    setManagerListLoading(true);
    getManagedCompanies()
      .then((data) => setManagerList(data))
      .catch((err) => {
        console.error(err);
        alert("담당자 목록을 불러오지 못했습니다.");
      })
      .finally(() => setManagerListLoading(false));
  };

  const fetchAdminList = () => {
    setAdminListLoading(true);
    getAdminList()
      .then((data) => setAdminList(data))
      .catch((err) => {
        console.error(err);
        alert("관리자 목록을 불러오지 못했습니다.");
      })
      .finally(() => setAdminListLoading(false));
  };

  useEffect(() => {
    if (activeTab === "managers") {
      fetchManagerList();
    } else if (activeTab === "admins") {
      fetchAdminList();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  const handleUnassignFromList = async (company) => {
    if (
      !window.confirm(
        `${company.name}의 담당자(${company.managerEmail}) 지정을 해제할까요?`,
      )
    ) {
      return;
    }
    try {
      await unassignCompanyManager(company.cmno);
      fetchManagerList();
    } catch (err) {
      console.error(err);
      alert("해제 중 오류가 발생했습니다.");
    }
  };

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

  // 검색창을 완전히 지우면(빈 문자열) 검색 버튼 안 눌러도 바로 전체 목록으로 리셋
  const handleKeywordChange = (e) => {
    const value = e.target.value;
    setKeywordInput(value);

    if (value === "") {
      setQueryParam((prev) => ({ ...prev, page: 1, keyword: "" }));
    }
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

  // 업체 담당자 임명 모달
  const openManagerModal = async (member) => {
    setManagerTarget(member);
    setSelectedCmno("");
    setManagerCurrent(null);
    setManagerLoading(true);

    try {
      const [companyRes, myManaged] = await Promise.all([
        companyOptions.length > 0
          ? Promise.resolve({ dtoList: companyOptions })
          : getCompanyList({ page: 1, size: 200 }),
        getManagedCompanyByEmail(member.email),
      ]);

      if (companyOptions.length === 0) {
        setCompanyOptions(companyRes.dtoList || []);
      }

      if (myManaged.isManager) {
        setManagerCurrent(myManaged.company);
        setSelectedCmno(String(myManaged.company.cmno));
      }
    } catch (err) {
      console.error(err);
      alert("업체 목록을 불러오지 못했습니다.");
    } finally {
      setManagerLoading(false);
    }
  };

  const closeManagerModal = () => {
    setManagerTarget(null);
    setManagerCurrent(null);
    setSelectedCmno("");
  };

  const confirmAssignManager = async () => {
    if (!selectedCmno) {
      alert("업체를 선택해주세요.");
      return;
    }

    try {
      setManagerLoading(true);
      await assignCompanyManager(Number(selectedCmno), managerTarget.email);
      alert(`${managerTarget.email} 님을 담당자로 임명했습니다.`);
      closeManagerModal();
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.msg;
      alert(msg || "임명 중 오류가 발생했습니다.");
    } finally {
      setManagerLoading(false);
    }
  };

  const handleUnassignManager = async () => {
    if (!managerCurrent) return;
    if (!window.confirm(`${managerCurrent.name} 담당자 지정을 해제할까요?`))
      return;

    try {
      setManagerLoading(true);
      await unassignCompanyManager(managerCurrent.cmno);
      alert("담당자 지정을 해제했습니다.");
      closeManagerModal();
    } catch (err) {
      console.error(err);
      alert("해제 중 오류가 발생했습니다.");
    } finally {
      setManagerLoading(false);
    }
  };

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

  const handleRoleChange = async (member, role) => {
    const roleLabel = role === "ADMIN" ? "관리자" : "일반 사용자";

    if (
      !window.confirm(
        `${member.nickname}(${member.email}) 님을 ${roleLabel}(으)로 변경할까요?`,
      )
    ) {
      return;
    }

    try {
      setActionEmail(member.email);
      await changeMemberRole(member.email, role);
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      alert("권한 변경 중 오류가 발생했습니다.");
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

      {/* 회원 목록 / 담당자 목록 / 관리자 목록 탭 */}
      <div className="mb-4 flex gap-1 border-b border-slate-200">
        <button
          onClick={() => setActiveTab("members")}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px ${
            activeTab === "members"
              ? "border-blue-600 text-blue-600"
              : "border-transparent text-slate-500 hover:text-slate-700"
          }`}
        >
          회원 목록
        </button>
        <button
          onClick={() => setActiveTab("managers")}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px ${
            activeTab === "managers"
              ? "border-blue-600 text-blue-600"
              : "border-transparent text-slate-500 hover:text-slate-700"
          }`}
        >
          담당자 목록
        </button>
        <button
          onClick={() => setActiveTab("admins")}
          className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px ${
            activeTab === "admins"
              ? "border-blue-600 text-blue-600"
              : "border-transparent text-slate-500 hover:text-slate-700"
          }`}
        >
          관리자 목록
        </button>
      </div>

      {activeTab === "admins" ? (
        <div className="overflow-x-auto rounded-lg border border-slate-200">
          <table className="w-full min-w-[600px] text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">닉네임</th>
                <th className="px-4 py-3">이메일</th>
                <th className="px-4 py-3">가입일</th>
                <th className="px-4 py-3">최근 로그인</th>
                <th className="px-4 py-3">관리</th>
              </tr>
            </thead>
            <tbody>
              {adminListLoading ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-8 text-center text-slate-400"
                  >
                    불러오는 중...
                  </td>
                </tr>
              ) : adminList.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-8 text-center text-slate-400"
                  >
                    관리자 계정이 없습니다.
                  </td>
                </tr>
              ) : (
                adminList.map((member) => (
                  <tr key={member.email} className="border-t border-slate-100">
                    <td className="px-4 py-3 font-medium">
                      {member.nickname}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {member.email}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {formatDate(member.regDate)}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {formatDate(member.lastLoginAt)}
                    </td>
                    <td className="px-4 py-3">
                      {member.email === currentEmail ? (
                        <span className="text-xs text-slate-400">
                          본인 계정이에요
                        </span>
                      ) : (
                        <button
                          disabled={actionEmail === member.email}
                          onClick={async () => {
                            await handleRoleChange(member, "USER");
                            fetchAdminList();
                          }}
                          className="rounded-md bg-red-50 px-2.5 py-1 text-xs font-medium text-red-600 hover:bg-red-100 disabled:opacity-50"
                        >
                          권한 해제
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : activeTab === "managers" ? (
        <div className="overflow-x-auto rounded-lg border border-slate-200">
          <table className="w-full min-w-[600px] text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">업체명</th>
                <th className="px-4 py-3">카테고리</th>
                <th className="px-4 py-3">담당자 이메일</th>
                <th className="px-4 py-3">관리</th>
              </tr>
            </thead>
            <tbody>
              {managerListLoading ? (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-8 text-center text-slate-400"
                  >
                    불러오는 중...
                  </td>
                </tr>
              ) : managerList.length === 0 ? (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-8 text-center text-slate-400"
                  >
                    아직 지정된 담당자가 없습니다.
                  </td>
                </tr>
              ) : (
                managerList.map((company) => (
                  <tr key={company.cmno} className="border-t border-slate-100">
                    <td className="px-4 py-3 font-medium">{company.name}</td>
                    <td className="px-4 py-3 text-slate-600">
                      {company.category}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {company.managerEmail}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleUnassignFromList(company)}
                        className="rounded-md bg-red-50 px-2.5 py-1 text-xs font-medium text-red-600 hover:bg-red-100"
                      >
                        담당 해제
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <>
          {/* 검색 + 필터 */}
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <form onSubmit={handleSearch} className="flex gap-2">
              <input
                type="text"
                value={keywordInput}
                onChange={handleKeywordChange}
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
                { value: "WITHDRAWN", label: "탈퇴" },
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
                  <th className="px-4 py-3">권한</th>
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
                      colSpan={8}
                      className="px-4 py-8 text-center text-slate-400"
                    >
                      불러오는 중...
                    </td>
                  </tr>
                ) : serverData.dtoList.length === 0 ? (
                  <tr>
                    <td
                      colSpan={8}
                      className="px-4 py-8 text-center text-slate-400"
                    >
                      조건에 맞는 회원이 없습니다.
                    </td>
                  </tr>
                ) : (
                  serverData.dtoList.map((member) => (
                    <tr
                      key={member.email}
                      className="border-t border-slate-100"
                    >
                      <td className="px-4 py-3 font-medium">
                        {member.nickname}
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        {member.email}
                      </td>
                      <td className="px-4 py-3">
                        <select
                          value={member.admin ? "ADMIN" : "USER"}
                          disabled={
                            member.email === currentEmail ||
                            member.status === "WITHDRAWN" ||
                            actionEmail === member.email
                          }
                          onChange={(e) => {
                            const role = e.target.value;
                            if (role === "MANAGER") {
                              openManagerModal(member);
                              return;
                            }
                            handleRoleChange(member, role);
                          }}
                          className="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-600 disabled:opacity-50"
                        >
                          <option value="USER">일반 사용자</option>
                          <option value="ADMIN">관리자</option>
                          <option value="MANAGER">업체 담당자</option>
                        </select>
                      </td>
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
                        {member.status === "BLACKLIST" &&
                        member.suspendReason ? (
                          <div className="mt-1 text-xs text-slate-400">
                            {member.suspendReason}
                            {member.suspendUntil
                              ? ` · ~${formatDate(member.suspendUntil)}`
                              : " · 영구정지"}
                          </div>
                        ) : null}
                      </td>
                      <td className="px-4 py-3">
                        {member.admin ? (
                          <span className="text-xs text-slate-400">
                            관리자 계정은 상태를 변경할 수 없어요
                          </span>
                        ) : member.status === "WITHDRAWN" ? (
                          <span className="text-xs text-slate-400">
                            탈퇴한 회원이에요
                          </span>
                        ) : (
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
                            {member.status === "BLACKLIST" && (
                              <button
                                disabled={actionEmail === member.email}
                                onClick={() => handleReactivate(member)}
                                className="rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100 disabled:opacity-50"
                              >
                                정지 해제
                              </button>
                            )}
                            {member.status === "DORMANT" && (
                              <button
                                disabled={actionEmail === member.email}
                                onClick={() => handleReactivate(member)}
                                className="rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100 disabled:opacity-50"
                              >
                                휴면 해제
                              </button>
                            )}
                          </div>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <PageComponent serverData={serverData} movePage={movePage} />
        </>
      )}

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

            <label className="mb-1.5 block text-xs font-medium text-slate-600">
              정지 기간
            </label>
            <div className="mb-4 flex flex-wrap gap-1.5">
              {[
                { label: "1일", value: "1" },
                { label: "7일", value: "7" },
                { label: "30일", value: "30" },
                { label: "365일", value: "365" },
                { label: "영구", value: "" },
              ].map((preset) => (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => setSuspendDays(preset.value)}
                  className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition ${
                    suspendDays === preset.value
                      ? "bg-red-600 text-white"
                      : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>

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

      {/* 업체 담당자 임명 모달 */}
      {managerTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6">
            <h3 className="mb-1 text-base font-semibold text-slate-800">
              업체 담당자 임명
            </h3>
            <p className="mb-4 text-xs text-slate-500">
              {managerTarget.email} 님을 업체 문의 답변 담당자로 지정합니다.
            </p>

            {managerCurrent && (
              <div className="mb-4 rounded-md bg-indigo-50 border border-indigo-200 px-3 py-2 text-xs text-indigo-700">
                현재 <strong>{managerCurrent.name}</strong>의 담당자로 지정되어
                있어요.
              </div>
            )}

            <label className="mb-1.5 block text-xs font-medium text-slate-600">
              담당할 업체 선택
            </label>
            <select
              value={selectedCmno}
              onChange={(e) => setSelectedCmno(e.target.value)}
              disabled={managerLoading}
              className="mb-4 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-400"
            >
              <option value="">-- 업체를 선택하세요 --</option>
              {companyOptions.map((c) => (
                <option key={c.cmno} value={c.cmno}>
                  [{c.category}] {c.name}
                </option>
              ))}
            </select>

            <div className="flex justify-between gap-2">
              <div>
                {managerCurrent && (
                  <button
                    onClick={handleUnassignManager}
                    disabled={managerLoading}
                    className="rounded-md bg-red-50 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-100 disabled:opacity-50"
                  >
                    담당 해제
                  </button>
                )}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={closeManagerModal}
                  className="rounded-md px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
                >
                  취소
                </button>
                <button
                  onClick={confirmAssignManager}
                  disabled={managerLoading}
                  className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
                >
                  임명하기
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
};

export default MemberManageComponent;
