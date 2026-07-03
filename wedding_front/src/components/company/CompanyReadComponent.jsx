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

const adminRoles = ["ADMIN", "ROLE_ADMIN"];

const CompanyReadComponent = () => {
  const { cmno } = useParams();
  const [company, setCompany] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const { exceptionHandle } = useCustomLogin();
  const navigate = useNavigate();
  const loginState = useSelector((state) => state.loginSlice);
  const canManageCompany = loginState.roleNames?.some((roleName) => adminRoles.includes(roleName));

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

  return (
    <section className="mx-auto max-w-5xl p-4 text-slate-800">
      {fetching ? <FetchingModal /> : null}

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <button className="mb-3 rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50" type="button" onClick={() => navigate({ pathname: "/companies/list" })}>
            목록으로
          </button>
          <h2 className="text-2xl font-semibold">{company.name}</h2>
          <p className="mt-1 text-sm text-slate-500">{categoryLabel[company.category] || company.category}</p>
        </div>
        <div className="flex items-center gap-2">
          <div className="rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-700">업체 번호 {company.cmno}</div>
          {canManageCompany ? (
            <>
              <button className="rounded-md border border-blue-200 px-3 py-2 text-sm text-blue-700 hover:bg-blue-50" type="button" onClick={() => navigate({ pathname: `/companies/modify/${company.cmno}` })}>
                수정
              </button>
              <button className="rounded-md border border-red-200 px-3 py-2 text-sm text-red-700 hover:bg-red-50" type="button" onClick={handleDelete}>
                삭제
              </button>
            </>
          ) : null}
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
          {mainImage ? (
            <img className="h-80 w-full object-cover" src={getCompanyImageUrl(mainImage)} alt={company.name} />
          ) : (
            <div className="flex h-80 items-center justify-center bg-slate-100 text-slate-400">대표 이미지가 없습니다.</div>
          )}
          <div className="grid grid-cols-3 gap-2 p-3">
            {(company.uploadFileNames || []).slice(1, 7).map((fileName) => (
              <img key={fileName} className="h-28 w-full rounded-md object-cover" src={getCompanyImageUrl(fileName, true)} alt={company.name} />
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
        <p className="whitespace-pre-wrap text-sm leading-6 text-slate-600">{company.description || "등록된 소개가 없습니다."}</p>
      </div>
    </section>
  );
};

const InfoRow = ({ label, value }) => (
  <div className="border-b border-slate-100 py-3 last:border-b-0">
    <div className="text-xs font-medium uppercase text-slate-400">{label}</div>
    <div className="mt-1 text-sm text-slate-800">{value}</div>
  </div>
);

export default CompanyReadComponent;

