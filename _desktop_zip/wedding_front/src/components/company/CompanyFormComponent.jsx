import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getCompanyImageUrl, getOne, postAdd, putOne, uploadCompanyImages } from "../../api/companyApi";
import FetchingModal from "../common/FetchingModal";
import ResultModal from "../common/ResultModal";
import useCustomLogin from "../../hooks/useCustomLogin";

const initState = {
  category: "HALL",
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

const categoryLabels = {
  HALL: "웨딩홀",
  DRESS: "드레스",
  MAKEUP: "메이크업",
  STUDIO: "스튜디오",
};

const CompanyFormComponent = ({ mode = "add" }) => {
  const { cmno } = useParams();
  const isModify = mode === "modify";
  const [company, setCompany] = useState({ ...initState });
  const [fetching, setFetching] = useState(false);
  const [result, setResult] = useState(null);
  const [previews, setPreviews] = useState([]);
  const uploadRef = useRef();
  const navigate = useNavigate();
  const location = useLocation();
  const { exceptionHandle } = useCustomLogin();
  // 관리자 경로에서 등록/수정한 뒤에는 관리자용 업체 경로로 돌아갑니다.
  const companyPathPrefix = location.pathname.startsWith("/admin/companies")
    ? "/admin/companies"
    : "/companies";

  useEffect(() => {
    if (!isModify) {
      return;
    }

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
  }, [cmno, isModify]);

  const categoryDetail = useMemo(() => {
    const label = categoryLabels[company.category] || "업체";
    return `${label} 업체의 기본 정보와 이미지를 저장합니다.`;
  }, [company.category]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCompany((prev) => ({ ...prev, [name]: value }));
  };

  const handlePreview = (e) => {
    const files = Array.from(e.target.files || []);
    setPreviews(files.slice(0, 6).map((file) => URL.createObjectURL(file)));
  };

  const handleRemoveCurrentImage = (fileName) => {
    setCompany((prev) => ({
      ...prev,
      uploadFileNames: prev.uploadFileNames.filter((name) => name !== fileName),
    }));
  };

  const validate = () => {
    if (!company.name.trim()) {
      alert("업체명을 입력해주세요.");
      return false;
    }
    if (!company.phone.trim()) {
      alert("연락처를 입력해주세요.");
      return false;
    }
    if (!company.address.trim()) {
      alert("주소를 입력해주세요.");
      return false;
    }
    if (company.priceAvg === "") {
      alert("평균 가격을 입력해주세요.");
      return false;
    }
    return true;
  };

  const buildPayload = async () => {
    const newUploadFileNames = await uploadCompanyImages(uploadRef.current?.files || []);
    return {
      ...company,
      latitude: company.latitude === "" || company.latitude === null ? null : Number(company.latitude),
      longitude: company.longitude === "" || company.longitude === null ? null : Number(company.longitude),
      priceAvg: company.priceAvg === "" || company.priceAvg === null ? null : Number(company.priceAvg),
      uploadFileNames: [...(company.uploadFileNames || []), ...newUploadFileNames],
    };
  };

  const handleSubmit = async () => {
    if (!validate()) {
      return;
    }

    try {
      setFetching(true);
      const payload = await buildPayload();
      if (isModify) {
        await putOne(cmno, payload);
        setResult(cmno);
      } else {
        const data = await postAdd(payload);
        setResult(data.cmno);
      }
    } catch (err) {
      exceptionHandle(err);
    } finally {
      setFetching(false);
    }
  };

  const closeModal = () => {
    const targetCmno = result || cmno;
    setResult(null);
    navigate({
      pathname: isModify
        ? `${companyPathPrefix}/read/${targetCmno}`
        : `${companyPathPrefix}/list`,
    });
  };

  return (
    <section className="mx-auto max-w-4xl p-4 text-slate-800">
      {fetching ? <FetchingModal /> : null}
      {result ? (
        <ResultModal
          title={isModify ? "업체 수정 결과" : "업체 등록 결과"}
          content={isModify ? "업체 정보가 수정되었습니다." : `${result}번 업체가 등록되었습니다.`}
          callbackFn={closeModal}
        />
      ) : null}

      <div className="mb-7 flex items-center gap-3">
        <button
          type="button"
          className="flex h-9 w-9 items-center justify-center rounded-md border border-slate-300 text-slate-600 hover:bg-slate-50"
          onClick={() =>
            navigate({
              pathname: isModify
                ? `${companyPathPrefix}/read/${cmno}`
                : `${companyPathPrefix}/list`,
            })
          }
          title="뒤로"
        >
          {"<"}
        </button>
        <div>
          <h2 className="text-xl font-semibold">{isModify ? "업체 수정" : "업체 등록"}</h2>
          <p className="mt-1 text-sm text-slate-500">업체 관리자 권한으로 업체 정보를 저장합니다.</p>
        </div>
      </div>

      <div className="mb-5 flex border-b border-slate-200 text-sm">
        <div className="border-b-2 border-blue-600 px-4 py-2 font-medium text-blue-700">기본 정보</div>
        <div className="px-4 py-2 text-slate-500">위치 정보</div>
        <div className="px-4 py-2 text-slate-500">이미지</div>
      </div>

      <FormSection title="업체 기본 정보">
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="업체명" required>
            <input className="form-input" name="name" type="text" value={company.name} onChange={handleChange} placeholder="예: 그랜드 웨딩홀" />
          </Field>
          <Field label="업체 유형" required>
            <select className="form-input" name="category" value={company.category} onChange={handleChange}>
              <option value="HALL">웨딩홀</option>
              <option value="DRESS">드레스</option>
              <option value="MAKEUP">메이크업</option>
              <option value="STUDIO">스튜디오</option>
            </select>
            <p className="mt-1 text-xs text-slate-500">{categoryDetail}</p>
          </Field>
          <Field label="대표자명">
            <input className="form-input" name="ceoName" type="text" value={company.ceoName || ""} onChange={handleChange} placeholder="대표자명" />
          </Field>
          <Field label="연락처" required>
            <input className="form-input" name="phone" type="text" value={company.phone || ""} onChange={handleChange} placeholder="02-0000-0000" />
          </Field>
          <Field label="업체 소개" full>
            <textarea className="form-textarea" name="description" value={company.description || ""} onChange={handleChange} placeholder="업체 소개를 입력하세요." />
          </Field>
        </div>
      </FormSection>

      <FormSection title="위치 및 견적 정보">
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="주소" required full>
            <input className="form-input" name="address" type="text" value={company.address || ""} onChange={handleChange} placeholder="상세 주소" />
          </Field>
          <Field label="위도">
            <input className="form-input" name="latitude" type="number" value={company.latitude ?? ""} onChange={handleChange} placeholder="37.5665" />
          </Field>
          <Field label="경도">
            <input className="form-input" name="longitude" type="number" value={company.longitude ?? ""} onChange={handleChange} placeholder="126.9780" />
          </Field>
          <Field label="평균 가격" required>
            <input className="form-input" name="priceAvg" type="number" value={company.priceAvg ?? ""} onChange={handleChange} placeholder="1200000" />
          </Field>
          <div className="rounded-md bg-slate-50 p-4">
            <div className="text-sm text-slate-500">견적 합계</div>
            <div className="mt-1 text-xl font-semibold">{company.priceAvg ? Number(company.priceAvg).toLocaleString() : "0"}원</div>
          </div>
        </div>
      </FormSection>

      <FormSection title="이미지 관리">
        {isModify && company.uploadFileNames?.length > 0 ? (
          <div className="mb-4">
            <div className="mb-2 text-sm font-medium text-slate-600">등록된 이미지</div>
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              {company.uploadFileNames.map((fileName, index) => (
                <div key={fileName} className="relative aspect-square overflow-hidden rounded-md border border-slate-200 bg-slate-100">
                  <img className="h-full w-full object-cover" src={getCompanyImageUrl(fileName, true)} alt={`current ${index + 1}`} />
                  {index === 0 ? <span className="absolute bottom-1 left-1 rounded bg-blue-600 px-1.5 py-0.5 text-xs text-white">대표</span> : null}
                  <button className="absolute right-1 top-1 rounded bg-white px-2 py-1 text-xs text-red-600 shadow" type="button" onClick={() => handleRemoveCurrentImage(fileName)}>
                    삭제
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        <div className="grid gap-4 md:grid-cols-2">
          <label className="flex min-h-36 cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed border-slate-300 bg-slate-50 p-6 text-center hover:border-blue-400 hover:bg-blue-50">
            <input ref={uploadRef} className="hidden" type="file" accept="image/*" multiple onChange={handlePreview} />
            <span className="text-2xl">+</span>
            <span className="mt-1 text-sm font-medium">업체 이미지 업로드</span>
            <span className="mt-1 text-xs text-slate-500">첫 번째 이미지가 대표 이미지로 사용됩니다.</span>
          </label>
          <div>
            <div className="mb-2 text-sm font-medium text-slate-600">새 이미지 미리보기</div>
            <div className="grid grid-cols-3 gap-2">
              {previews.length > 0 ? (
                previews.map((preview, index) => (
                  <div key={preview} className="relative aspect-square overflow-hidden rounded-md border border-slate-200 bg-slate-100">
                    <img className="h-full w-full object-cover" src={preview} alt={`preview ${index + 1}`} />
                    {index === 0 && !company.uploadFileNames?.length ? <span className="absolute bottom-1 left-1 rounded bg-blue-600 px-1.5 py-0.5 text-xs text-white">대표</span> : null}
                  </div>
                ))
              ) : (
                <div className="col-span-3 flex h-28 items-center justify-center rounded-md bg-slate-50 text-sm text-slate-400">선택한 이미지가 없습니다.</div>
              )}
            </div>
          </div>
        </div>
      </FormSection>

      <div className="mt-5 flex items-center justify-between border-t border-slate-200 pt-5">
        <button
          className="rounded-md border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
          type="button"
          onClick={() =>
            navigate({
              pathname: isModify
                ? `${companyPathPrefix}/read/${cmno}`
                : `${companyPathPrefix}/list`,
            })
          }
        >
          취소
        </button>
        <button className="rounded-md bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700" type="button" onClick={handleSubmit}>
          {isModify ? "업체 수정" : "업체 저장"}
        </button>
      </div>
    </section>
  );
};

const FormSection = ({ title, children }) => (
  <section className="mb-5 rounded-lg border border-slate-200 bg-white p-5">
    <div className="mb-5 border-b border-slate-100 pb-3">
      <h3 className="text-base font-semibold">{title}</h3>
    </div>
    {children}
  </section>
);

const Field = ({ label, required = false, full = false, children }) => (
  <label className={`block ${full ? "md:col-span-2" : ""}`}>
    <div className="mb-1 text-sm font-medium text-slate-600">
      {label}
      {required ? <span className="ml-1 text-red-500">*</span> : null}
    </div>
    {children}
  </label>
);

export default CompanyFormComponent;
