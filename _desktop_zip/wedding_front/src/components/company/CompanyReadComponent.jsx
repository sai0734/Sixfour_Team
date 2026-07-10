import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { deleteOne, getCompanyImageUrl, getOne, updateMakeupDetail, updateDressDetail, uploadCompanyImages } from "../../api/companyApi";
import FetchingModal from "../common/FetchingModal";
import useCustomLogin from "../../hooks/useCustomLogin";

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
  FULL: "풀 패키지",
  TWO: "2종 패키지",
  THREE: "3종 패키지",
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

const dressTypeLabel = {
  ALINE: "A라인",
  BELL: "벨라인",
  MERMAID: "머메이드",
  MINI: "미니",
  SLIM: "슬림",
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
  const canManageCompany = loginState.roleNames?.some((roleName) =>
    adminRoles.includes(roleName),
  ) && !company._isDummyOnly;
  // 관리자 경로에서 상세로 들어온 경우 목록/수정 이동도 관리자 경로로 유지합니다.
  const companyPathPrefix = location.pathname.startsWith("/admin/companies")
    ? "/admin/companies"
    : "/companies";

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

  const mainImage = company.uploadFileNames?.[0];

  const detailImageClassByCategory = {
    DRESS: "h-[520px] w-full",
    MAKEUP: "h-[420px] w-full",
    STUDIO: "h-[460px] w-full",
  };

  const thumbnailImageClassByCategory = {
    DRESS: "h-40 w-full",
    MAKEUP: "h-52 w-full",
    STUDIO: "h-40 w-50",
  };

  const getDetailImageClass = (category) =>
    detailImageClassByCategory[category] || "h-80 w-full";
  const getThumbnailImageClass = (category) =>
    thumbnailImageClassByCategory[category] || "h-28 w-full";

  return (
    <section className="mx-auto max-w-5xl p-4 text-slate-800">
      {fetching ? <FetchingModal /> : null}

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <button
            className="mb-3 rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50"
            type="button"
            onClick={() => navigate(`${companyPathPrefix}/list${listSearch}`)}
          >
            목록으로
          </button>
          <h2 className="text-2xl font-semibold">{company.name}</h2>
          <p className="mt-1 text-sm text-slate-500">
            {categoryLabel[company.category] || company.category}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-700">
            업체 번호 {company.cmno}
          </div>
          {canManageCompany ? (
            <>
              <button
                className="rounded-md border border-blue-200 px-3 py-2 text-sm text-blue-700 hover:bg-blue-50"
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
                className="rounded-md border border-red-200 px-3 py-2 text-sm text-red-700 hover:bg-red-50"
                type="button"
                onClick={handleDelete}
              >
                삭제
              </button>
            </>
          ) : null}
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
          {mainImage ? (
            <img
              className={`${getDetailImageClass(company.category)} object-contain bg-white`}
              src={getCompanyImageUrl(mainImage)}
              alt={company.name}
            />
          ) : (
            <div className="flex h-80 items-center justify-center bg-slate-100 text-slate-400">
              대표 이미지가 없습니다.
            </div>
          )}
          <div className="grid grid-cols-3 gap-2 p-3">
            {(company.uploadFileNames || []).slice(1, 7).map((fileName) => (
              <img
                key={fileName}
                className={`${getThumbnailImageClass(company.category)} rounded-md object-contain bg-white`}
                src={getCompanyImageUrl(fileName, true)}
                alt={company.name}
              />
            ))}
          </div>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-5">
          <h3 className="mb-4 text-base font-semibold">업체 정보</h3>
          <InfoRow label="대표자" value={company.ceoName || "-"} />
          <InfoRow label="연락처" value={company.phone || "-"} />
          <InfoRow label="주소" value={company.address || "-"} />
          <InfoRow
            label="평균 가격"
            value={
              company.priceAvg
                ? `${Number(company.priceAvg).toLocaleString()}원`
                : "-"
            }
          />
          <InfoRow label="위도" value={company.latitude ?? "-"} />
          <InfoRow label="경도" value={company.longitude ?? "-"} />
        </div>
      </div>

      <div className="mt-5 rounded-lg border border-slate-200 bg-white p-5">
        <h3 className="mb-3 text-base font-semibold">업체 소개</h3>
        <p className="whitespace-pre-wrap text-sm leading-6 text-slate-600">
          {company.description || "등록된 소개가 없습니다."}
        </p>
      </div>

      <CategoryDetail company={company} canManageCompany={canManageCompany} onRefresh={handleRefresh} />
    </section>
  );
};

const CategoryDetail = ({ company, canManageCompany, onRefresh }) => {
  if (company.category === "MAKEUP") {
    return <MakeupDetail detail={company.makeupDetail} cmno={company.cmno} canManageCompany={canManageCompany} onRefresh={onRefresh} />;
  }
  if (company.category === "DRESS") {
    return <DressDetail detail={company.dressDetail} cmno={company.cmno} canManageCompany={canManageCompany} onRefresh={onRefresh} />;
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
    <div className="text-xs font-medium uppercase text-slate-400 mb-1">{label}</div>
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
    <div className="text-xs font-medium uppercase text-slate-400 mb-1">{label}</div>
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
  { value: "HAIR", label: "헤어 패키지" },
  { value: "MAKEUP", label: "메이크업 패키지" },
  { value: "NAIL", label: "네일 패키지" },
  { value: "HAIR_MAKEUP", label: "헤어+메이크업 패키지" },
  { value: "FULL", label: "풀 패키지" },
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
      const serverMsg = err?.response?.data?.message || err?.response?.data || "";
      console.error("메이크업 상세 저장 실패:", status, serverMsg, err);
      if (status === 403) {
        alert("권한이 없습니다. 관리자 계정으로 로그인되어 있는지 확인해주세요.");
      } else if (status === 404) {
        alert(`저장 실패: 해당 업체의 메이크업 상세 정보를 찾을 수 없습니다. (cmno: ${cmno})`);
      } else if (status === 500) {
        alert(`서버 오류가 발생했습니다. 백엔드 로그를 확인해주세요.\n${serverMsg}`);
      } else {
        alert(`저장에 실패했습니다. (${status ?? "네트워크 오류"})`);
      }
    } finally {
      setSaving(false);
    }
  };

  const editAction = canManageCompany && (
    editing ? (
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
    )
  );

  return (
    <DetailSection title="메이크업 상세" headerAction={editAction}>
      {editing ? (
        <>
          {/* 가격 수정 */}
          <div className="mb-1 text-xs font-semibold text-slate-500">가격 수정</div>
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
            <div className="text-xs font-semibold text-slate-500">패키지 수정</div>
            <button
              type="button"
              onClick={handleAddPackage}
              className="flex items-center gap-1 rounded-md border border-rose-200 px-2 py-1 text-xs text-rose-500 hover:bg-rose-50"
            >
              + 패키지 추가
            </button>
          </div>

          {form.packages.length === 0 ? (
            <p className="text-xs text-slate-400 py-2">등록된 패키지가 없습니다. 추가 버튼으로 패키지를 추가하세요.</p>
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
                      <div className="text-xs text-slate-400 mb-1">패키지 종류</div>
                      <select
                        value={pkg.packageType}
                        onChange={(e) => handlePackageTypeChange(index, e.target.value)}
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
                      <div className="text-xs text-slate-400 mb-1">할인율 (%)</div>
                      <div className="flex items-center gap-1">
                        <input
                          type="number"
                          min="0"
                          max="100"
                          step="0.1"
                          value={pkg.discountRate}
                          onChange={(e) => handlePackageRateChange(index, e.target.value)}
                          className="w-full rounded-md border border-slate-200 px-2 py-1.5 text-sm text-slate-800 focus:border-rose-300 focus:outline-none"
                          placeholder="예: 10"
                        />
                        <span className="text-sm text-slate-500 shrink-0">%</span>
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
            <InfoRow label="메이크업 가격" value={formatPrice(detail.makeupPrice)} />
            <InfoRow label="네일 가격" value={formatPrice(detail.nailPrice)} />
          </div>
          {packages.length > 0 && (
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {packages.map((pkg, index) => (
                <InfoRow
                  key={`${pkg.packageType || "PACKAGE"}-${index}`}
                  label={packageTypeLabel[pkg.packageType] || pkg.packageType || "패키지"}
                  value={formatDiscountRate(pkg.discountRate)}
                />
              ))}
            </div>
          )}
        </>
      )}
    </DetailSection>
  );
};

const DRESS_ITEM_TYPE_OPTIONS = [
  { value: "DRESS", label: "드레스" },
  { value: "ALINE", label: "A라인" },
  { value: "BELL", label: "벨라인" },
  { value: "MERMAID", label: "머메이드" },
  { value: "MINI", label: "미니" },
  { value: "SLIM", label: "슬림" },
];

const SUIT_ITEM_TYPE_OPTIONS = [
  { value: "SUIT", label: "슈트" },
];

const ITEMS_PER_PAGE = 4;

const DressDetail = ({ detail, cmno, canManageCompany, onRefresh }) => {
  const [activeTab, setActiveTab] = useState("DRESS");
  const [dressPage, setDressPage] = useState(0);
  const [suitPage, setSuitPage] = useState(0);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(null);
  const [editingItemIdx, setEditingItemIdx] = useState(null);

  if (!detail) return null;

  const allItems = detail.items || detail.dressItems || [];

  // itemType이 "SUIT"이거나, 이름에 슈트/수트/정장 계열 키워드가 포함된 경우 슈트로 분류
  const isSuit = (it) => {
    const type = String(it?.itemType ?? "").toUpperCase();
    if (type === "SUIT") return true;
    const name = String(it?.itemName ?? "");
    return /슈트|수트|정장|슈츠|수츠|턱시도|tuxedo/i.test(name);
  };

  const displayItems = editing ? form.items : allItems;
  const dressItems = displayItems.filter((it) => !isSuit(it));
  const suitItems  = displayItems.filter((it) =>  isSuit(it));
  const hasSuit = suitItems.length > 0;

  const currentItems   = isSuit({ itemType: activeTab }) ? suitItems : dressItems;
  const currentPage    = activeTab === "SUIT" ? suitPage  : dressPage;
  const setCurrentPage = activeTab === "SUIT" ? setSuitPage : setDressPage;
  const totalPages = Math.ceil(currentItems.length / ITEMS_PER_PAGE);
  const pageItems  = currentItems.slice(
    currentPage * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE + ITEMS_PER_PAGE
  );
  const typeOptions = activeTab === "SUIT" ? SUIT_ITEM_TYPE_OPTIONS : DRESS_ITEM_TYPE_OPTIONS;

  const startEdit = () => {
    setForm({
      sizeRange: detail.sizeRange ?? "",
      items: allItems.map((it) => ({ ...it })),
    });
    setEditingItemIdx(null);
    setEditing(true);
  };

  const cancelEdit = () => {
    setEditing(false);
    setForm(null);
    setEditingItemIdx(null);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await updateDressDetail(cmno, {
        cmno,
        sizeRange: form.sizeRange,
        items: form.items.map((it, i) => {
          const normalizedType = isSuit(it) ? "SUIT" : (it.itemType ?? "DRESS");
          return {
            dressItemId: it.dressItemId ?? null,
            cmno,
            itemName: it.itemName,
            price: it.price === "" ? null : Number(it.price),
            imageUrl: it.imageUrl ?? null,
            ord: i,
            itemType: normalizedType,
            styleTags: it.styleTags ?? "",
            sizeRange: it.sizeRange ?? "",
          };
        }),
      });
      setEditing(false);
      setForm(null);
      setEditingItemIdx(null);
      onRefresh?.();
    } catch (err) {
      const status = err?.response?.status;
      console.error("드레스 상세 저장 실패:", status, err);
      if (status === 403) alert("권한이 없습니다.");
      else if (status === 404) alert(`저장 실패: 업체를 찾을 수 없습니다. (cmno: ${cmno})`);
      else alert(`저장에 실패했습니다. (${status ?? "네트워크 오류"})`);
    } finally {
      setSaving(false);
    }
  };

  const handleAddItem = (type) => {
    const normalizedType = type.toUpperCase();
    const newItem = {
      dressItemId: null,
      itemName: "",
      price: "",
      imageUrl: "",
      itemType: normalizedType,
      styleTags: "",
      sizeRange: "",
    };
    const nextIdx = form.items.length;
    setForm((prev) => ({ ...prev, items: [...prev.items, newItem] }));
    setEditingItemIdx(nextIdx);
    if (normalizedType === "SUIT") {
      setActiveTab("SUIT");
      setSuitPage(Math.floor(suitItems.length / ITEMS_PER_PAGE));
    } else {
      setActiveTab("DRESS");
      setDressPage(Math.floor(dressItems.length / ITEMS_PER_PAGE));
    }
  };

  const handleRemoveItem = (globalIdx) => {
    setForm((prev) => ({
      ...prev,
      items: prev.items.filter((_, i) => i !== globalIdx),
    }));
    setEditingItemIdx(null);
  };

  const handleItemFieldChange = (globalIdx, field, value) => {
    setForm((prev) => {
      const updated = [...prev.items];
      updated[globalIdx] = { ...updated[globalIdx], [field]: value };
      return { ...prev, items: updated };
    });
  };

  const getGlobalIdx = (item) =>
    editing ? form.items.findIndex((it) => it === item) : -1;

  const editAction = canManageCompany && (
    editing ? (
      <div className="flex items-center gap-2">
        <button type="button" onClick={cancelEdit}
          className="rounded-md border border-slate-200 px-3 py-1 text-xs text-slate-500 hover:bg-slate-50">
          취소
        </button>
        <button type="button" onClick={handleSave} disabled={saving}
          className="rounded-md bg-rose-500 px-3 py-1 text-xs font-semibold text-white hover:bg-rose-600 disabled:opacity-50">
          {saving ? "저장 중…" : "저장"}
        </button>
      </div>
    ) : (
      <button type="button" onClick={startEdit}
        className="text-sm font-bold text-rose-400 hover:text-rose-600 transition-colors">
        ✏ 수정
      </button>
    )
  );

  return (
    <DetailSection title="드레스 상세" headerAction={editAction}>
      {/* 사이즈 */}
      {editing ? (
        <div className="mb-4 flex items-center gap-2">
          <span className="text-xs text-slate-400 shrink-0">보유 사이즈</span>
          <input
            value={form.sizeRange}
            onChange={(e) => setForm((p) => ({ ...p, sizeRange: e.target.value }))}
            className="rounded-md border border-slate-200 px-2 py-1 text-sm focus:border-rose-300 focus:outline-none"
            placeholder="예: 44~66"
          />
        </div>
      ) : (
        <InfoRow label="보유 사이즈" value={detail.sizeRange || "-"} />
      )}

      {/* 탭 — 슈트가 있을 때만 슈트 탭 표시 */}
      <div className="mt-4 flex items-center border-b border-slate-200">
        {[
          { key: "DRESS", label: "드레스", count: dressItems.length, color: "rose", always: true },
          { key: "SUIT",  label: "슈트",   count: suitItems.length,  color: "blue", always: false },
        ]
          .filter(({ always, key }) => always || hasSuit || (editing && key === "SUIT"))
          .map(({ key, label, count, color }) => (
          <button
            key={key}
            type="button"
            onClick={() => { setActiveTab(key); key === "SUIT" ? setSuitPage(0) : setDressPage(0); }}
            className={`px-5 py-2 text-sm font-medium border-b-2 transition-colors ${
              activeTab === key
                ? `border-${color}-400 text-${color}-600`
                : "border-transparent text-slate-400 hover:text-slate-600"
            }`}
          >
            {label}
            <span className={`ml-1 rounded-full px-1.5 py-0.5 text-xs ${
              activeTab === key ? `bg-${color}-100 text-${color}-600` : "bg-slate-100 text-slate-400"
            }`}>
              {count}
            </span>
          </button>
        ))}

        {/* 수정 모드일 때만 추가 버튼 */}
        {editing && (
          <button
            type="button"
            onClick={() => handleAddItem(activeTab === "SUIT" ? "SUIT" : "DRESS")}
            className={`ml-auto mb-1 flex items-center gap-1 rounded-md border px-2 py-1 text-xs ${
              activeTab === "SUIT"
                ? "border-blue-200 text-blue-500 hover:bg-blue-50"
                : "border-rose-200 text-rose-500 hover:bg-rose-50"
            }`}
          >
            + {activeTab === "SUIT" ? "슈트" : "드레스"} 추가
          </button>
        )}
      </div>

      {/* 슬라이드 그리드 */}
      <div className="mt-4">
        {currentItems.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-400">
            {editing
              ? `+ ${activeTab === "SUIT" ? "슈트" : "드레스"} 추가 버튼으로 항목을 등록하세요.`
              : `등록된 ${activeTab === "SUIT" ? "슈트" : "드레스"} 항목이 없습니다.`}
          </p>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              {pageItems.map((item) => {
                const globalIdx = getGlobalIdx(item);
                const isEditingThis = editing && editingItemIdx === globalIdx;
                const imageSrc = item.imageUrl ? getCompanyImageUrl(item.imageUrl) : null;

                return (
                  <div
                    key={globalIdx >= 0 ? globalIdx : item.dressItemId}
                    className={`rounded-lg border overflow-hidden ${
                      isEditingThis ? "border-rose-300 bg-rose-50" : "border-slate-200 bg-white"
                    }`}
                  >
                    {/* 이미지 영역 — 수정 모드에서 클릭 시 파일 업로드 */}
                    {editing ? (
                      <label
                        className="block cursor-pointer relative group"
                        title="클릭하여 이미지 변경"
                      >
                        <input
                          type="file"
                          accept="image/*"
                          className="hidden"
                          onChange={async (e) => {
                            const file = e.target.files?.[0];
                            if (!file) return;
                            try {
                              const uploaded = await uploadCompanyImages([file]);
                              if (uploaded?.[0]) {
                                handleItemFieldChange(globalIdx, "imageUrl", uploaded[0]);
                              }
                            } catch {
                              alert("이미지 업로드에 실패했습니다.");
                            }
                            e.target.value = "";
                          }}
                        />
                        {imageSrc ? (
                          <>
                            <img src={imageSrc} alt={item.itemName}
                              className="h-44 w-full object-contain bg-slate-50" />
                            <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                              <span className="text-white text-xs font-medium bg-black/50 px-2 py-1 rounded">
                                📷 이미지 변경
                              </span>
                            </div>
                          </>
                        ) : (
                          <div className="h-44 w-full bg-slate-100 flex flex-col items-center justify-center gap-1 text-slate-400 hover:bg-rose-50 hover:text-rose-400 transition-colors">
                            <span className="text-2xl">📷</span>
                            <span className="text-xs">클릭하여 이미지 추가</span>
                          </div>
                        )}
                      </label>
                    ) : (
                      imageSrc ? (
                        <img src={imageSrc} alt={item.itemName}
                          className="h-44 w-full object-contain bg-slate-50" />
                      ) : (
                        <div className="h-44 w-full bg-slate-100 flex items-center justify-center text-slate-300 text-xs">
                          이미지 없음
                        </div>
                      )
                    )}
                    <div className="p-3">
                      {/* 타입 배지 */}
                      {!isEditingThis && (
                        <span className={`inline-block mb-1 rounded-full px-2 py-0.5 text-xs font-medium ${
                          isSuit(item) ? "bg-blue-50 text-blue-500" : "bg-rose-50 text-rose-500"
                        }`}>
                          {isSuit(item) ? "슈트" : "드레스"}
                        </span>
                      )}
                      {isEditingThis ? (
                        <div className="flex flex-col gap-1.5">
                          <input value={item.itemName}
                            onChange={(e) => handleItemFieldChange(globalIdx, "itemName", e.target.value)}
                            placeholder="아이템명"
                            className="w-full rounded border border-slate-200 px-2 py-1 text-xs focus:border-rose-300 focus:outline-none" />
                          <div className="flex items-center gap-1">
                            <input type="number" value={item.price}
                              onChange={(e) => handleItemFieldChange(globalIdx, "price", e.target.value)}
                              placeholder="가격"
                              className="w-full rounded border border-slate-200 px-2 py-1 text-xs focus:border-rose-300 focus:outline-none" />
                            <span className="text-xs text-slate-400 shrink-0">원</span>
                          </div>
                          <select value={item.itemType}
                            onChange={(e) => handleItemFieldChange(globalIdx, "itemType", e.target.value)}
                            className="w-full rounded border border-slate-200 px-2 py-1 text-xs bg-white focus:border-rose-300 focus:outline-none">
                            {typeOptions.map((opt) => (
                              <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                          </select>
                          <input value={item.sizeRange}
                            onChange={(e) => handleItemFieldChange(globalIdx, "sizeRange", e.target.value)}
                            placeholder="사이즈 (예: 44~66)"
                            className="w-full rounded border border-slate-200 px-2 py-1 text-xs focus:border-rose-300 focus:outline-none" />
                          <input value={item.styleTags}
                            onChange={(e) => handleItemFieldChange(globalIdx, "styleTags", e.target.value)}
                            placeholder="태그 (쉼표 구분)"
                            className="w-full rounded border border-slate-200 px-2 py-1 text-xs focus:border-rose-300 focus:outline-none" />
                          <div className="flex gap-1 mt-1">
                            <button type="button" onClick={() => setEditingItemIdx(null)}
                              className="flex-1 rounded bg-slate-100 py-1 text-xs text-slate-600 hover:bg-slate-200">
                              완료
                            </button>
                            <button type="button" onClick={() => handleRemoveItem(globalIdx)}
                              className="flex-1 rounded bg-red-50 py-1 text-xs text-red-500 hover:bg-red-100">
                              삭제
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <div className="text-sm font-semibold text-slate-800 truncate">
                            {item.itemName || "이름 없음"}
                          </div>
                          <div className="mt-1 text-xs text-slate-500">
                            {item.price ? `${Number(item.price).toLocaleString()}원` : "-"}
                          </div>
                          <div className="text-xs text-slate-400 truncate">
                            {item.sizeRange || ""}
                            {item.styleTags ? ` · ${item.styleTags}` : ""}
                          </div>
                          {editing && (
                            <button type="button"
                              onClick={() => setEditingItemIdx(globalIdx)}
                              className="mt-2 w-full rounded border border-slate-200 py-1 text-xs text-slate-500 hover:bg-slate-50">
                              수정
                            </button>
                          )}
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* 페이지네이션 */}
            {totalPages > 1 && (
              <div className="mt-4 flex items-center justify-center gap-3">
                <button type="button"
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  disabled={currentPage === 0}
                  className="rounded-md border border-slate-200 px-3 py-1 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30">
                  ◀ 이전
                </button>
                <span className="text-xs text-slate-500">
                  {currentPage + 1} / {totalPages}
                </span>
                <button type="button"
                  onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={currentPage >= totalPages - 1}
                  className="rounded-md border border-slate-200 px-3 py-1 text-xs text-slate-500 hover:bg-slate-50 disabled:opacity-30">
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
  <div className="mt-5 rounded-lg border border-slate-200 bg-white p-5">
    <div className="mb-3 flex items-center justify-between">
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

const formatDiscountRate = (rate) => {
  const numericRate = Number(rate || 0);
  if (!numericRate) return "할인 없음";
  return `${numericRate > 1 ? numericRate : numericRate * 100}%`;
};

export default CompanyReadComponent;
