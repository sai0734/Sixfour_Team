import { useEffect, useMemo, useRef, useState } from "react";
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { useSelector } from "react-redux";
import {
  deleteOne,
  getCompanyImageUrl,
  getOne,
  updateMakeupDetail,
  updateDressDetail,
  uploadCompanyImages,
} from "../../api/companyApi";
import {
  checkCompanyWish,
  addCompanyWish,
  removeCompanyWish,
} from "../../api/companywishApi";
import CompanyWishOptionModal from "../companywish/CompanyWishOptionModal";
import { buildCompanyOptions } from "../../util/companyOptionBuilder";
import FetchingModal from "../common/FetchingModal";
import KakaoMap from "../common/KakaoMap";
import useCustomLogin from "../../hooks/useCustomLogin";
import useManagedCompany from "../../hooks/useManagedCompany";
import useInquiryChat from "../../context/InquiryChatContext";

const initState = {
  cmno: 0,
  category: "",
  name: "",
  ceoName: "",
  phone: "",
  address: "",
  latitude: "",
  longitude: "",
  description: "",
  priceAvg: "",
  uploadFileNames: [],
};

const categoryLabel = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

const packageTypeLabel = {
  HAIR: "헤어 패키지",
  MAKEUP: "메이크업 패키지",
  NAIL: "네일 패키지",
  HAIR_MAKEUP: "헤어+메이크업 패키지",
  HAIR_NAIL: "헤어+네일 패키지",
  MAKEUP_NAIL: "메이크업+네일 패키지",
  FULL: "풀 패키지",
};

const mealTypeLabel = {
  KOREAN: "한식",
  WESTERN: "양식",
  BUFFET: "뷔페",
  FUSION: "퓨전",
  COURSE: "코스",
  BOTH: "뷔페/코스",
};

const hallTypeLabel = {
  GRAND: "그랜드홀",
  HOTEL: "호텔",
  HOUSE: "하우스",
  CONVENTION: "컨벤션",
  CHAPEL: "채플",
  GARDEN: "가든",
  BANQUET: "연회장",
};

const adminRoles = ["ADMIN", "ROLE_ADMIN"];

const CompanyReadComponent = () => {
  const { cmno } = useParams();
  const [queryParams] = useSearchParams();
  const listSearch = (() => {
    const page = queryParams.get("page");
    const size = queryParams.get("size");
    const params = new URLSearchParams();
    if (page) params.set("page", page);
    if (size) params.set("size", size);
    return params.toString() ? `?${params.toString()}` : "";
  })();
  const [company, setCompany] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const { exceptionHandle } = useCustomLogin();
  const navigate = useNavigate();
  const location = useLocation();
  const loginState = useSelector((state) => state.loginSlice);
  const canManageCompany =
    loginState.roleNames?.some((roleName) => adminRoles.includes(roleName)) &&
    !company._isDummyOnly;
  const { isManager, company: managedCompany } = useManagedCompany({
    enabled: Boolean(loginState.email),
  });
  const isManagedByMe =
    isManager &&
    managedCompany?.cmno != null &&
    Number(managedCompany.cmno) === Number(company.cmno);
  // 관리자 경로에서 상세로 들어온 경우 목록/수정 이동도 관리자 경로로 유지합니다.
  const companyPathPrefix = location.pathname.startsWith("/admin/companies")
    ? "/admin/companies"
    : "/companies";

  // ── 스크롤 & 탭 ──
  const [activeSection, setActiveSection] = useState("intro");
  const [headerHeight, setHeaderHeight] = useState(64);
  const sectionRefs = useRef({});

  useEffect(() => {
    const el = document.getElementById("mainNav");
    if (!el) return;
    const update = () => setHeaderHeight(el.getBoundingClientRect().height);
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const scrollToSection = (key) => {
    setActiveSection(key);
    const el = sectionRefs.current[key];
    if (el) {
      const top =
        el.getBoundingClientRect().top + window.scrollY - (headerHeight + 8);
      window.scrollTo({ top, behavior: "smooth" });
    }
  };

  useEffect(() => {
    setFetching(true);
    getOne(cmno)
      .then((data) => {
        setCompany({ ...initState, ...data });
        setFetching(false);
      })
      .catch((err) => {
        setFetching(false);
        exceptionHandle(err);
      });
  }, [cmno]);

  const handleDelete = async () => {
    if (!window.confirm(`${company.name} 업체를 삭제하시겠습니까?`)) {
      return;
    }

    try {
      setFetching(true);
      await deleteOne(cmno);
      navigate({ pathname: `${companyPathPrefix}/list` });
    } catch (err) {
      exceptionHandle(err);
    } finally {
      setFetching(false);
    }
  };

  const handleRefresh = () => {
    setFetching(true);
    getOne(cmno)
      .then((data) => {
        setCompany({ ...initState, ...data });
        setFetching(false);
      })
      .catch((err) => {
        setFetching(false);
        exceptionHandle(err);
      });
  };

  // ── 찜 상태 ──
  const isLoggedIn = Boolean(loginState?.email || loginState?.accessToken);
  const [liked, setLiked] = useState(false);
  const [favoriteLoading, setFavoriteLoading] = useState(false);
  // 재원 추가 - 홀/드레스/메이크업처럼 옵션이 있는 업체는 찜하기 전에 옵션을 고르게 함
  const [wishModalOpen, setWishModalOpen] = useState(false);
  const { startInquiry } = useInquiryChat();
  const wishOptions = useMemo(() => buildCompanyOptions(company), [company]);

  useEffect(() => {
    if (!cmno || !isLoggedIn) {
      setLiked(false);
      return;
    }
    checkCompanyWish(cmno)
      .then((data) => {
        const nextLiked =
          typeof data === "boolean" ? data : Boolean(data?.liked);
        setLiked(nextLiked);
      })
      .catch((err) => {
        console.error("업체 찜 여부 조회 실패:", err);
        setLiked(false);
      });
  }, [cmno, isLoggedIn]);

  const handleFavoriteClick = async () => {
    if (!company.cmno || favoriteLoading) return;

    if (!isLoggedIn) {
      alert("로그인 후 찜하기를 이용할 수 있습니다.");
      navigate("/auth/login");
      return;
    }

    // 옵션이 있는 업체(홀/드레스/메이크업)는 어떤 옵션에 관심있는지 고르고 찜하도록
    // 모달을 띄움. 여러 옵션을 각각 찜해둘 수 있어서, 이미 찜한 상태라도 다시 누르면
    // 다른 옵션을 추가로 찜할 수 있게 함. 특정 옵션만 찜 해제하는 건 마이페이지에서.
    if (wishOptions.length > 0) {
      setWishModalOpen(true);
      return;
    }

    try {
      setFavoriteLoading(true);
      if (liked) {
        await removeCompanyWish(company.cmno);
        setLiked(false);
      } else {
        await addCompanyWish(company.cmno);
        setLiked(true);
      }
    } catch (err) {
      console.error("업체 찜 처리 실패:", err);
      exceptionHandle(err);
    } finally {
      setFavoriteLoading(false);
    }
  };

  // 용현 추가 문의하기 클릭 — 로그인 확인 후 채팅 시작
  const handleInquiryClick = () => {
    if (!loginState.email) {
      alert("로그인이 필요한 기능입니다.");
      navigate("/auth/login");
      return;
    }
    startInquiry(company.cmno, company.name);
  };

  const handleWishOptionSubmit = async (option) => {
    try {
      setFavoriteLoading(true);
      await addCompanyWish(
        company.cmno,
        option.label,
        option.price,
        option.image,
      );
      setLiked(true);
      setWishModalOpen(false);
      alert(`"${option.label}" 옵션으로 찜했습니다.`);
    } catch (err) {
      console.error("업체 찜 처리 실패:", err);
      exceptionHandle(err);
    } finally {
      setFavoriteLoading(false);
    }
  };

  const mainImage = company.uploadFileNames?.[0];

  const detailImageClassByCategory = {
    DRESS: "h-[260px] sm:h-[400px] lg:h-[520px] w-full",
    MAKEUP: "h-[220px] sm:h-[320px] lg:h-[420px] w-full",
    STUDIO: "h-[240px] sm:h-[360px] lg:h-[460px] w-full",
  };

  const thumbnailImageClassByCategory = {
    DRESS: "h-28 sm:h-40 w-full",
    MAKEUP: "h-32 sm:h-52 w-full",
    STUDIO: "h-28 sm:h-40 w-full",
  };

  const getDetailImageClass = (category) =>
    detailImageClassByCategory[category] || "h-80 w-full";
  const getThumbnailImageClass = (category) =>
    thumbnailImageClassByCategory[category] || "h-28 w-full";

  return (
    <div className="bg-white text-ink pb-16">
      {fetching ? <FetchingModal /> : null}

      {/* ── 브레드크럼 ── */}
      <p className="mb-5 text-xs text-ink-faint">
        {companyPathPrefix === "/admin/companies" ? "관리자" : "홀/스드메"}
        {" > "}
        {categoryLabel[company.category] || company.category || "업체"}
        {" > "}
        <span className="text-ink-soft">{company.name}</span>
      </p>

      {/* ── 메인 그리드: 갤러리 + 정보 패널 ── */}
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-[420px_1fr] lg:gap-12">
        {/* 이미지 갤러리 */}
        <div>
          {mainImage ? (
            <img
              className={`${getDetailImageClass(company.category)} w-full object-cover rounded-2xl`}
              src={getCompanyImageUrl(mainImage)}
              alt={company.name}
            />
          ) : (
            <div className="flex h-48 sm:h-72 items-center justify-center rounded-2xl bg-blush-50 text-ink-muted text-sm">
              대표 이미지가 없습니다.
            </div>
          )}
          {(company.uploadFileNames || []).length > 1 && (
            <div className="mt-2.5 grid grid-cols-3 gap-2">
              {(company.uploadFileNames || []).slice(1, 7).map((fileName) => (
                <img
                  key={fileName}
                  className={`${getThumbnailImageClass(company.category)} w-full object-cover rounded-xl`}
                  src={getCompanyImageUrl(fileName, true)}
                  alt={company.name}
                />
              ))}
            </div>
          )}
        </div>

        {/* 정보 패널 */}
        <div className="flex flex-col">
          {/* 카테고리 태그 */}
          <span className="inline-block -rotate-2 bg-blush-100 px-3 py-1 mb-3 font-['Gaegu'] text-[13px] text-brand-deep w-fit">
            {categoryLabel[company.category] || company.category}
          </span>

          {/* 업체명 */}
          <p className="font-['Gowun_Batang'] text-2xl sm:text-3xl mb-4 leading-snug">
            {company.name || "업체명 로딩 중..."}
          </p>

          {/* 기본 정보 */}
          <div className="space-y-2.5 border-t border-line pt-4 mb-5 text-sm text-ink-muted">
            {company.address && (
              <div className="flex gap-2 items-start">
                <span className="shrink-0 mt-0.5">📍</span>
                <span className="leading-relaxed">{company.address}</span>
              </div>
            )}
            {company.phone && (
              <div className="flex gap-2">
                <span className="shrink-0">📞</span>
                <span>{company.phone}</span>
              </div>
            )}
            {company.ceoName && (
              <div className="flex gap-2">
                <span className="shrink-0">👤</span>
                <span>대표 {company.ceoName}</span>
              </div>
            )}
          </div>

          {/* 가격 */}
          {company.priceAvg ? (
            <p className="text-2xl font-medium mb-5">
              {Number(company.priceAvg).toLocaleString()}원~
            </p>
          ) : (
            <div className="mb-5" />
          )}

          {/* 액션 버튼 */}
          <div className="space-y-2.5">
            <div className="flex gap-2.5">
              <button
                className={`w-[46px] h-[46px] shrink-0 border rounded-full flex items-center justify-center text-lg transition disabled:cursor-not-allowed disabled:opacity-60 ${
                  liked
                    ? "border-rose-300 bg-rose-50 text-rose-500"
                    : "border-line bg-white text-brand hover:bg-blush-50"
                }`}
                type="button"
                title={liked ? "찜 해제" : "찜하기"}
                aria-label={
                  liked ? `${company.name} 찜 해제` : `${company.name} 찜하기`
                }
                aria-pressed={liked}
                onClick={handleFavoriteClick}
                disabled={favoriteLoading}
              >
                {favoriteLoading ? "…" : liked ? "♥" : "♡"}
              </button>
              {/* 승진 코드 추가 - 예약하기 / 결제하기 분리 */}
              <button
                className="flex-1 h-[46px] rounded-full border border-line bg-white text-sm font-medium transition hover:border-brand hover:text-brand"
                type="button"
                onClick={() => {
                  if (!loginState.email) {
                    alert("로그인이 필요한 기능입니다.");
                    navigate("/auth/login");
                    return;
                  }
                  navigate(`/companies/reserve/${company.cmno}?mode=reserve`);
                }}
              >
                예약하기
              </button>
              <button
                className="flex-1 h-[46px] rounded-full bg-brand text-white text-sm font-medium transition hover:bg-brand-deep"
                type="button"
                onClick={() => {
                  if (!loginState.email) {
                    alert("로그인이 필요한 기능입니다.");
                    navigate("/auth/login");
                    return;
                  }
                  navigate(`/companies/reserve/${company.cmno}?mode=pay`);
                }}
              >
                결제하기
              </button>
              {/* 승진 코드 추가 끝 */}
            </div>

            {(isManagedByMe ||
              (!canManageCompany && !isManagedByMe) ||
              canManageCompany) && (
              <div className="flex gap-2.5">
                {isManagedByMe && (
                  <button
                    className="flex-1 h-[46px] rounded-full border border-brand bg-brand text-sm font-medium text-white transition hover:opacity-90"
                    type="button"
                    onClick={() => navigate("/manager/inquiries")}
                  >
                    업체페이지
                  </button>
                )}
                {!canManageCompany && !isManagedByMe && (
                  <button
                    className="flex-1 h-[46px] rounded-full border border-brand bg-brand text-sm font-medium text-white transition hover:opacity-90"
                    type="button"
                    onClick={handleInquiryClick}
                  >
                    문의하기
                  </button>
                )}
                {canManageCompany && (
                  <>
                    <button
                      className="flex-1 h-[46px] rounded-full border border-brand text-brand text-sm font-medium transition hover:bg-brand hover:text-white"
                      type="button"
                      onClick={() =>
                        navigate({
                          pathname: `${companyPathPrefix}/modify/${company.cmno}`,
                        })
                      }
                    >
                      수정
                    </button>
                    <button
                      className="flex-1 h-[46px] rounded-full border border-red-300 text-red-500 text-sm font-medium transition hover:bg-red-50"
                      type="button"
                      onClick={handleDelete}
                    >
                      삭제
                    </button>
                  </>
                )}
              </div>
            )}
          </div>

          {canManageCompany && (
            <p className="mt-3 text-xs text-ink-faint">
              업체 번호 #{company.cmno}
            </p>
          )}

          <KakaoMap
            latitude={company.latitude}
            longitude={company.longitude}
            name={company.name}
            address={company.address}
          />
        </div>
      </div>

      {wishModalOpen && (
        <CompanyWishOptionModal
          companyName={company.name}
          options={wishOptions}
          onSubmit={handleWishOptionSubmit}
          onClose={() => setWishModalOpen(false)}
        />
      )}

      {/* ── 탭 네비게이션 ── */}
      <div
        className="sticky z-10 bg-white border-b border-line mt-10"
        style={{ top: `${headerHeight}px` }}
      >
        <div className="flex gap-5 md:gap-7 text-sm overflow-x-auto">
          {[
            { key: "intro", label: "업체 소개" },
            ...(company.category
              ? [
                  {
                    key: "detail",
                    label: categoryLabel[company.category] || "상세 정보",
                  },
                ]
              : []),
          ].map((tab) => (
            <span
              key={tab.key}
              onClick={() => scrollToSection(tab.key)}
              className={`py-3.5 cursor-pointer border-b-2 whitespace-nowrap transition ${
                activeSection === tab.key
                  ? "text-ink font-medium border-brand"
                  : "text-ink-faint border-transparent hover:text-ink-soft"
              }`}
            >
              {tab.label}
            </span>
          ))}
        </div>
      </div>

      {/* ── 업체 소개 섹션 ── */}
      <div
        ref={(el) => (sectionRefs.current.intro = el)}
        className="py-8 md:py-10 border-b border-line"
      >
        <p className="text-sm font-medium mb-4">업체 소개</p>
        <p className="whitespace-pre-wrap text-sm leading-7 text-ink-muted">
          {company.description || "등록된 소개가 없습니다."}
        </p>
      </div>

      {/* ── 카테고리 상세 섹션 ── */}
      <div
        ref={(el) => (sectionRefs.current.detail = el)}
        className="py-6 md:py-8"
      >
        <CategoryDetail
          company={company}
          canManageCompany={canManageCompany}
          onRefresh={handleRefresh}
        />
      </div>

      {/* ── 목록으로 ── */}
      <div className="flex justify-end pt-4">
        <button
          onClick={() => navigate(`${companyPathPrefix}/list${listSearch}`)}
          className="h-11 px-6 rounded-full border border-line bg-white text-sm transition hover:border-brand hover:text-brand"
        >
          목록으로
        </button>
      </div>
    </div>
  );
};

const CategoryDetail = ({ company, canManageCompany, onRefresh }) => {
  if (company.category === "MAKEUP") {
    return (
      <MakeupDetail
        detail={company.makeupDetail}
        cmno={company.cmno}
        canManageCompany={canManageCompany}
        onRefresh={onRefresh}
      />
    );
  }
  if (company.category === "DRESS") {
    return (
      <DressDetail
        detail={company.dressDetail}
        cmno={company.cmno}
        canManageCompany={canManageCompany}
        onRefresh={onRefresh}
      />
    );
  }
  if (company.category === "HALL") {
    return <HallDetail detail={company.hallDetail} />;
  }
  if (company.category === "STUDIO") {
    return <StudioDetail detail={company.studioDetail} />;
  }
  return null;
};

/* ── 수정용 입력 컴포넌트 (MakeupDetail 보다 먼저 선언) ── */
const EditPriceRow = ({ label, value, onChange }) => (
  <div className="border-b border-slate-100 py-3 last:border-b-0">
    <div className="text-xs font-medium uppercase text-slate-400 mb-1">
      {label}
    </div>
    <div className="flex items-center gap-1">
      <input
        type="number"
        min="0"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-md border border-slate-200 px-2 py-1 text-sm text-slate-800 focus:border-rose-300 focus:outline-none"
        placeholder="금액 입력"
      />
      <span className="text-sm text-slate-500 shrink-0">원</span>
    </div>
  </div>
);

const EditDiscountRow = ({ label, value, onChange }) => (
  <div className="border-b border-slate-100 py-3 last:border-b-0">
    <div className="text-xs font-medium uppercase text-slate-400 mb-1">
      {label}
    </div>
    <div className="flex items-center gap-1">
      <input
        type="number"
        min="0"
        max="100"
        step="0.1"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-md border border-slate-200 px-2 py-1 text-sm text-slate-800 focus:border-rose-300 focus:outline-none"
        placeholder="할인율 입력"
      />
      <span className="text-sm text-slate-500 shrink-0">%</span>
    </div>
  </div>
);

const PACKAGE_TYPE_OPTIONS = [
  { value: "HAIR", label: "헤어", keys: ["hair"] },
  { value: "MAKEUP", label: "메이크업", keys: ["makeup"] },
  { value: "NAIL", label: "네일", keys: ["nail"] },
  { value: "HAIR_MAKEUP", label: "헤어+메이크업", keys: ["hair", "makeup"] },
  { value: "HAIR_NAIL", label: "헤어+네일", keys: ["hair", "nail"] },
  { value: "MAKEUP_NAIL", label: "메이크업+네일", keys: ["makeup", "nail"] },
  {
    value: "FULL",
    label: "풀패키지 (헤어+메이크업+네일)",
    keys: ["hair", "makeup", "nail"],
  },
];

const MakeupDetail = ({ detail, cmno, canManageCompany, onRefresh }) => {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(null);

  if (!detail) return null;
  const packages = (detail.packages || []).filter(Boolean);

  const startEdit = () => {
    setForm({
      hairPrice: detail.hairPrice ?? "",
      makeupPrice: detail.makeupPrice ?? "",
      nailPrice: detail.nailPrice ?? "",
      includesHairService: detail.includesHairService ?? true,
      includesMakeupService: detail.includesMakeupService ?? true,
      includesNailService: detail.includesNailService ?? false,
      packages: packages.map((p) => ({
        packageId: p.packageId,
        packageType: p.packageType,
        discountRate: p.discountRate ?? "",
      })),
    });
    setEditing(true);
  };

  const cancelEdit = () => {
    setEditing(false);
    setForm(null);
  };

  const handlePriceChange = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handlePackageTypeChange = (index, value) => {
    setForm((prev) => {
      const updated = [...prev.packages];
      updated[index] = { ...updated[index], packageType: value };
      return { ...prev, packages: updated };
    });
  };

  const handlePackageRateChange = (index, value) => {
    setForm((prev) => {
      const updated = [...prev.packages];
      updated[index] = { ...updated[index], discountRate: value };
      return { ...prev, packages: updated };
    });
  };

  const handleAddPackage = () => {
    setForm((prev) => ({
      ...prev,
      packages: [
        ...prev.packages,
        { packageId: null, packageType: "HAIR_MAKEUP", discountRate: "" },
      ],
    }));
  };

  const handleRemovePackage = (index) => {
    setForm((prev) => ({
      ...prev,
      packages: prev.packages.filter((_, i) => i !== index),
    }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await updateMakeupDetail(cmno, {
        cmno,
        hairPrice: form.hairPrice === "" ? null : Number(form.hairPrice),
        makeupPrice: form.makeupPrice === "" ? null : Number(form.makeupPrice),
        nailPrice: form.nailPrice === "" ? null : Number(form.nailPrice),
        includesHairService: form.includesHairService,
        includesMakeupService: form.includesMakeupService,
        includesNailService: form.includesNailService,
        packages: form.packages.map((p) => ({
          packageId: p.packageId,
          cmno,
          packageType: p.packageType,
          discountRate: p.discountRate === "" ? null : Number(p.discountRate),
        })),
      });
      setEditing(false);
      setForm(null);
      onRefresh?.();
    } catch (err) {
      const status = err?.response?.status;
      const serverMsg =
        err?.response?.data?.message || err?.response?.data || "";
      console.error("메이크업 상세 저장 실패:", status, serverMsg, err);
      if (status === 403) {
        alert(
          "권한이 없습니다. 관리자 계정으로 로그인되어 있는지 확인해주세요.",
        );
      } else if (status === 404) {
        alert(
          `저장 실패: 해당 업체의 메이크업 상세 정보를 찾을 수 없습니다. (cmno: ${cmno})`,
        );
      } else if (status === 500) {
        alert(
          `서버 오류가 발생했습니다. 백엔드 로그를 확인해주세요.\n${serverMsg}`,
        );
      } else {
        alert(`저장에 실패했습니다. (${status ?? "네트워크 오류"})`);
      }
    } finally {
      setSaving(false);
    }
  };

  const editAction =
    canManageCompany &&
    (editing ? (
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={cancelEdit}
          className="rounded-md border border-slate-200 px-3 py-1 text-xs text-slate-500 hover:bg-slate-50"
        >
          취소
        </button>
        <button
          type="button"
          onClick={handleSave}
          disabled={saving}
          className="rounded-md bg-rose-500 px-3 py-1 text-xs font-semibold text-white hover:bg-rose-600 disabled:opacity-50"
        >
          {saving ? "저장 중…" : "저장"}
        </button>
      </div>
    ) : (
      <button
        type="button"
        onClick={startEdit}
        className="text-sm font-bold text-rose-400 hover:text-rose-600 transition-colors"
      >
        ✏ 수정
      </button>
    ));

  return (
    <DetailSection title="메이크업 상세" headerAction={editAction}>
      {editing ? (
        <>
          {/* 가격 수정 */}
          <div className="mb-1 text-xs font-semibold text-slate-500">
            가격 수정
          </div>
          <div className="grid gap-3 sm:grid-cols-3 mb-6">
            <EditPriceRow
              label="헤어 가격"
              value={form.hairPrice}
              onChange={(v) => handlePriceChange("hairPrice", v)}
            />
            <EditPriceRow
              label="메이크업 가격"
              value={form.makeupPrice}
              onChange={(v) => handlePriceChange("makeupPrice", v)}
            />
            <EditPriceRow
              label="네일 가격"
              value={form.nailPrice}
              onChange={(v) => handlePriceChange("nailPrice", v)}
            />
          </div>

          {/* 패키지 수정 */}
          <div className="flex items-center justify-between mb-2">
            <div className="text-xs font-semibold text-slate-500">
              패키지 수정
            </div>
            <button
              type="button"
              onClick={handleAddPackage}
              className="flex items-center gap-1 rounded-md border border-rose-200 px-2 py-1 text-xs text-rose-500 hover:bg-rose-50"
            >
              + 패키지 추가
            </button>
          </div>

          {form.packages.length === 0 ? (
            <p className="text-xs text-slate-400 py-2">
              등록된 패키지가 없습니다. 추가 버튼으로 패키지를 추가하세요.
            </p>
          ) : (
            <div className="flex flex-col gap-3">
              {form.packages.map((pkg, index) => (
                <div
                  key={index}
                  className="rounded-lg border border-slate-200 bg-slate-50 p-3"
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-medium text-slate-500">
                      패키지 {index + 1}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleRemovePackage(index)}
                      className="text-xs text-red-400 hover:text-red-600"
                    >
                      ✕ 삭제
                    </button>
                  </div>
                  <div className="grid gap-2 sm:grid-cols-2">
                    {/* 패키지 타입 선택 */}
                    <div>
                      <div className="text-xs text-slate-400 mb-1">
                        패키지 종류
                      </div>
                      <select
                        value={pkg.packageType}
                        onChange={(e) =>
                          handlePackageTypeChange(index, e.target.value)
                        }
                        className="w-full rounded-md border border-slate-200 px-2 py-1.5 text-sm text-slate-800 bg-white focus:border-rose-300 focus:outline-none"
                      >
                        {PACKAGE_TYPE_OPTIONS.map((opt) => (
                          <option key={opt.value} value={opt.value}>
                            {opt.label}
                          </option>
                        ))}
                      </select>
                    </div>
                    {/* 할인율 입력 */}
                    <div>
                      <div className="text-xs text-slate-400 mb-1">
                        할인율 (%)
                      </div>
                      <div className="flex items-center gap-1">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          step="0.1"
                          value={pkg.discountRate}
                          onChange={(e) =>
                            handlePackageRateChange(index, e.target.value)
                          }
                          className="w-full rounded-md border border-slate-200 px-2 py-1.5 text-sm text-slate-800 focus:border-rose-300 focus:outline-none"
                          placeholder="예: 10"
                        />
                        <span className="text-sm text-slate-500 shrink-0">
                          %
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      ) : (
        <>
          <div className="grid gap-3 sm:grid-cols-3">
            <InfoRow label="헤어 가격" value={formatPrice(detail.hairPrice)} />
            <InfoRow
              label="메이크업 가격"
              value={formatPrice(detail.makeupPrice)}
            />
            <InfoRow label="네일 가격" value={formatPrice(detail.nailPrice)} />
          </div>
          {packages.length > 0 && (
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {packages.map((pkg, index) => {
                const opt = PACKAGE_TYPE_OPTIONS.find(
                  (o) => o.value === pkg.packageType,
                );
                const priceMap = {
                  hair: Number(detail.hairPrice || 0),
                  makeup: Number(detail.makeupPrice || 0),
                  nail: Number(detail.nailPrice || 0),
                };
                const basePrice = opt
                  ? opt.keys.reduce((sum, k) => sum + (priceMap[k] || 0), 0)
                  : 0;
                const rate = Number(pkg.discountRate || 0);
                const discountRate = rate > 1 ? rate / 100 : rate;
                const discountedPrice =
                  basePrice > 0 && discountRate > 0
                    ? Math.round(basePrice * (1 - discountRate))
                    : null;
                return (
                  <div
                    key={`${pkg.packageType || "PACKAGE"}-${index}`}
                    className="border-b border-slate-100 py-3 last:border-b-0"
                  >
                    <div className="text-xs font-medium uppercase text-slate-400 mb-1">
                      {packageTypeLabel[pkg.packageType] ||
                        pkg.packageType ||
                        "패키지"}
                    </div>
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-semibold text-rose-500">
                        {discountRate > 0
                          ? `${Math.round(discountRate * 100)}% 할인`
                          : "할인 없음"}
                      </span>
                      {discountedPrice != null && (
                        <span className="text-sm text-slate-700">
                          {basePrice > 0 && (
                            <span className="text-xs text-slate-400 line-through mr-1">
                              {basePrice.toLocaleString()}원
                            </span>
                          )}
                          {discountedPrice.toLocaleString()}원
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
    </DetailSection>
  );
};

const ITEMS_PER_PAGE = 8;

/* ── 드레스/슈트 아이템 뷰 모달 (일반 유저용 크게 보기) ── */
const DressItemViewModal = ({
  item,
  isSuitFn,
  allItems,
  currentIdx,
  onNavigate,
  onClose,
}) => {
  const imageSrc = item.imageUrl ? getCompanyImageUrl(item.imageUrl) : null;
  const isSuitItem = isSuitFn(item);
  const hasPrev = currentIdx > 0;
  const hasNext = currentIdx < allItems.length - 1;

  // 키보드 ← → ESC 지원
  const handleKeyDown = (e) => {
    if (e.key === "ArrowLeft" && hasPrev) onNavigate(currentIdx - 1);
    if (e.key === "ArrowRight" && hasNext) onNavigate(currentIdx + 1);
    if (e.key === "Escape") onClose();
  };

  return (
    /* 스크롤 가능한 오버레이 */
    <div
      className="fixed inset-0 z-50 overflow-y-auto"
      style={{
        backdropFilter: "blur(6px)",
        backgroundColor: "rgba(0,0,0,0.75)",
      }}
      onKeyDown={handleKeyDown}
      tabIndex={-1}
      ref={(el) => el?.focus()}
    >
      {/* 클릭-닫기 + 센터링 래퍼 */}
      <div
        className="flex min-h-full items-center justify-center p-4"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div className="relative flex w-full max-w-3xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl sm:flex-row my-4">
          {/* 닫기 버튼 */}
          <button
            type="button"
            onClick={onClose}
            className="absolute right-3 top-3 z-10 rounded-full bg-black/40 p-1.5 text-white hover:bg-black/60 transition-colors leading-none text-sm"
          >
            ✕
          </button>

          {/* 이미지 영역 */}
          <div className="relative flex items-center justify-center bg-slate-900 sm:w-[55%]">
            {/* 이전 */}
            {hasPrev && (
              <button
                type="button"
                onClick={() => onNavigate(currentIdx - 1)}
                className="absolute left-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 p-2 text-white hover:bg-black/60 transition-colors z-10"
              >
                ◀
              </button>
            )}
            {imageSrc ? (
              <img
                src={imageSrc}
                alt={item.itemName || "드레스 이미지"}
                className="max-h-[45vh] sm:max-h-[70vh] w-full object-contain"
              />
            ) : (
              <div className="flex h-48 sm:h-80 w-full items-center justify-center text-slate-500">
                <span className="text-4xl">👗</span>
              </div>
            )}
            {/* 다음 */}
            {hasNext && (
              <button
                type="button"
                onClick={() => onNavigate(currentIdx + 1)}
                className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 p-2 text-white hover:bg-black/60 transition-colors z-10"
              >
                ▶
              </button>
            )}
            {/* 인덱스 표시 */}
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-black/40 px-3 py-1 text-xs text-white">
              {currentIdx + 1} / {allItems.length}
            </div>
          </div>

          {/* 정보 영역 */}
          <div className="flex flex-1 flex-col justify-between p-6">
            <div>
              <span
                className={`mb-3 inline-block rounded-full px-3 py-1 text-xs font-semibold ${isSuitItem ? "bg-blue-100 text-blue-600" : "bg-rose-100 text-rose-600"}`}
              >
                {isSuitItem ? "슈트" : "드레스"}
              </span>
              <h2 className="mt-2 text-xl font-bold text-slate-800 leading-snug">
                {item.itemName || "이름 없음"}
              </h2>

              {item.price && (
                <div className="mt-4">
                  <p className="text-xs font-medium text-slate-400 uppercase tracking-wide">
                    가격
                  </p>
                  <p className="mt-1 text-2xl font-bold text-rose-500">
                    {Number(item.price).toLocaleString()}원
                  </p>
                </div>
              )}

              {item.sizeRange && (
                <div className="mt-4">
                  <p className="text-xs font-medium text-slate-400 uppercase tracking-wide">
                    보유 사이즈
                  </p>
                  <p className="mt-1 text-sm text-slate-700">
                    {item.sizeRange}
                  </p>
                </div>
              )}

              {item.styleTags && (
                <div className="mt-4">
                  <p className="text-xs font-medium text-slate-400 uppercase tracking-wide">
                    스타일 태그
                  </p>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {String(item.styleTags)
                      .split(",")
                      .map((tag) => tag.trim())
                      .filter(Boolean)
                      .map((tag) => (
                        <span
                          key={tag}
                          className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs text-slate-600"
                        >
                          {tag}
                        </span>
                      ))}
                  </div>
                </div>
              )}
            </div>

            {/* 하단 네비게이션 힌트 */}
            <div className="mt-6 flex items-center justify-between">
              <button
                type="button"
                disabled={!hasPrev}
                onClick={() => onNavigate(currentIdx - 1)}
                className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30 transition-colors"
              >
                ◀ 이전
              </button>
              <span className="text-xs text-slate-400">← → 키로 이동</span>
              <button
                type="button"
                disabled={!hasNext}
                onClick={() => onNavigate(currentIdx + 1)}
                className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30 transition-colors"
              >
                다음 ▶
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

/* ── 드레스/슈트 아이템 수정 모달 ── */
const DressItemModal = ({ modalData, isSuitFn, onSave, onDelete, onClose }) => {
  const { idx, item } = modalData;
  const isNew = idx === null;
  const [localItem, setLocalItem] = useState({ ...item });
  const [uploading, setUploading] = useState(false);
  const imageSrc = localItem.imageUrl
    ? getCompanyImageUrl(localItem.imageUrl)
    : null;
  const isSuitItem = isSuitFn(localItem);

  const handleImageChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const uploaded = await uploadCompanyImages([file]);
      if (uploaded?.[0]) setLocalItem((p) => ({ ...p, imageUrl: uploaded[0] }));
    } catch {
      alert("이미지 업로드에 실패했습니다.");
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  };

  return (
    /* 스크롤 가능한 오버레이 */
    <div
      className="fixed inset-0 z-50 overflow-y-auto"
      style={{
        backdropFilter: "blur(4px)",
        backgroundColor: "rgba(0,0,0,0.6)",
      }}
    >
      {/* 클릭-닫기 + 센터링 래퍼 */}
      <div
        className="flex min-h-full items-center justify-center p-4"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div className="relative w-full max-w-xl rounded-2xl bg-white shadow-2xl overflow-hidden my-4">
          {/* 헤더 */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
            <div className="flex items-center gap-2">
              <span
                className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${isSuitItem ? "bg-blue-100 text-blue-600" : "bg-rose-100 text-rose-600"}`}
              >
                {isSuitItem ? "슈트" : "드레스"}
              </span>
              <h3 className="text-base font-semibold text-slate-800">
                {isNew ? "새 아이템 추가" : "아이템 수정"}
              </h3>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="rounded-full p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors text-base leading-none"
            >
              ✕
            </button>
          </div>

          {/* 바디 */}
          <div className="flex flex-col sm:flex-row gap-5 p-6 max-h-[70vh] overflow-y-auto">
            {/* 이미지 */}
            <div className="sm:w-56 shrink-0">
              <label
                className="block cursor-pointer relative group rounded-xl overflow-hidden border-2 border-dashed border-slate-200 hover:border-rose-300 transition-colors"
                title="클릭하여 이미지 변경"
              >
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleImageChange}
                  disabled={uploading}
                />
                {imageSrc ? (
                  <>
                    <img
                      src={imageSrc}
                      alt={localItem.itemName || "이미지"}
                      className="h-48 sm:h-64 w-full object-contain bg-slate-50"
                    />
                    <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center rounded-xl">
                      <span className="text-white text-xs font-medium bg-black/60 px-3 py-1.5 rounded-lg">
                        📷 이미지 변경
                      </span>
                    </div>
                  </>
                ) : (
                  <div className="h-48 sm:h-64 w-full bg-slate-50 flex flex-col items-center justify-center gap-2 text-slate-400 group-hover:text-rose-400 transition-colors">
                    {uploading ? (
                      <>
                        <span className="text-2xl">⏳</span>
                        <span className="text-xs">업로드 중...</span>
                      </>
                    ) : (
                      <>
                        <span className="text-3xl">📷</span>
                        <span className="text-xs text-center px-4">
                          클릭하여
                          <br />
                          이미지 추가
                        </span>
                      </>
                    )}
                  </div>
                )}
              </label>
            </div>

            {/* 폼 */}
            <div className="flex-1 flex flex-col gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1.5">
                  아이템명 *
                </label>
                <input
                  value={localItem.itemName ?? ""}
                  onChange={(e) =>
                    setLocalItem((p) => ({ ...p, itemName: e.target.value }))
                  }
                  placeholder="예: 아이보리 볼가운"
                  className="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm focus:border-rose-300 focus:outline-none focus:ring-1 focus:ring-rose-100"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1.5">
                  가격
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    value={localItem.price ?? ""}
                    onChange={(e) =>
                      setLocalItem((p) => ({ ...p, price: e.target.value }))
                    }
                    placeholder="예: 1500000"
                    className="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm focus:border-rose-300 focus:outline-none focus:ring-1 focus:ring-rose-100"
                  />
                  <span className="text-sm text-slate-500 shrink-0 font-medium">
                    원
                  </span>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1.5">
                  보유 사이즈
                </label>
                <input
                  value={localItem.sizeRange ?? ""}
                  onChange={(e) =>
                    setLocalItem((p) => ({ ...p, sizeRange: e.target.value }))
                  }
                  placeholder="예: 44~66"
                  className="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm focus:border-rose-300 focus:outline-none focus:ring-1 focus:ring-rose-100"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1.5">
                  스타일 태그
                </label>
                <input
                  value={localItem.styleTags ?? ""}
                  onChange={(e) =>
                    setLocalItem((p) => ({ ...p, styleTags: e.target.value }))
                  }
                  placeholder="예: 로맨틱, 클래식, 볼가운"
                  className="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm focus:border-rose-300 focus:outline-none focus:ring-1 focus:ring-rose-100"
                />
                <p className="mt-1 text-xs text-slate-400">
                  쉼표(,)로 구분해서 입력하세요
                </p>
              </div>
            </div>
          </div>

          {/* 푸터 */}
          <div className="flex items-center justify-between px-6 py-4 border-t border-slate-100 bg-slate-50">
            {!isNew ? (
              <button
                type="button"
                onClick={() => onDelete(idx)}
                className="flex items-center gap-1.5 rounded-lg border border-red-200 px-4 py-2 text-sm text-red-500 hover:bg-red-50 transition-colors"
              >
                🗑 삭제
              </button>
            ) : (
              <div />
            )}
            <div className="flex gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 transition-colors"
              >
                취소
              </button>
              <button
                type="button"
                onClick={() => onSave(idx, localItem)}
                className={`rounded-lg px-5 py-2 text-sm font-semibold text-white transition-colors ${
                  isSuitItem
                    ? "bg-blue-500 hover:bg-blue-600"
                    : "bg-rose-500 hover:bg-rose-600"
                }`}
              >
                {isNew ? "추가" : "완료"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const DressDetail = ({ detail, cmno, canManageCompany, onRefresh }) => {
  const [activeTab, setActiveTab] = useState("DRESS");
  const [dressPage, setDressPage] = useState(0);
  const [suitPage, setSuitPage] = useState(0);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(null);
  // modalData: null | { idx: number | null, item: object }  (idx=null → 새 아이템)
  const [modalData, setModalData] = useState(null);
  // viewModalIdx: null | number  — 뷰 전용 크게 보기 모달 (activeIndexed 기준 인덱스)
  const [viewModalIdx, setViewModalIdx] = useState(null);

  if (!detail) return null;

  const allItems = detail.items || detail.dressItems || [];

  const isSuit = (it) => {
    const type = String(it?.itemType ?? "").toUpperCase();
    if (type === "SUIT") return true;
    const name = String(it?.itemName ?? "");
    return /슈트|수트|정장|슈츠|수츠|턱시도|tuxedo/i.test(name);
  };

  const displayItems = editing ? form.items : allItems;
  const dressIndexed = displayItems
    .map((it, idx) => ({ it, idx }))
    .filter(({ it }) => !isSuit(it));
  const suitIndexed = displayItems
    .map((it, idx) => ({ it, idx }))
    .filter(({ it }) => isSuit(it));
  const hasSuit = suitIndexed.length > 0;

  const activeIndexed = activeTab === "SUIT" ? suitIndexed : dressIndexed;
  const currentPage = activeTab === "SUIT" ? suitPage : dressPage;
  const setCurrentPage = activeTab === "SUIT" ? setSuitPage : setDressPage;
  const totalPages = Math.max(
    1,
    Math.ceil(activeIndexed.length / ITEMS_PER_PAGE),
  );
  const pageIndexed = activeIndexed.slice(
    currentPage * ITEMS_PER_PAGE,
    (currentPage + 1) * ITEMS_PER_PAGE,
  );

  /* ── edit mode 진입 ── */
  const startEdit = () => {
    setForm({
      sizeRange: detail.sizeRange ?? "",
      items: allItems.map((it) => ({ ...it })),
    });
    setEditing(true);
  };

  const cancelEdit = () => {
    setEditing(false);
    setForm(null);
    setModalData(null);
  };

  /* ── 전체 저장 ── */
  const handleSave = async () => {
    setSaving(true);
    try {
      await updateDressDetail(cmno, {
        cmno,
        sizeRange: form.sizeRange,
        items: form.items.map((it, i) => ({
          dressItemId: it.dressItemId ?? null,
          cmno,
          itemName: it.itemName ?? "",
          price: it.price === "" || it.price == null ? null : Number(it.price),
          imageUrl: it.imageUrl ?? null,
          ord: i,
          itemType: isSuit(it) ? "SUIT" : (it.itemType ?? "DRESS"),
          styleTags: it.styleTags ?? "",
          sizeRange: it.sizeRange ?? "",
        })),
      });
      setEditing(false);
      setForm(null);
      setModalData(null);
      onRefresh?.();
    } catch (err) {
      const status = err?.response?.status;
      const serverMsg =
        err?.response?.data?.message || err?.response?.data || "";
      console.error("드레스 상세 저장 실패:", status, serverMsg, err);
      if (status === 403)
        alert(
          "권한이 없습니다. 관리자 계정으로 로그인되어 있는지 확인해주세요.",
        );
      else if (status === 404)
        alert(`저장 실패: 업체를 찾을 수 없습니다. (cmno: ${cmno})`);
      else if (status === 500)
        alert(
          `서버 오류가 발생했습니다. 백엔드 로그를 확인해주세요.\n${serverMsg}`,
        );
      else alert(`저장에 실패했습니다. (${status ?? "네트워크 오류"})`);
    } finally {
      setSaving(false);
    }
  };

  /* ── 카드의 "수정" 버튼 클릭 → 모달 열기 ── */
  const openEditModal = (globalIdx) => {
    const currentItems = editing
      ? form.items
      : allItems.map((it) => ({ ...it }));
    if (!editing) {
      setForm({ sizeRange: detail.sizeRange ?? "", items: currentItems });
      setEditing(true);
    }
    setModalData({ idx: globalIdx, item: { ...currentItems[globalIdx] } });
  };

  /* ── "+ 추가" 버튼 클릭 → 빈 모달 열기 ── */
  const openAddModal = (type) => {
    const normalizedType = type.toUpperCase();
    if (!editing) {
      setForm({
        sizeRange: detail.sizeRange ?? "",
        items: allItems.map((it) => ({ ...it })),
      });
      setEditing(true);
    }
    setModalData({
      idx: null,
      item: {
        dressItemId: null,
        itemName: "",
        price: "",
        imageUrl: "",
        itemType: normalizedType,
        styleTags: "",
        sizeRange: "",
      },
    });
    if (normalizedType === "SUIT") setActiveTab("SUIT");
    else setActiveTab("DRESS");
  };

  /* ── 모달 "완료" ── */
  const handleModalSave = (idx, updatedItem) => {
    if (idx === null) {
      // 새 아이템 — form.items 끝에 추가
      setForm((prev) => {
        const newItems = [...prev.items, updatedItem];
        const isSuitNew = isSuit(updatedItem);
        const sameTypeCount = prev.items.filter((it) =>
          isSuitNew ? isSuit(it) : !isSuit(it),
        ).length;
        const targetPage = Math.floor(sameTypeCount / ITEMS_PER_PAGE);
        if (isSuitNew) setSuitPage(targetPage);
        else setDressPage(targetPage);
        return { ...prev, items: newItems };
      });
    } else {
      // 기존 아이템 수정
      setForm((prev) => {
        const updated = [...prev.items];
        updated[idx] = { ...updated[idx], ...updatedItem };
        return { ...prev, items: updated };
      });
    }
    setModalData(null);
  };

  /* ── 모달 "삭제" ── */
  const handleModalDelete = (idx) => {
    if (!window.confirm("이 항목을 삭제하시겠습니까?")) return;
    setForm((prev) => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== idx),
    }));
    setModalData(null);
  };

  const editAction =
    canManageCompany &&
    (editing ? (
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={cancelEdit}
          className="rounded-md border border-slate-200 px-3 py-1 text-xs text-slate-500 hover:bg-slate-50"
        >
          취소
        </button>
        <button
          type="button"
          onClick={handleSave}
          disabled={saving}
          className="rounded-md bg-rose-500 px-3 py-1 text-xs font-semibold text-white hover:bg-rose-600 disabled:opacity-50"
        >
          {saving ? "저장 중…" : "저장"}
        </button>
      </div>
    ) : (
      <button
        type="button"
        onClick={startEdit}
        className="text-sm font-bold text-rose-400 hover:text-rose-600 transition-colors"
      >
        ✏ 수정
      </button>
    ));

  return (
    <DetailSection title="드레스 상세" headerAction={editAction}>
      {/* 아이템 수정 모달 (관리자) */}
      {modalData && (
        <DressItemModal
          modalData={modalData}
          isSuitFn={isSuit}
          onSave={handleModalSave}
          onDelete={handleModalDelete}
          onClose={() => setModalData(null)}
        />
      )}

      {/* 아이템 뷰 모달 (모든 유저 — 크게 보기) */}
      {viewModalIdx !== null && (
        <DressItemViewModal
          item={activeIndexed[viewModalIdx]?.it ?? {}}
          isSuitFn={isSuit}
          allItems={activeIndexed.map(({ it }) => it)}
          currentIdx={viewModalIdx}
          onNavigate={(nextIdx) => setViewModalIdx(nextIdx)}
          onClose={() => setViewModalIdx(null)}
        />
      )}

      {/* 사이즈 */}
      {editing ? (
        <div className="mb-4 flex items-center gap-2">
          <span className="text-xs text-slate-400 shrink-0">보유 사이즈</span>
          <input
            value={form.sizeRange}
            onChange={(e) =>
              setForm((p) => ({ ...p, sizeRange: e.target.value }))
            }
            className="rounded-md border border-slate-200 px-2 py-1 text-sm focus:border-rose-300 focus:outline-none"
            placeholder="예: 44~66"
          />
        </div>
      ) : (
        <InfoRow label="보유 사이즈" value={detail.sizeRange || "-"} />
      )}

      {/* 탭 */}
      <div className="mt-4 border-b border-slate-200 flex items-center">
        {[
          {
            key: "DRESS",
            label: "드레스",
            count: dressIndexed.length,
            activeClass: "border-rose-400 text-rose-600",
            badgeClass: "bg-rose-100 text-rose-600",
          },
          ...(hasSuit || editing
            ? [
                {
                  key: "SUIT",
                  label: "슈트",
                  count: suitIndexed.length,
                  activeClass: "border-blue-400 text-blue-600",
                  badgeClass: "bg-blue-100 text-blue-600",
                },
              ]
            : []),
        ].map(({ key, label, count, activeClass, badgeClass }) => (
          <button
            key={key}
            type="button"
            onClick={() => {
              setActiveTab(key);
              setViewModalIdx(null);
              key === "SUIT" ? setSuitPage(0) : setDressPage(0);
            }}
            className={`px-5 py-2 text-sm font-medium border-b-2 transition-colors ${activeTab === key ? activeClass : "border-transparent text-slate-400 hover:text-slate-600"}`}
          >
            {label}
            <span
              className={`ml-1 rounded-full px-1.5 py-0.5 text-xs ${activeTab === key ? badgeClass : "bg-slate-100 text-slate-400"}`}
            >
              {count}
            </span>
          </button>
        ))}
      </div>

      {/* 추가 버튼 — 관리자라면 항상 표시 */}
      {canManageCompany && (
        <div className="mt-3 flex gap-2 justify-end">
          {activeTab === "DRESS" && (
            <button
              type="button"
              onClick={() => openAddModal("DRESS")}
              className="flex items-center gap-1 rounded-md border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-medium text-rose-600 hover:bg-rose-100"
            >
              + 드레스 추가
            </button>
          )}
          {activeTab === "SUIT" && (
            <button
              type="button"
              onClick={() => openAddModal("SUIT")}
              className="flex items-center gap-1 rounded-md border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-100"
            >
              + 슈트 추가
            </button>
          )}
        </div>
      )}

      {/* 그리드 (8개/페이지) */}
      <div className="mt-4">
        {activeIndexed.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-400">
            {editing
              ? `위의 추가 버튼으로 ${activeTab === "SUIT" ? "슈트" : "드레스"}를 등록하세요.`
              : `등록된 ${activeTab === "SUIT" ? "슈트" : "드레스"} 항목이 없습니다.`}
          </p>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {pageIndexed.map(({ it: item, idx: globalIdx }, pagePos) => {
                const imageSrc = item.imageUrl
                  ? getCompanyImageUrl(item.imageUrl)
                  : null;
                // activeIndexed 전체 기준 뷰 모달 인덱스
                const viewIdx = currentPage * ITEMS_PER_PAGE + pagePos;
                return (
                  <div
                    key={`item-${globalIdx}`}
                    className="group rounded-lg border border-slate-200 bg-white overflow-hidden hover:shadow-md transition-shadow"
                  >
                    {/* 이미지 — 클릭 시 뷰 모달 */}
                    <button
                      type="button"
                      className="relative block w-full overflow-hidden cursor-zoom-in"
                      onClick={() => setViewModalIdx(viewIdx)}
                    >
                      {imageSrc ? (
                        <>
                          <img
                            src={imageSrc}
                            alt={item.itemName}
                            className="h-36 sm:h-44 w-full object-contain bg-slate-50 transition-transform duration-300 group-hover:scale-105"
                          />
                          <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors flex items-center justify-center">
                            <span className="opacity-0 group-hover:opacity-100 transition-opacity text-white text-xs font-medium bg-black/50 px-2 py-1 rounded-full">
                              🔍 크게 보기
                            </span>
                          </div>
                        </>
                      ) : (
                        <div className="h-36 sm:h-44 w-full bg-slate-100 flex items-center justify-center text-slate-300 text-xs">
                          이미지 없음
                        </div>
                      )}
                    </button>

                    <div className="p-3">
                      <span
                        className={`inline-block mb-1 rounded-full px-2 py-0.5 text-xs font-medium ${isSuit(item) ? "bg-blue-50 text-blue-500" : "bg-rose-50 text-rose-500"}`}
                      >
                        {isSuit(item) ? "슈트" : "드레스"}
                      </span>
                      {/* 이름 클릭도 뷰 모달 열기 */}
                      <div
                        className="text-sm font-semibold text-slate-800 truncate cursor-pointer hover:text-rose-500 transition-colors"
                        onClick={() => setViewModalIdx(viewIdx)}
                      >
                        {item.itemName || "이름 없음"}
                      </div>
                      <div className="mt-0.5 text-xs text-slate-500">
                        {item.price
                          ? `${Number(item.price).toLocaleString()}원`
                          : "-"}
                      </div>
                      <div className="text-xs text-slate-400 truncate">
                        {item.sizeRange || ""}
                        {item.styleTags ? ` · ${item.styleTags}` : ""}
                      </div>
                      {/* 수정 모드에서만 수정 버튼 표시 */}
                      {editing && (
                        <button
                          type="button"
                          onClick={() => openEditModal(globalIdx)}
                          className="mt-2 w-full rounded border border-slate-200 py-1 text-xs text-slate-500 hover:bg-rose-50 hover:border-rose-200 hover:text-rose-500 transition-colors"
                        >
                          ✏ 수정
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {totalPages > 1 && (
              <div className="mt-4 flex items-center justify-center gap-3">
                <button
                  type="button"
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  disabled={currentPage === 0}
                  className="rounded-md border border-slate-200 px-3 py-1 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30"
                >
                  ◀ 이전
                </button>
                <span className="text-xs text-slate-500">
                  {currentPage + 1} / {totalPages}
                </span>
                <button
                  type="button"
                  onClick={() =>
                    setCurrentPage((p) => Math.min(totalPages - 1, p + 1))
                  }
                  disabled={currentPage >= totalPages - 1}
                  className="rounded-md border border-slate-200 px-3 py-1 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30"
                >
                  다음 ▶
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </DetailSection>
  );
};

const HallDetail = ({ detail }) => {
  if (!detail) return null;
  const items = detail.items || detail.hallItems || [];
  return (
    <DetailSection title="웨딩홀 상세">
      <div className="grid gap-3 sm:grid-cols-2">
        <InfoRow label="홀명" value={detail.hallName || "-"} />
        <InfoRow
          label="홀 유형"
          value={hallTypeLabel[detail.hallType] || detail.hallType || "-"}
        />
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        {items.map((item, index) => (
          <ItemCard key={`${item.itemName}-${index}`} title={item.itemName}>
            <InfoLine label="가격" value={formatPrice(item.price)} />
            <InfoLine
              label="수용 인원"
              value={
                item.capacity
                  ? `${Number(item.capacity).toLocaleString()}명`
                  : "-"
              }
            />
            <InfoLine
              label="식사 유형"
              value={mealTypeLabel[item.mealType] || item.mealType || "-"}
            />
          </ItemCard>
        ))}
      </div>
    </DetailSection>
  );
};

const StudioDetail = ({ detail }) => {
  if (!detail) return null;
  const tags = Array.isArray(detail.themeTags)
    ? detail.themeTags.join(", ")
    : detail.themeTags;
  return (
    <DetailSection title="스튜디오 상세">
      <InfoRow label="테마" value={tags || detail.theme || "-"} />
      {detail.priceRange ? (
        <InfoRow label="가격대" value={detail.priceRange} />
      ) : null}
      {detail.rating ? <InfoRow label="평점" value={detail.rating} /> : null}
    </DetailSection>
  );
};

const DetailSection = ({ title, children, headerAction }) => (
  <div className="mt-5 rounded-lg border border-slate-200 bg-white p-3 sm:p-5">
    <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
      <h3 className="text-base font-semibold">{title}</h3>
      {headerAction && <div>{headerAction}</div>}
    </div>
    {children}
  </div>
);

const ItemCard = ({ title, children }) => (
  <div className="rounded-md border border-slate-100 p-4">
    <div className="mb-2 text-sm font-semibold text-slate-800">
      {title || "항목"}
    </div>
    <div className="space-y-1">{children}</div>
  </div>
);

const InfoLine = ({ label, value }) => (
  <div className="flex justify-between gap-3 text-sm">
    <span className="text-slate-400">{label}</span>
    <span className="text-right text-slate-700">{value}</span>
  </div>
);

const InfoRow = ({ label, value }) => (
  <div className="border-b border-slate-100 py-3 last:border-b-0">
    <div className="text-xs font-medium uppercase text-slate-400">{label}</div>
    <div className="mt-1 text-sm text-slate-800">{value}</div>
  </div>
);

const formatPrice = (price) => {
  if (price === null || price === undefined || price === "") return "-";
  return `${Number(price).toLocaleString()}원`;
};

export default CompanyReadComponent;
