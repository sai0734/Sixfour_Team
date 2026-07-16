import { useEffect, useState } from "react";
import { getCompanyImageUrl } from "../../api/companyApi";
import {
  getDressList,
  getMyPhoto,
  requestTryOn,
  uploadMyPhoto,
} from "../../api/aiDressApi";
import PageComponent from "../common/PageComponent";
import TapeLabel from "../common/TapeLabel";

const initPage = {
  dtoList: [],
  pageNumList: [],
  pageRequestDTO: null,
  prev: false,
  next: false,
  totalCount: 0,
  prevPage: 0,
  nextPage: 0,
  totalPage: 0,
  current: 0,
};

const extractErrorMessage = (err) => {
  const data = err?.response?.data;
  if (typeof data?.msg === "string" && data.msg) return data.msg;
  if (typeof data?.message === "string" && data.message) return data.message;
  if (data?.error === "ERROR_ACCESS_TOKEN") {
    return "로그인이 만료되었습니다. 다시 로그인해 주세요.";
  }
  if (err?.response?.status === 401) {
    return "인증이 필요합니다. 로그아웃 후 다시 로그인해 주세요.";
  }
  return err?.message || "요청 처리에 실패했습니다.";
};

const AiDressTryOnComponent = () => {
  const [dressPage, setDressPage] = useState(initPage);
  const [page, setPage] = useState(1);
  const [myPhotoFileName, setMyPhotoFileName] = useState("");
  const [selectedDressId, setSelectedDressId] = useState(null);
  const [resultImageUrl, setResultImageUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    getMyPhoto()
      .then((data) => setMyPhotoFileName(data.photoFileName || ""))
      .catch(() => setMyPhotoFileName(""));
  }, []);

  useEffect(() => {
    setLoading(true);
    getDressList({ page, size: 12 })
      .then(setDressPage)
      .catch((err) => setMessage(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [page]);

  const handlePhotoChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setMessage("");
    try {
      const data = await uploadMyPhoto(file);
      setMyPhotoFileName(data.photoFileName);
      setMessage("내 사진이 저장되었습니다.");
    } catch (err) {
      setMessage(extractErrorMessage(err));
    }
  };

  const handleTryOn = async () => {
    if (!selectedDressId) {
      setMessage("드레스를 선택해주세요.");
      return;
    }
    if (!myPhotoFileName) {
      setMessage("합성할 내 사진을 먼저 업로드해주세요.");
      return;
    }

    setMessage("");
    setResultImageUrl("");
    setLoading(true);

    try {
      const data = await requestTryOn({
        dressItemId: selectedDressId,
        photoFileName: myPhotoFileName,
      });
      setResultImageUrl(data.resultImageUrl);
      setMessage("AI 합성이 완료되었습니다.");
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-line bg-white p-6">
        <TapeLabel className="mb-4">내 사진</TapeLabel>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          {myPhotoFileName ? (
            <img
              src={getCompanyImageUrl(myPhotoFileName)}
              alt="내 사진"
              className="h-48 w-36 rounded-xl border border-line object-cover"
            />
          ) : (
            <div className="flex h-48 w-36 items-center justify-center rounded-xl border border-dashed border-line text-xs text-ink-faint">
              사진 없음
            </div>
          )}
          <label className="inline-flex cursor-pointer rounded-full border border-line px-4 py-2 text-sm hover:border-brand">
            사진 업로드
            <input
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handlePhotoChange}
            />
          </label>
        </div>
      </section>

      <section className="rounded-2xl border border-line bg-white p-6">
        <TapeLabel className="mb-4">드레스 선택</TapeLabel>
        {loading && dressPage.dtoList.length === 0 ? (
          <p className="text-sm text-ink-faint">불러오는 중...</p>
        ) : dressPage.dtoList.length === 0 ? (
          <p className="text-sm text-ink-faint">등록된 드레스가 없습니다.</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {dressPage.dtoList.map((dress) => {
              const selected = selectedDressId === dress.dressItemId;
              return (
                <button
                  key={dress.dressItemId}
                  type="button"
                  onClick={() => setSelectedDressId(dress.dressItemId)}
                  className={`rounded-2xl border p-3 text-left transition-colors ${
                    selected
                      ? "border-brand bg-brand/5"
                      : "border-line hover:border-brand/50"
                  }`}
                >
                  {dress.imageUrl ? (
                    <img
                      src={getCompanyImageUrl(dress.imageUrl)}
                      alt={dress.itemName}
                      className="mb-2 h-40 w-full rounded-xl object-cover"
                    />
                  ) : (
                    <div className="mb-2 flex h-40 items-center justify-center rounded-xl bg-cream text-xs text-ink-faint">
                      이미지 없음
                    </div>
                  )}
                  <p className="text-sm font-medium text-ink">{dress.itemName}</p>
                  <p className="text-xs text-ink-faint">{dress.companyName}</p>
                </button>
              );
            })}
          </div>
        )}

        <div className="mt-4">
          <PageComponent
            serverData={dressPage}
            movePage={({ page: nextPage }) => setPage(nextPage)}
          />
        </div>
      </section>

      <section className="rounded-2xl border border-line bg-white p-6">
        <TapeLabel className="mb-4">AI 합성</TapeLabel>
        <button
          type="button"
          onClick={handleTryOn}
          disabled={loading}
          className="rounded-full bg-brand px-6 py-2.5 text-sm font-medium text-white disabled:opacity-50"
        >
          {loading ? "처리 중..." : "입어보기"}
        </button>
        {message && (
          <p className="mt-3 text-sm text-ink-muted">{message}</p>
        )}
        {resultImageUrl && (
          <img
            src={resultImageUrl}
            alt="합성 결과"
            className="mt-4 max-h-[480px] rounded-2xl border border-line object-contain"
          />
        )}
      </section>
    </div>
  );
};

export default AiDressTryOnComponent;
