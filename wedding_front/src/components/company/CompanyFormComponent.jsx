import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
  getCompanyImageUrl,
  getOne,
  postAdd,
  putOne,
  uploadCompanyImages,
} from "../../api/companyApi";
import FetchingModal from "../common/FetchingModal";
import ResultModal from "../common/ResultModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import ShopTapeLabel from "../product/ShopTapeLabel";

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

const inputClass =
  "h-10 w-full rounded-lg border border-line-soft px-3 text-sm text-ink outline-none transition focus:border-brand";
const textareaClass =
  "w-full resize-none rounded-lg border border-line-soft p-3 text-sm text-ink outline-none transition focus:border-brand";

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

  const loadKakaoSdk = () =>
    new Promise((resolve) => {
      if (window.kakao && window.kakao.maps && window.kakao.maps.services) {
        resolve();
        return;
      }
      if (window.kakao && window.kakao.maps) {
        window.kakao.maps.load(resolve);
        return;
      }
      const existing = document.querySelector('script[src*="dapi.kakao.com"]');
      if (existing) {
        existing.addEventListener("load", () =>
          window.kakao.maps.load(resolve),
        );
        return;
      }
      const script = document.createElement("script");
      script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_MAP_KEY}&autoload=false&libraries=services`;
      script.async = true;
      script.onload = () => window.kakao.maps.load(resolve);
      document.head.appendChild(script);
    });

  const handleSearchAddress = async () => {
    if (!window.daum || !window.daum.Postcode) {
      alert(
        "주소 검색 기능을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
      return;
    }
    new window.daum.Postcode({
      oncomplete: async (data) => {
        const fullAddress = data.roadAddress || data.jibunAddress;
        setCompany((prev) => ({ ...prev, address: fullAddress }));
        try {
          await loadKakaoSdk();
          const geocoder = new window.kakao.maps.services.Geocoder();
          geocoder.addressSearch(fullAddress, (result, status) => {
            if (status === window.kakao.maps.services.Status.OK) {
              setCompany((prev) => ({
                ...prev,
                latitude: result[0].y,
                longitude: result[0].x,
              }));
            }
          });
        } catch {
          // 좌표 변환 실패 시 주소만 채워줌
        }
      },
    }).open();
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
    const newUploadFileNames = await uploadCompanyImages(
      uploadRef.current?.files || [],
    );
    return {
      ...company,
      latitude:
        company.latitude === "" || company.latitude === null
          ? null
          : Number(company.latitude),
      longitude:
        company.longitude === "" || company.longitude === null
          ? null
          : Number(company.longitude),
      priceAvg:
        company.priceAvg === "" || company.priceAvg === null
          ? null
          : Number(company.priceAvg),
      uploadFileNames: [
        ...(company.uploadFileNames || []),
        ...newUploadFileNames,
      ],
    };
  };

  const handleSubmit = async () => {
    if (!validate()) {
      return;
    }

    if (isModify) {
      const confirmed = window.confirm("변경된 사항을 저장하겠습니까?");
      if (!confirmed) return;
    }

    try {
      setFetching(true);
      const payload = await buildPayload();
      if (isModify) {
        await putOne(cmno, payload);
        setFetching(false);
        window.location.href = `${companyPathPrefix}/read/${cmno}`;
      } else {
        const data = await postAdd(payload);
        setResult(data.cmno);
      }
    } catch (err) {
      setFetching(false);
      const errorMsg = err?.response?.data?.error || "";
      if (errorMsg === "ERROR_ACCESS_TOKEN") {
        alert("로그인 세션이 만료되었습니다.\n다시 로그인해주세요.");
        window.location.href = "/auth/login";
        return;
      }
      try {
        exceptionHandle(err);
      } catch {
        alert("저장에 실패했습니다. 다시 시도해주세요.");
      }
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

  const goBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }
    navigate({
      pathname: isModify
        ? `${companyPathPrefix}/read/${cmno}`
        : `${companyPathPrefix}/list`,
    });
  };

  return (
    <div className="pb-10 text-ink">
      {fetching ? <FetchingModal /> : null}
      {result ? (
        <ResultModal
          title={isModify ? "업체 수정 결과" : "업체 등록 결과"}
          content={
            isModify
              ? "업체 정보가 수정되었습니다."
              : `${result}번 업체가 등록되었습니다.`
          }
          callbackFn={closeModal}
        />
      ) : null}

      <div className="mx-auto max-w-[900px]">
        <ShopTapeLabel className="mb-4">관리자</ShopTapeLabel>
        <p className="mb-8 font-['Gowun_Batang'] text-2xl">
          {isModify ? "업체 수정" : "업체 등록"}
          {isModify ? (
            <span className="ml-2 text-base text-ink-faint">#{cmno}</span>
          ) : null}
        </p>

        {/* 기본 정보 */}
        <div className="mb-6 rounded-2xl bg-white p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          <p className="mb-4 text-sm font-medium">기본 정보</p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="md:col-span-2">
              <label className="mb-1.5 block text-xs text-ink-soft">
                업체명 *
              </label>
              <input
                name="name"
                type="text"
                value={company.name}
                onChange={handleChange}
                placeholder="예: 그랜드 웨딩홀"
                className={inputClass}
              />
            </div>

            <div>
              <label className="mb-1.5 block text-xs text-ink-soft">
                업체 유형 *
              </label>
              <select
                name="category"
                value={company.category}
                onChange={handleChange}
                className={inputClass}
              >
                <option value="HALL">웨딩홀</option>
                <option value="DRESS">드레스</option>
                <option value="MAKEUP">메이크업</option>
                <option value="STUDIO">스튜디오</option>
              </select>
              <p className="mt-1 text-xs text-ink-faint">{categoryDetail}</p>
            </div>

            <div>
              <label className="mb-1.5 block text-xs text-ink-soft">
                대표자명
              </label>
              <input
                name="ceoName"
                type="text"
                value={company.ceoName || ""}
                onChange={handleChange}
                placeholder="대표자명"
                className={inputClass}
              />
            </div>

            <div>
              <label className="mb-1.5 block text-xs text-ink-soft">
                연락처 *
              </label>
              <input
                name="phone"
                type="text"
                value={company.phone || ""}
                onChange={handleChange}
                placeholder="02-0000-0000"
                className={inputClass}
              />
            </div>

            <div>
              <label className="mb-1.5 block text-xs text-ink-soft">
                평균 가격(원) *
              </label>
              <input
                name="priceAvg"
                type="number"
                value={company.priceAvg ?? ""}
                onChange={handleChange}
                placeholder="1200000"
                className={inputClass}
              />
            </div>

            <div className="md:col-span-2">
              <label className="mb-1.5 block text-xs text-ink-soft">
                업체 소개
              </label>
              <textarea
                name="description"
                rows={5}
                value={company.description || ""}
                onChange={handleChange}
                placeholder="업체 소개를 입력하세요."
                className={textareaClass}
              />
            </div>
          </div>
        </div>

        {/* 위치 */}
        <div className="mb-6 rounded-2xl bg-white p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          <p className="mb-4 text-sm font-medium">위치 정보</p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="md:col-span-2">
              <label className="mb-1.5 block text-xs text-ink-soft">주소 *</label>
              <div className="flex gap-2">
                <input
                  name="address"
                  type="text"
                  value={company.address || ""}
                  onChange={handleChange}
                  placeholder="주소 찾기 버튼을 눌러 검색하세요"
                  readOnly
                  className={`${inputClass} flex-1 bg-cream/40`}
                />
                <button
                  type="button"
                  onClick={handleSearchAddress}
                  className="h-10 shrink-0 rounded-full bg-brand px-4 text-sm font-medium text-white transition hover:bg-brand-dark"
                >
                  주소 찾기
                </button>
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-xs text-ink-soft">위도</label>
              <input
                name="latitude"
                type="number"
                value={company.latitude ?? ""}
                onChange={handleChange}
                placeholder="주소 검색 시 자동 입력"
                className={`${inputClass} bg-cream/40`}
              />
            </div>

            <div>
              <label className="mb-1.5 block text-xs text-ink-soft">경도</label>
              <input
                name="longitude"
                type="number"
                value={company.longitude ?? ""}
                onChange={handleChange}
                placeholder="주소 검색 시 자동 입력"
                className={`${inputClass} bg-cream/40`}
              />
            </div>

            <div className="rounded-xl bg-cream/70 px-4 py-3 md:col-span-2">
              <div className="text-xs text-ink-soft">견적 합계 (평균 가격)</div>
              <div className="mt-1 font-['Gowun_Batang'] text-xl text-ink">
                {company.priceAvg
                  ? `${Number(company.priceAvg).toLocaleString()}원`
                  : "0원"}
              </div>
            </div>
          </div>
        </div>

        {/* 이미지 */}
        <div className="mb-6 rounded-2xl bg-white p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
          <p className="mb-4 text-sm font-medium">업체 이미지</p>

          {isModify && company.uploadFileNames?.length > 0 ? (
            <div className="mb-4 grid grid-cols-3 gap-3 md:grid-cols-4">
              {company.uploadFileNames.map((fileName, index) => (
                <div key={fileName} className="relative">
                  <div className="aspect-square overflow-hidden rounded-lg bg-surface">
                    <img
                      alt=""
                      src={getCompanyImageUrl(fileName, true)}
                      className="h-full w-full object-cover"
                    />
                  </div>
                  {index === 0 ? (
                    <span className="absolute bottom-1.5 left-1.5 rounded-full bg-brand px-2 py-0.5 text-[10px] font-medium text-white">
                      대표
                    </span>
                  ) : null}
                  <button
                    type="button"
                    onClick={() => handleRemoveCurrentImage(fileName)}
                    className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-ink text-xs text-white"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          ) : null}

          <p className="mb-2 text-xs text-ink-soft">새 이미지 추가</p>
          <label className="mb-4 flex min-h-28 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed border-line bg-cream/50 px-4 py-6 text-center transition hover:border-brand hover:bg-blush-50">
            <input
              ref={uploadRef}
              className="hidden"
              type="file"
              accept="image/*"
              multiple
              onChange={handlePreview}
            />
            <span className="text-lg text-ink-soft">+</span>
            <span className="mt-1 text-sm font-medium text-ink">
              업체 이미지 업로드
            </span>
            <span className="mt-1 text-xs text-ink-faint">
              첫 번째 이미지가 대표 이미지로 사용됩니다.
            </span>
          </label>

          {previews.length > 0 ? (
            <div className="grid grid-cols-3 gap-3 md:grid-cols-4">
              {previews.map((preview, index) => (
                <div key={preview} className="relative">
                  <div className="aspect-square overflow-hidden rounded-lg bg-surface">
                    <img
                      alt=""
                      src={preview}
                      className="h-full w-full object-cover"
                    />
                  </div>
                  {index === 0 && !company.uploadFileNames?.length ? (
                    <span className="absolute bottom-1.5 left-1.5 rounded-full bg-brand px-2 py-0.5 text-[10px] font-medium text-white">
                      대표
                    </span>
                  ) : null}
                </div>
              ))}
            </div>
          ) : null}
        </div>

        <div className="flex flex-wrap justify-end gap-2">
          <button
            type="button"
            onClick={goBack}
            className="h-11 rounded-full border border-line-soft px-6 text-sm"
          >
            목록으로
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            className="h-11 rounded-full bg-brand px-6 text-sm font-medium text-white hover:bg-brand-dark"
          >
            {isModify ? "수정 완료" : "업체 저장"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CompanyFormComponent;
