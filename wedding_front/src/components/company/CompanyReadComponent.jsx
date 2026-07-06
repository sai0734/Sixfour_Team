import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { deleteOne, getCompanyImageUrl, getOne } from "../../api/companyApi";
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
  const [company, setCompany] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const { exceptionHandle } = useCustomLogin();
  const navigate = useNavigate();
  const loginState = useSelector((state) => state.loginSlice);
  const canManageCompany = loginState.roleNames?.some((roleName) =>
    adminRoles.includes(roleName),
  );

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
      navigate({ pathname: "/companies/list" });
    } catch (err) {
      exceptionHandle(err);
    } finally {
      setFetching(false);
    }
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

  const getDetailImageClass = (category) => detailImageClassByCategory[category] || "h-80 w-full";
  const getThumbnailImageClass = (category) => thumbnailImageClassByCategory[category] || "h-28 w-full";

  return (
    <section className="mx-auto max-w-5xl p-4 text-slate-800">
      {fetching ? <FetchingModal /> : null}

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <button
            className="mb-3 rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50"
            type="button"
            onClick={() => navigate({ pathname: "/companies/list" })}
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
                onClick={() => navigate({ pathname: `/companies/modify/${company.cmno}` })}
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
          <InfoRow label="평균 가격" value={company.priceAvg ? `${Number(company.priceAvg).toLocaleString()}원` : "-"} />
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

      <CategoryDetail company={company} />
    </section>
  );
};

const CategoryDetail = ({ company }) => {
  if (company.category === "MAKEUP") {
    return <MakeupDetail detail={company.makeupDetail} />;
  }
  if (company.category === "DRESS") {
    return <DressDetail detail={company.dressDetail} />;
  }
  if (company.category === "HALL") {
    return <HallDetail detail={company.hallDetail} />;
  }
  if (company.category === "STUDIO") {
    return <StudioDetail detail={company.studioDetail} />;
  }
  return null;
};

const MakeupDetail = ({ detail }) => {
  if (!detail) return null;
  const packages = (detail.packages || []).filter(Boolean);
  return (
    <DetailSection title="메이크업 상세">
      <div className="grid gap-3 sm:grid-cols-3">
        <InfoRow label="헤어 가격" value={formatPrice(detail.hairPrice)} />
        <InfoRow label="메이크업 가격" value={formatPrice(detail.makeupPrice)} />
        <InfoRow label="네일 가격" value={formatPrice(detail.nailPrice)} />
      </div>
      {packages.length > 0 ? (
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {packages.map((pkg, index) => (
            <InfoRow
              key={`${pkg.packageType || "PACKAGE"}-${index}`}
              label={packageTypeLabel[pkg.packageType] || pkg.packageType || "패키지"}
              value={formatDiscountRate(pkg.discountRate)}
            />
          ))}
        </div>
      ) : null}
    </DetailSection>
  );
};

const DressDetail = ({ detail }) => {
  if (!detail) return null;
  const items = detail.items || detail.dressItems || [];
  return (
    <DetailSection title="보유 드레스">
      <InfoRow label="보유 사이즈" value={detail.sizeRange || "-"} />
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        {items.slice(0, 12).map((item, index) => (
          <ItemCard key={`${item.itemName}-${index}`} title={item.itemName}>
            <InfoLine label="가격" value={formatPrice(item.price)} />
            <InfoLine label="타입" value={dressTypeLabel[item.itemType] || item.itemType || "-"} />
            <InfoLine label="사이즈" value={item.sizeRange || "-"} />
            <InfoLine label="태그" value={item.styleTags || "-"} />
          </ItemCard>
        ))}
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
        <InfoRow label="홀 유형" value={hallTypeLabel[detail.hallType] || detail.hallType || "-"} />
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        {items.map((item, index) => (
          <ItemCard key={`${item.itemName}-${index}`} title={item.itemName}>
            <InfoLine label="가격" value={formatPrice(item.price)} />
            <InfoLine label="수용 인원" value={item.capacity ? `${Number(item.capacity).toLocaleString()}명` : "-"} />
            <InfoLine label="식사 유형" value={mealTypeLabel[item.mealType] || item.mealType || "-"} />
          </ItemCard>
        ))}
      </div>
    </DetailSection>
  );
};

const StudioDetail = ({ detail }) => {
  if (!detail) return null;
  const tags = Array.isArray(detail.themeTags) ? detail.themeTags.join(", ") : detail.themeTags;
  return (
    <DetailSection title="스튜디오 상세">
      <InfoRow label="테마" value={tags || detail.theme || "-"} />
      {detail.priceRange ? <InfoRow label="가격대" value={detail.priceRange} /> : null}
      {detail.rating ? <InfoRow label="평점" value={detail.rating} /> : null}
    </DetailSection>
  );
};

const DetailSection = ({ title, children }) => (
  <div className="mt-5 rounded-lg border border-slate-200 bg-white p-5">
    <h3 className="mb-3 text-base font-semibold">{title}</h3>
    {children}
  </div>
);

const ItemCard = ({ title, children }) => (
  <div className="rounded-md border border-slate-100 p-4">
    <div className="mb-2 text-sm font-semibold text-slate-800">{title || "항목"}</div>
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
