import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  getDressListForTryOn,
  getMyTryOnPhoto,
  getTryOnImageUrl,
  requestTryOn,
  uploadMyTryOnPhoto,
} from "../../api/aiDressApi";
import { getCompanyImageUrl } from "../../api/companyApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import FetchingModal from "../common/FetchingModal";

const AiDressTryOnComponent = () => {
  const { loginState } = useCustomLogin();
  const [searchParams] = useSearchParams();
  const preselectedId = searchParams.get("dressItemId");

  const [dressList, setDressList] = useState([]);
  const [myPhoto, setMyPhoto] = useState("");
  const [selectedDressId, setSelectedDressId] = useState(
    preselectedId ? Number(preselectedId) : null,
  );
  const [resultImage, setResultImage] = useState("");
  const [loading, setLoading] = useState(false);
  const fileInputRef = useRef(null);

  useEffect(() => {
    getDressListForTryOn({ page: 1, size: 50 })
      .then((data) => {
        setDressList(data.dtoList || []);
      })
      .catch((err) => {
        console.error("드레스 목록 조회 실패:", err);
      });
  }, []);

  useEffect(() => {
    if (!loginState.email) return;

    getMyTryOnPhoto()
      .then((data) => {
        setMyPhoto(data.photoFileName || data.phototoFileName || "");
      })
      .catch(() => {});
  }, [loginState.email]);

  const handlePhotoUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setLoading(true);
    try {
      const data = await uploadMyTryOnPhoto(file);
      setMyPhoto(data.photoFileName || "");
      setResultImage("");
    } catch (err) {
      console.error("사진 업로드 실패:", err);
      alert("사진 업로드에 실패했습니다.");
    } finally {
      setLoading(false);
      e.target.value = "";
    }
  };

  const handleTryOn = async () => {
    if (!loginState.email) {
      alert("로그인 후 이용해주세요.");
      return;
    }
    if (!myPhoto) {
      alert("내 사진을 먼저 업로드해주세요.");
      return;
    }
    if (!selectedDressId) {
      alert("입어볼 드레스를 선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      const data = await requestTryOn({
        dressItemId: selectedDressId,
        photoFileName: myPhoto,
      });
      setResultImage(data.resultImageUrl || "");
    } catch (err) {
      console.error("합성 실패:", err);
      alert(
        err.response?.data?.message ||
          err.response?.data?.error ||
          "합성에 실패했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  if (!loginState.email) {
    return (
      <div className="rounded-2xl border border-line bg-white py-16 text-center text-sm text-ink-muted">
        로그인 후 AI 드레스 체험을 이용할 수 있습니다.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {loading ? <FetchingModal /> : null}

      <section className="rounded-2xl border border-line bg-white p-5">
        <h2 className="mb-3 font-['Gowun_Batang'] text-lg text-ink">1. 내 사진</h2>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="h-56 w-40 overflow-hidden rounded-2xl bg-blush-50">
            {myPhoto ? (
              <img
                src={getTryOnImageUrl(myPhoto)}
                alt="내 사진"
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full items-center justify-center px-3 text-center text-xs text-ink-muted">
                사진 없음
              </div>
            )}
          </div>

          <div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handlePhotoUpload}
            />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="h-10 rounded-full bg-brand px-5 text-sm text-white transition hover:bg-brand-dark"
            >
              사진 업로드
            </button>
            <p className="mt-2 text-xs text-ink-muted">
              정면 전신 또는 상반신 사진이 가장 잘 맞습니다.
            </p>
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-line bg-white p-5">
        <h2 className="mb-3 font-['Gowun_Batang'] text-lg text-ink">2. 드레스 선택</h2>

        {dressList.length === 0 ? (
          <div className="py-10 text-center text-sm text-ink-muted">
            등록된 드레스가 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {dressList.map((dress) => (
              <button
                key={dress.dressItemId}
                type="button"
                onClick={() => setSelectedDressId(dress.dressItemId)}
                className={`overflow-hidden rounded-2xl border text-left transition ${
                  selectedDressId === dress.dressItemId
                    ? "border-brand shadow-sm"
                    : "border-line hover:border-brand"
                }`}
              >
                {dress.imageUrl ? (
                  <img
                    src={getCompanyImageUrl(dress.imageUrl, true)}
                    alt={dress.itemName}
                    className="h-36 w-full object-cover"
                  />
                ) : (
                  <div className="flex h-36 items-center justify-center bg-blush-50 text-xs text-ink-muted">
                    이미지 없음
                  </div>
                )}
                <div className="p-3">
                  <p className="truncate text-sm font-medium text-ink">
                    {dress.itemName}
                  </p>
                  <p className="truncate text-xs text-ink-muted">
                    {dress.companyName}
                  </p>
                  {dress.price > 0 && (
                    <p className="mt-1 text-xs font-medium text-brand">
                      {Number(dress.price).toLocaleString()}원
                    </p>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="rounded-2xl border border-line bg-white p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-['Gowun_Batang'] text-lg text-ink">3. 입혀보기</h2>
          <button
            type="button"
            onClick={handleTryOn}
            disabled={loading}
            className="h-10 rounded-full bg-brand px-5 text-sm text-white transition hover:bg-brand-dark disabled:opacity-50"
          >
            드레스 입혀보기
          </button>
        </div>

        <div className="flex min-h-[280px] items-center justify-center overflow-hidden rounded-2xl bg-blush-50">
          {resultImage ? (
            <img
              src={resultImage.startsWith("http") ? resultImage : getTryOnImageUrl(resultImage)}
              alt="합성 결과"
              className="max-h-[480px] w-full object-contain"
            />
          ) : (
            <p className="px-4 text-center text-sm text-ink-muted">
              사진과 드레스를 선택한 뒤 「드레스 입혀보기」를 눌러주세요.
            </p>
          )}
        </div>
      </section>
    </div>
  );
};

export default AiDressTryOnComponent;
