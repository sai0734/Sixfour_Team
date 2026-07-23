import { useEffect, useState } from "react";
import { getCompanyImageUrl } from "../../api/companyApi";
import {
  applyBackground,
  getDressList,
  requestTryOn,
} from "../../api/aiDressApi";
import PageComponent from "../common/PageComponent";
import { showConfirm } from "../../util/globalConfirm";

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
  if (typeof data?.detail === "string" && data.detail) return data.detail;
  if (data?.error === "ERROR_ACCESS_TOKEN") {
    return "로그인이 만료되었습니다. 다시 로그인해 주세요.";
  }
  if (err?.response?.status === 401) {
    return "인증이 필요합니다. 로그아웃 후 다시 로그인해 주세요.";
  }
  return err?.message || "요청 처리에 실패했습니다.";
};

const formatDate = (value) => {
  if (!value) return "";
  try {
    return new Date(value).toLocaleString("ko-KR");
  } catch {
    return String(value);
  }
};

const downloadImage = (dataUrl, fileName) => {
  const link = document.createElement("a");
  link.href = dataUrl;
  link.download = fileName || `ai-dress-${Date.now()}.png`;
  document.body.appendChild(link);
  link.click();
  link.remove();
};

const AiDressTryOnComponent = () => {
  const [dressPage, setDressPage] = useState(initPage);
  const [page, setPage] = useState(1);
  /** 서버 저장 없음 — File + 미리보기 URL만 세션에 유지 */
  const [myPhotoFile, setMyPhotoFile] = useState(null);
  const [myPhotoPreviewUrl, setMyPhotoPreviewUrl] = useState("");
  const [selectedDressId, setSelectedDressId] = useState(null);
  const [backgroundPrompt, setBackgroundPrompt] = useState("");
  const [resultImageUrl, setResultImageUrl] = useState("");
  /** 서버 저장 없음 → 이번 세션 기록만 (브라우저) */
  const [history, setHistory] = useState([]);
  /** 합성 기록에서 선택한 항목 — 있을 때만 '수정하기' 표시 */
  const [selectedHistoryId, setSelectedHistoryId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const [dressLoading, setDressLoading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    return () => {
      if (myPhotoPreviewUrl) URL.revokeObjectURL(myPhotoPreviewUrl);
    };
  }, [myPhotoPreviewUrl]);

  useEffect(() => {
    setDressLoading(true);
    getDressList({ page, size: 12 })
      .then(setDressPage)
      .catch((err) => setMessage(extractErrorMessage(err)))
      .finally(() => setDressLoading(false));
  }, [page]);

  const handlePhotoChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setMessage("");
    if (myPhotoPreviewUrl) URL.revokeObjectURL(myPhotoPreviewUrl);
    setMyPhotoFile(file);
    setMyPhotoPreviewUrl(URL.createObjectURL(file));
    setMessage("사진을 선택했습니다. (서버에 저장되지 않습니다)");
  };

  const handleTryOn = async () => {
    if (!selectedDressId) {
      setMessage("드레스를 선택해주세요.");
      return;
    }
    if (!myPhotoFile) {
      setMessage("합성할 내 사진을 먼저 선택해주세요.");
      return;
    }

    const prompt = backgroundPrompt.trim();
    setMessage("");
    setResultImageUrl("");
    setSelectedHistoryId(null);
    setLoading(true);

    try {
      setMessage(
        prompt
          ? "드레스 합성 중... (완료 후 배경까지 적용한 뒤 표시됩니다)"
          : "드레스 합성 중... (1~2분)",
      );
      const data = await requestTryOn({
        dressItemId: selectedDressId,
        file: myPhotoFile,
      });
      const tryOnUrl =
        data.resultImageUrl ||
        (data.resultImageBase64
          ? `data:image/png;base64,${data.resultImageBase64}`
          : "");
      if (!tryOnUrl) {
        throw new Error("합성 결과 이미지를 받지 못했습니다.");
      }

      const historyId = `local-${Date.now()}`;
      const base64 = data.resultImageBase64 || "";
      let finalUrl = tryOnUrl;
      let finalPrompt = "";

      // 배경 프롬프트가 있으면 중간 결과를 화면에 띄우지 않고, 2번까지 끝난 뒤 한 번만 표시
      if (prompt) {
        setMessage("배경 적용 중... 잠시만 기다려 주세요.");
        const bg = await applyBackground({
          imageBase64: base64 || tryOnUrl,
          backgroundPrompt: prompt,
        });
        const bgUrl =
          bg.resultImageUrl ||
          (bg.resultImageBase64
            ? `data:image/png;base64,${bg.resultImageBase64}`
            : "");
        if (!bgUrl) {
          throw new Error("배경 적용 결과 이미지를 받지 못했습니다.");
        }
        finalUrl = bgUrl;
        finalPrompt = prompt;
      }

      setResultImageUrl(finalUrl);
      setHistory((prev) => [
        {
          historyId,
          tryOnImageUrl: tryOnUrl,
          tryOnImageBase64: base64,
          resultImageUrl: finalUrl,
          dressName: data.dressName || "드레스",
          backgroundPrompt: finalPrompt,
          createdAt: new Date().toISOString(),
        },
        ...prev,
      ]);
      setMessage(
        "합성이 완료되었습니다. 기록을 누르면 수정하기·저장하기를 쓸 수 있습니다.",
      );
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleSelectHistory = (item) => {
    setSelectedHistoryId(item.historyId);
    setResultImageUrl(item.resultImageUrl);
    setBackgroundPrompt(item.backgroundPrompt || "");
    setMessage("기록을 선택했습니다. 배경을 바꾼 뒤 수정하기를 누르거나, 저장하기로 받을 수 있습니다.");
  };

  const handleEdit = async () => {
    const item = history.find((h) => h.historyId === selectedHistoryId);
    if (!item) {
      setMessage("먼저 합성 기록에서 이미지를 선택해 주세요.");
      return;
    }

    const prompt = backgroundPrompt.trim();
    const sourceBase64 =
      item.tryOnImageBase64 ||
      item.tryOnImageUrl ||
      item.resultImageUrl;
    if (!sourceBase64) {
      setMessage("수정할 원본 합성 이미지가 없습니다.");
      return;
    }

    setEditLoading(true);
    setMessage("");
    try {
      if (!prompt) {
        // 프롬프트 비움 → CatVTON 원본으로 되돌림
        const original = item.tryOnImageUrl || item.resultImageUrl;
        setResultImageUrl(original);
        setHistory((prev) =>
          prev.map((h) =>
            h.historyId === item.historyId
              ? { ...h, resultImageUrl: original, backgroundPrompt: "" }
              : h,
          ),
        );
        setMessage("배경을 제거하고 원본 합성으로 되돌렸습니다.");
        return;
      }

      setMessage("수정 적용 중...");
      const bg = await applyBackground({
        imageBase64: sourceBase64,
        backgroundPrompt: prompt,
      });
      const bgUrl =
        bg.resultImageUrl ||
        (bg.resultImageBase64
          ? `data:image/png;base64,${bg.resultImageBase64}`
          : "");
      if (!bgUrl) {
        throw new Error("수정 결과 이미지를 받지 못했습니다.");
      }
      setResultImageUrl(bgUrl);
      setHistory((prev) =>
        prev.map((h) =>
          h.historyId === item.historyId
            ? { ...h, resultImageUrl: bgUrl, backgroundPrompt: prompt }
            : h,
        ),
      );
      setMessage("수정이 반영되었습니다. 저장하기로 기기에 받을 수 있습니다.");
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setEditLoading(false);
    }
  };

  const handleDownload = (url) => {
    const target = url || resultImageUrl;
    if (!target) {
      setMessage("먼저 합성을 완료해 주세요.");
      return;
    }
    downloadImage(target, `ai-dress-${Date.now()}.png`);
    setMessage("이미지를 기기에 저장했습니다.");
  };

  const handleDeleteHistory = async (item) => {
    if (
      !(await showConfirm(
        "이 합성 기록을 삭제할까요? (이 기기 화면에서만 사라집니다)",
      ))
    ) {
      return;
    }
    setHistory((prev) => prev.filter((h) => h.historyId !== item.historyId));
    if (selectedHistoryId === item.historyId) {
      setSelectedHistoryId(null);
    }
    if (resultImageUrl === item.resultImageUrl) {
      setResultImageUrl("");
    }
    setMessage("화면에서 기록을 삭제했습니다.");
  };

  const selectedDress = dressPage.dtoList.find(
    (d) => d.dressItemId === selectedDressId,
  );

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-[minmax(0,1.35fr)_minmax(280px,0.85fr)] lg:items-start">
        <section className="rounded-2xl border border-line bg-white p-5 min-w-0">
          <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="shrink-0">
              <p className="mb-2 text-sm font-medium text-ink">내 사진</p>
              <div className="flex items-end gap-3">
                {myPhotoPreviewUrl ? (
                  <img
                    src={myPhotoPreviewUrl}
                    alt="내 사진"
                    className="h-36 w-28 rounded-xl border border-line object-cover"
                  />
                ) : (
                  <div className="flex h-36 w-28 items-center justify-center rounded-xl border border-dashed border-line text-[11px] text-ink-faint">
                    사진 없음
                  </div>
                )}
                <div className="space-y-2">
                  <label className="inline-flex cursor-pointer rounded-full border border-line px-4 py-2 text-sm hover:border-brand">
                    선택
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={handlePhotoChange}
                    />
                  </label>
                  {selectedDress && (
                    <p className="max-w-[140px] text-[11px] text-ink-faint">
                      선택: {selectedDress.itemName}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>

          <p className="mb-3 text-sm font-medium text-ink">드레스 선택</p>
          {dressLoading ? (
            <p className="text-sm text-ink-faint">드레스 불러오는 중...</p>
          ) : (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
              {dressPage.dtoList.map((dress) => {
                const selected = dress.dressItemId === selectedDressId;
                return (
                  <button
                    key={dress.dressItemId}
                    type="button"
                    onClick={() => setSelectedDressId(dress.dressItemId)}
                    className={`overflow-hidden rounded-xl border text-left transition ${
                      selected
                        ? "border-brand ring-2 ring-brand/30"
                        : "border-line hover:border-brand/50"
                    }`}
                  >
                    <img
                      src={getCompanyImageUrl(dress.imageUrl)}
                      alt={dress.itemName}
                      className="aspect-[3/4] w-full object-cover"
                    />
                    <div className="space-y-0.5 p-2">
                      <p className="truncate text-xs font-medium text-ink">
                        {dress.itemName}
                      </p>
                      <p className="truncate text-[10px] text-ink-faint">
                        {dress.companyName}
                      </p>
                    </div>
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

        <section className="rounded-2xl border border-line bg-white p-5 lg:sticky lg:top-24">
          <p className="mb-3 text-sm font-medium text-ink">합성 결과</p>

          <div className="mb-4 overflow-hidden rounded-xl border border-line bg-cream">
            {resultImageUrl ? (
              <img
                src={resultImageUrl}
                alt="합성 결과"
                className="mx-auto max-h-[360px] w-full object-contain"
              />
            ) : (
              <div className="flex h-[280px] flex-col items-center justify-center gap-1.5 px-4 text-center text-xs text-ink-faint">
                <span>결과가 여기에 표시됩니다</span>
                <span className="text-[10px] text-ink-faint/80">
                  사진 선택 → 드레스 선택 → 입어보기
                </span>
              </div>
            )}
          </div>

          <label className="mb-1.5 block text-xs text-ink-muted">
            배경 프롬프트 (선택)
          </label>
          <textarea
            value={backgroundPrompt}
            onChange={(e) => setBackgroundPrompt(e.target.value)}
            rows={3}
            maxLength={800}
            placeholder="예: 석양이 지는 해변 웨딩홀"
            className="mb-2 w-full resize-y rounded-xl border border-line px-3 py-2 text-sm outline-none focus:border-brand"
          />
          <p className="mb-4 text-[11px] leading-relaxed text-ink-faint">
            합성 후 아래 기록을 누르면 수정하기·저장하기를 쓸 수 있습니다.
          </p>

          <div className="flex flex-col gap-2">
            <button
              type="button"
              onClick={handleTryOn}
              disabled={loading || editLoading}
              className="w-full rounded-full bg-brand px-4 py-2.5 text-sm font-medium text-white disabled:opacity-50"
            >
              {loading ? "처리 중... (1~2분)" : "입어보기"}
            </button>
            {selectedHistoryId && (
              <>
                <button
                  type="button"
                  onClick={handleEdit}
                  disabled={loading || editLoading}
                  className="w-full rounded-full border border-brand bg-brand/5 px-4 py-2.5 text-sm font-medium text-brand-deep hover:bg-brand/10 disabled:opacity-40"
                >
                  {editLoading ? "수정 중..." : "수정하기"}
                </button>
                <button
                  type="button"
                  onClick={() => handleDownload()}
                  disabled={!resultImageUrl || loading || editLoading}
                  className="w-full rounded-full border border-line px-4 py-2.5 text-sm font-medium text-ink hover:border-brand disabled:opacity-40"
                >
                  저장하기
                </button>
              </>
            )}
          </div>

          {message && (
            <p className="mt-3 text-xs leading-relaxed text-ink-muted">
              {message}
            </p>
          )}
        </section>
      </div>

      <section className="rounded-2xl border border-line bg-white p-5">
        <div className="mb-3 flex items-center justify-between gap-3">
          <p className="text-sm font-medium text-ink">합성 기록</p>
          <p className="text-[11px] text-ink-faint">
            {history.length > 0
              ? `${history.length}건 · 이 세션만 (서버 저장 없음)`
              : "아직 기록이 없습니다"}
          </p>
        </div>

        {history.length === 0 ? (
          <p className="text-sm text-ink-faint">
            입어보기를 실행하면 결과가 여기에 쌓입니다. 새로고침 시 사라집니다.
          </p>
        ) : (
          <div className="-mx-1 flex gap-3 overflow-x-auto px-1 pb-2">
            {history.map((item) => {
              const selected = selectedHistoryId === item.historyId;
              return (
              <div
                key={item.historyId}
                className={`w-[200px] shrink-0 rounded-xl border p-2.5 ${
                  selected
                    ? "border-brand ring-2 ring-brand/30"
                    : "border-line"
                }`}
              >
                <button
                  type="button"
                  onClick={() => handleSelectHistory(item)}
                  className="w-full text-left"
                >
                  <img
                    src={item.resultImageUrl}
                    alt={item.dressName || "합성 결과"}
                    className="mb-2 h-36 w-full rounded-lg object-cover"
                  />
                </button>
                <p className="truncate text-xs font-medium text-ink">
                  {item.dressName || "드레스"}
                </p>
                <p className="mt-0.5 text-[10px] text-ink-faint">
                  {formatDate(item.createdAt)}
                </p>
                <p className="mt-1 line-clamp-2 text-[10px] text-ink-faint">
                  {item.backgroundPrompt
                    ? item.backgroundPrompt
                    : "배경 프롬프트 없음"}
                </p>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  <button
                    type="button"
                    onClick={() => handleDownload(item.resultImageUrl)}
                    className="rounded-full border border-line px-2.5 py-1 text-[11px] hover:border-brand"
                  >
                    저장하기
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDeleteHistory(item)}
                    className="rounded-full border border-red-200 px-2.5 py-1 text-[11px] text-red-600 hover:border-red-400"
                  >
                    삭제
                  </button>
                </div>
              </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};

export default AiDressTryOnComponent;
