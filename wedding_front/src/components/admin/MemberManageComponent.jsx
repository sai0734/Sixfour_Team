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
import AdminLayout from "../../layouts/AdminLayout";
import ShopTapeLabel from "../product/ShopTapeLabel";
import { showAlert } from "../../util/globalAlert";

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
  ACTIVE: "bg-emerald-100 text-emerald-700",
  BLACKLIST: "bg-red-100 text-red-700",
  DORMANT: "bg-gray-200 text-gray-600",
  WITHDRAWN: "bg-gray-200 text-gray-500",
};

const MEMBER_TABS = [
  { key: "members", label: "회원 목록" },
  { key: "managers", label: "담당자 목록" },
  { key: "admins", label: "관리자 목록" },
];

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

  // 담당자 목록 탭 검색/필터 (이미 전체가 로드돼 있어서 클라이언트에서 필터링)
  const [managerKeywordInput, setManagerKeywordInput] = useState("");
  const [managerKeyword, setManagerKeyword] = useState("");
  const [managerCategory, setManagerCategory] = useState("");

  // 관리자 목록 탭 검색 (역시 클라이언트 필터링)
  const [adminKeywordInput, setAdminKeywordInput] = useState("");
  const [adminKeyword, setAdminKeyword] = useState("");

  // 확인 모달 (window.confirm 대체) - { message, resolve } / null이면 닫힘
  const [confirmState, setConfirmState] = useState(null);

  const askConfirm = (message) =>
    new Promise((resolve) => setConfirmState({ message, resolve }));

  const closeConfirm = (result) => {
    confirmState?.resolve(result);
    setConfirmState(null);
  };

  const fetchManagerList = () => {
    setManagerListLoading(true);
    getManagedCompanies()
      .then((data) => setManagerList(data))
      .catch((err) => {
        console.error(err);
        showAlert("담당자 목록을 불러오지 못했습니다.");
      })
      .finally(() => setManagerListLoading(false));
  };

  const fetchAdminList = () => {
    setAdminListLoading(true);
    getAdminList()
      .then((data) => setAdminList(data))
      .catch((err) => {
        console.error(err);
        showAlert("관리자 목록을 불러오지 못했습니다.");
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
    const ok = await askConfirm(
      `${company.name}의 담당자(${company.managerEmail}) 지정을 해제할까요?`,
    );
    if (!ok) return;
    try {
      await unassignCompanyManager(company.cmno);
      fetchManagerList();
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      showAlert("해제 중 오류가 발생했습니다.");
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
      showAlert("업체 목록을 불러오지 못했습니다.");
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
      showAlert("업체를 선택해주세요.");
      return;
    }

    try {
      setManagerLoading(true);
      await assignCompanyManager(Number(selectedCmno), managerTarget.email);
      showAlert(`${managerTarget.email} 님을 담당자로 임명했습니다.`);
      closeManagerModal();
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      const msg = err.response?.data?.msg;
      showAlert(msg || "임명 중 오류가 발생했습니다.");
    } finally {
      setManagerLoading(false);
    }
  };

  const handleUnassignManager = async () => {
    if (!managerCurrent) return;
    const ok = await askConfirm(`${managerCurrent.name} 담당자 지정을 해제할까요?`);
    if (!ok) return;

    try {
      setManagerLoading(true);
      await unassignCompanyManager(managerCurrent.cmno);
      showAlert("담당자 지정을 해제했습니다.");
      closeManagerModal();
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      showAlert("해제 중 오류가 발생했습니다.");
    } finally {
      setManagerLoading(false);
    }
  };

  const confirmSuspend = async () => {
    if (!suspendReason.trim()) {
      showAlert("정지 사유를 입력해주세요.");
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
      showAlert("정지 처리 중 오류가 발생했습니다.");
    } finally {
      setActionEmail(null);
    }
  };

  const handleReactivate = async (member) => {
    const ok = await askConfirm(
      `${member.nickname}(${member.email}) 님을 정상 상태로 되돌릴까요?`,
    );
    if (!ok) return;

    try {
      setActionEmail(member.email);
      await reactivateMember(member.email);
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      showAlert("처리 중 오류가 발생했습니다.");
    } finally {
      setActionEmail(null);
    }
  };

  const handleRoleChange = async (member, role) => {
    const roleLabel = role === "ADMIN" ? "관리자" : "일반 사용자";

    const ok = await askConfirm(
      `${member.nickname}(${member.email}) 님을 ${roleLabel}(으)로 변경할까요?`,
    );
    if (!ok) return;

    try {
      setActionEmail(member.email);
      await changeMemberRole(member.email, role);
      fetchList(queryParam);
    } catch (err) {
      console.error(err);
      showAlert("권한 변경 중 오류가 발생했습니다.");
    } finally {
      setActionEmail(null);
    }
  };

  const handleManagerSearch = (e) => {
    e.preventDefault();
    setManagerKeyword(managerKeywordInput);
  };

  const handleManagerKeywordChange = (e) => {
    const value = e.target.value;
    setManagerKeywordInput(value);
    if (value === "") setManagerKeyword("");
  };

  const managerCategoryOptions = [
    ...new Set(managerList.map((c) => c.category).filter(Boolean)),
  ];

  const filteredManagerList = managerList.filter((c) => {
    const lowered = managerKeyword.toLowerCase();
    const matchesKeyword =
      !managerKeyword ||
      [c.name, c.category, c.managerEmail].some((v) =>
        String(v || "").toLowerCase().includes(lowered),
      );
    const matchesCategory = !managerCategory || c.category === managerCategory;
    return matchesKeyword && matchesCategory;
  });

  const handleAdminSearch = (e) => {
    e.preventDefault();
    setAdminKeyword(adminKeywordInput);
  };

  const handleAdminKeywordChange = (e) => {
    const value = e.target.value;
    setAdminKeywordInput(value);
    if (value === "") setAdminKeyword("");
  };

  const filteredAdminList = adminList.filter((m) => {
    if (!adminKeyword) return true;
    const lowered = adminKeyword.toLowerCase();
    return (
      String(m.nickname || "").toLowerCase().includes(lowered) ||
      String(m.email || "").toLowerCase().includes(lowered)
    );
  });

  return (
    <AdminLayout>
      <div className="mb-6">
        <ShopTapeLabel className="mb-2.5">관리자</ShopTapeLabel>
        <p className="font-['Gowun_Batang'] text-2xl text-ink">회원 관리</p>
        <p className="mt-1 text-sm text-ink-faint">
          {activeTab === "managers"
            ? `전체 담당자 ${filteredManagerList.length}명 · 업체명/카테고리/이메일 검색과 카테고리 필터로 좁혀볼 수 있어요.`
            : activeTab === "admins"
              ? `전체 관리자 ${filteredAdminList.length}명 · 닉네임/이메일 검색으로 좁혀볼 수 있어요.`
              : `전체 회원 ${serverData.totalCount}명 · 닉네임/이메일 검색과 상태 필터로 좁혀볼 수 있어요.`}
        </p>
      </div>

      {error ? (
        <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      ) : null}

      {/* 회원 목록 / 담당자 목록 / 관리자 목록 탭 */}
      <div className="flex flex-wrap gap-2 mb-5">
        {MEMBER_TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`h-9 px-4 rounded-full text-xs border transition ${
              activeTab === tab.key
                ? "border-brand bg-brand text-white"
                : "border-line bg-white text-ink-muted hover:border-brand hover:text-brand-deep"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "admins" ? (
        <>
          {/* 검색 */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-5">
            <form onSubmit={handleAdminSearch} className="flex gap-2">
              <input
                type="text"
                value={adminKeywordInput}
                onChange={handleAdminKeywordChange}
                placeholder="닉네임 또는 이메일 검색"
                className="h-9 px-4 border border-line-soft rounded-full text-sm w-64 focus:outline-none focus:border-brand"
              />
              <button
                type="submit"
                className="h-9 px-4 rounded-full bg-cream text-ink-soft text-sm hover:bg-blush-100 transition"
              >
                검색
              </button>
            </form>
          </div>

          <div className="bg-white rounded-2xl shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[600px] text-left text-sm">
              <thead>
                <tr className="text-ink-faint text-xs">
                  <th className="py-3 px-4">닉네임</th>
                  <th className="py-3 px-4">이메일</th>
                  <th className="py-3 px-4">가입일</th>
                  <th className="py-3 px-4">최근 로그인</th>
                  <th className="py-3 px-4">관리</th>
                </tr>
              </thead>
              <tbody>
                {adminListLoading ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="py-8 px-4 text-center text-ink-faint"
                    >
                      불러오는 중...
                    </td>
                  </tr>
                ) : adminList.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="py-8 px-4 text-center text-ink-faint"
                    >
                      관리자 계정이 없습니다.
                    </td>
                  </tr>
                ) : filteredAdminList.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="py-8 px-4 text-center text-ink-faint"
                    >
                      검색 결과가 없습니다.
                    </td>
                  </tr>
                ) : (
                  filteredAdminList.map((member) => (
                    <tr
                      key={member.email}
                      className="border-t border-line hover:bg-cream transition"
                    >
                      <td className="py-2.5 px-4 font-medium text-ink">
                        {member.nickname}
                      </td>
                      <td className="py-2.5 px-4 text-ink-muted">
                        {member.email}
                      </td>
                      <td className="py-2.5 px-4 text-ink-muted">
                        {formatDate(member.regDate)}
                      </td>
                      <td className="py-2.5 px-4 text-ink-muted">
                        {formatDate(member.lastLoginAt)}
                      </td>
                      <td className="py-2.5 px-4">
                        {member.email === currentEmail ? (
                          <span className="text-xs text-ink-faint">
                            본인 계정이에요
                          </span>
                        ) : (
                          <button
                            disabled={actionEmail === member.email}
                            onClick={async () => {
                              await handleRoleChange(member, "USER");
                              fetchAdminList();
                            }}
                            className="text-xs text-red-600 underline disabled:opacity-50"
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
          </div>
        </>
      ) : activeTab === "managers" ? (
        <>
          {/* 검색 + 카테고리 필터 */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <form onSubmit={handleManagerSearch} className="flex gap-2">
                <input
                  type="text"
                  value={managerKeywordInput}
                  onChange={handleManagerKeywordChange}
                  placeholder="업체명 또는 담당자 이메일 검색"
                  className="h-9 px-4 border border-line-soft rounded-full text-sm w-64 focus:outline-none focus:border-brand"
                />
                <button
                  type="submit"
                  className="h-9 px-4 rounded-full bg-cream text-ink-soft text-sm hover:bg-blush-100 transition"
                >
                  검색
                </button>
              </form>

              {managerCategoryOptions.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => setManagerCategory("")}
                    className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition ${
                      managerCategory === ""
                        ? "bg-brand text-white"
                        : "bg-cream text-ink-soft hover:bg-blush-100"
                    }`}
                  >
                    전체
                  </button>
                  {managerCategoryOptions.map((cat) => (
                    <button
                      key={cat}
                      onClick={() => setManagerCategory(cat)}
                      className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition ${
                        managerCategory === cat
                          ? "bg-brand text-white"
                          : "bg-cream text-ink-soft hover:bg-blush-100"
                      }`}
                    >
                      {cat}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

        <div className="bg-white rounded-2xl shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[600px] text-left text-sm">
              <thead>
                <tr className="text-ink-faint text-xs">
                  <th className="py-3 px-4">업체명</th>
                  <th className="py-3 px-4">카테고리</th>
                  <th className="py-3 px-4">담당자 이메일</th>
                  <th className="py-3 px-4">관리</th>
                </tr>
              </thead>
              <tbody>
                {managerListLoading ? (
                  <tr>
                    <td
                      colSpan={4}
                      className="py-8 px-4 text-center text-ink-faint"
                    >
                      불러오는 중...
                    </td>
                  </tr>
                ) : managerList.length === 0 ? (
                  <tr>
                    <td
                      colSpan={4}
                      className="py-8 px-4 text-center text-ink-faint"
                    >
                      아직 지정된 담당자가 없습니다.
                    </td>
                  </tr>
                ) : filteredManagerList.length === 0 ? (
                  <tr>
                    <td
                      colSpan={4}
                      className="py-8 px-4 text-center text-ink-faint"
                    >
                      검색 결과가 없습니다.
                    </td>
                  </tr>
                ) : (
                  filteredManagerList.map((company) => (
                    <tr
                      key={company.cmno}
                      className="border-t border-line hover:bg-cream transition"
                    >
                      <td className="py-2.5 px-4 font-medium text-ink">
                        {company.name}
                      </td>
                      <td className="py-2.5 px-4 text-ink-muted">
                        {company.category}
                      </td>
                      <td className="py-2.5 px-4 text-ink-muted">
                        {company.managerEmail}
                      </td>
                      <td className="py-2.5 px-4">
                        <button
                          onClick={() => handleUnassignFromList(company)}
                          className="text-xs text-red-600 underline"
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
        </div>
        </>
      ) : (
        <>
          {/* 검색 + 필터 */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <form onSubmit={handleSearch} className="flex gap-2">
                <input
                  type="text"
                  value={keywordInput}
                  onChange={handleKeywordChange}
                  placeholder="닉네임 또는 이메일 검색"
                  className="h-9 px-4 border border-line-soft rounded-full text-sm w-64 focus:outline-none focus:border-brand"
                />
                <button
                  type="submit"
                  className="h-9 px-4 rounded-full bg-cream text-ink-soft text-sm hover:bg-blush-100 transition"
                >
                  검색
                </button>
              </form>

              <div className="flex flex-wrap gap-2">
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
                    className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition ${
                      queryParam.status === opt.value
                        ? "bg-brand text-white"
                        : "bg-cream text-ink-soft hover:bg-blush-100"
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 테이블 */}
          <div className="bg-white rounded-2xl shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[820px] text-left text-sm">
                <thead>
                  <tr className="text-ink-faint text-xs">
                    <th className="py-3 px-4">닉네임</th>
                    <th className="py-3 px-4">이메일</th>
                    <th className="py-3 px-4">권한</th>
                    <th className="py-3 px-4">가입일</th>
                    <th className="py-3 px-4">최근 로그인</th>
                    <th className="py-3 px-4">상태</th>
                    <th className="py-3 px-4">관리</th>
                  </tr>
                </thead>
                <tbody>
                  {fetching ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="py-8 px-4 text-center text-ink-faint"
                      >
                        불러오는 중...
                      </td>
                    </tr>
                  ) : serverData.dtoList.length === 0 ? (
                    <tr>
                      <td
                        colSpan={7}
                        className="py-8 px-4 text-center text-ink-faint"
                      >
                        조건에 맞는 회원이 없습니다.
                      </td>
                    </tr>
                  ) : (
                    serverData.dtoList.map((member) => (
                      <tr
                        key={member.email}
                        className="border-t border-line hover:bg-cream transition"
                      >
                        <td className="py-2.5 px-4 font-medium text-ink">
                          {member.nickname}
                        </td>
                        <td className="py-2.5 px-4 text-ink-muted">
                          {member.email}
                        </td>
                        <td className="py-2.5 px-4">
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
                            className="h-8 px-2.5 border border-line-soft rounded-full text-xs text-ink-muted disabled:opacity-50"
                          >
                            <option value="USER">일반 사용자</option>
                            <option value="ADMIN">관리자</option>
                            <option value="MANAGER">업체 담당자</option>
                          </select>
                        </td>
                        <td className="py-2.5 px-4 text-ink-muted">
                          {formatDate(member.regDate)}
                        </td>
                        <td className="py-2.5 px-4 text-ink-muted">
                          {formatDate(member.lastLoginAt)}
                        </td>
                        <td className="py-2.5 px-4">
                          <span
                            className={`px-2.5 py-1 rounded-full text-xs font-medium ${statusColor[member.status]}`}
                          >
                            {statusLabel[member.status] || member.status}
                          </span>
                          {member.status === "BLACKLIST" &&
                          member.suspendReason ? (
                            <div className="mt-1 text-xs text-ink-faint">
                              {member.suspendReason}
                              {member.suspendUntil
                                ? ` · ~${formatDate(member.suspendUntil)}`
                                : " · 영구정지"}
                            </div>
                          ) : null}
                        </td>
                        <td className="py-2.5 px-4">
                          {member.admin ? (
                            <span className="text-xs text-ink-faint">
                              관리자 계정은 상태를 변경할 수 없어요
                            </span>
                          ) : member.status === "WITHDRAWN" ? (
                            <span className="text-xs text-ink-faint">
                              탈퇴한 회원이에요
                            </span>
                          ) : (
                            <div className="flex flex-wrap gap-1.5">
                              {member.status !== "BLACKLIST" && (
                                <button
                                  disabled={actionEmail === member.email}
                                  onClick={() => openSuspendModal(member)}
                                  className="rounded-full bg-red-50 px-2.5 py-1 text-xs font-medium text-red-600 hover:bg-red-100 disabled:opacity-50"
                                >
                                  정지
                                </button>
                              )}
                              {member.status === "BLACKLIST" && (
                                <button
                                  disabled={actionEmail === member.email}
                                  onClick={() => handleReactivate(member)}
                                  className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100 disabled:opacity-50"
                                >
                                  정지 해제
                                </button>
                              )}
                              {member.status === "DORMANT" && (
                                <button
                                  disabled={actionEmail === member.email}
                                  onClick={() => handleReactivate(member)}
                                  className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-100 disabled:opacity-50"
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
          </div>

          <PageComponent serverData={serverData} movePage={movePage} />
        </>
      )}

      {/* 정지 처리 모달 */}
      {suspendTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-[0_20px_60px_-15px_rgba(58,54,47,0.35)]">
            <h3 className="mb-1 text-lg font-['Gowun_Batang'] text-ink">
              활동 정지 처리
            </h3>
            <p className="mb-4 text-sm text-ink-faint">
              {suspendTarget.nickname} ({suspendTarget.email})
            </p>

            <label className="mb-1 block text-xs font-medium text-ink-soft">
              정지 사유 *
            </label>
            <textarea
              value={suspendReason}
              onChange={(e) => setSuspendReason(e.target.value)}
              rows={3}
              placeholder="신고 누적, 이용약관 위반 등"
              className="mb-3 w-full rounded-xl border border-line-soft px-3 py-2 text-sm outline-none focus:border-brand"
            />

            <label className="mb-1.5 block text-xs font-medium text-ink-soft">
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
                      : "bg-cream text-ink-soft hover:bg-blush-100"
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>

            <div className="flex justify-end gap-2">
              <button
                onClick={closeSuspendModal}
                className="rounded-full px-4 py-2 text-sm font-medium text-ink-muted hover:bg-surface transition"
              >
                취소
              </button>
              <button
                onClick={confirmSuspend}
                className="rounded-full bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition"
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
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-[0_20px_60px_-15px_rgba(58,54,47,0.35)]">
            <h3 className="mb-1 text-base font-['Gowun_Batang'] text-ink">
              업체 담당자 임명
            </h3>
            <p className="mb-4 text-xs text-ink-faint">
              {managerTarget.email} 님을 업체 문의 답변 담당자로 지정합니다.
            </p>

            {managerCurrent && (
              <div className="mb-4 rounded-xl bg-brand-light px-3 py-2 text-xs text-brand-deep">
                현재 <strong>{managerCurrent.name}</strong>의 담당자로 지정되어
                있어요.
              </div>
            )}

            <label className="mb-1.5 block text-xs font-medium text-ink-soft">
              담당할 업체 선택
            </label>
            <select
              value={selectedCmno}
              onChange={(e) => setSelectedCmno(e.target.value)}
              disabled={managerLoading}
              className="mb-4 w-full rounded-full border border-line-soft px-3 py-2 text-sm outline-none focus:border-brand"
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
                    className="rounded-full bg-red-50 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-100 disabled:opacity-50"
                  >
                    담당 해제
                  </button>
                )}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={closeManagerModal}
                  className="rounded-full px-4 py-2 text-sm font-medium text-ink-muted hover:bg-surface transition"
                >
                  취소
                </button>
                <button
                  onClick={confirmAssignManager}
                  disabled={managerLoading}
                  className="rounded-full bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-50 transition"
                >
                  임명하기
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {/* 확인 모달 (window.confirm 대체) */}
      {confirmState ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-[0_20px_60px_-15px_rgba(58,54,47,0.35)]">
            <p className="mb-5 text-sm text-ink whitespace-pre-wrap leading-relaxed">
              {confirmState.message}
            </p>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => closeConfirm(false)}
                className="rounded-full px-4 py-2 text-sm font-medium text-ink-muted hover:bg-surface transition"
              >
                취소
              </button>
              <button
                onClick={() => closeConfirm(true)}
                className="rounded-full bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark transition"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </AdminLayout>
  );
};

export default MemberManageComponent;
