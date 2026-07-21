import { useEffect, useState } from "react";
import { getCompanyImageUrl } from "../../api/companyApi";
import {
  deleteTryOnHistory,
  getDressList,
  getMyPhoto,
  getTryOnHistory,
  requestTryOn,
  updateTryOnHistory,
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

const AiDressTryOnComponent = () => {
  const [dressPage, setDressPage] = useState(initPage);
  const [page, setPage] = useState(1);
  const [myPhotoFileName, setMyPhotoFileName] = useState("");
  const [selectedDressId, setSelectedDressId] = useState(null);
  const [backgroundPrompt, setBackgroundPrompt] = useState("");
  const [resultImageUrl, setResultImageUrl] = useState("");
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dressLoading, setDressLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editPrompt, setEditPrompt] = useState("");
  const [historyBusyId, setHistoryBusyId] = useState(null);

  const loadHistory = () =>
    getTryOnHistory()
      .then((data) => setHistory(Array.isArray(data) ? data : []))
      .catch(() => setHistory([]));

  const handleStartEdit = (item) => {
    setEditingId(item.historyId);
    setEditPrompt(item.backgroundPrompt || "");
    setResultImageUrl(item.resultImageUrl);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditPrompt("");
  };

  const handleSaveEdit = async (historyId) => {
    setHistoryBusyId(historyId);
    setMessage("");
    try {
      const updated = await updateTryOnHistory(historyId, {
        backgroundPrompt: editPrompt,
      });
      setResultImageUrl(updated.resultImageUrl);
      setEditingId(null);
      setEditPrompt("");
      setMessage(
        editPrompt.trim()
          ? "배경 프롬프트를 수정해 다시 적용했습니다."
          : "배경을 제거하고 CatVTON 원본으로 되돌렸습니다.",
      );
      await loadHistory();
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setHistoryBusyId(null);
    }
  };

  const handleDeleteHistory = async (item) => {
    if (!window.confirm("이 합성 기록을 삭제할까요?")) return;
    setHistoryBusyId(item.historyId);
    setMessage("");
    try {
      await deleteTryOnHistory(item.historyId);
      if (resultImageUrl === item.resultImageUrl) {
        setResultImageUrl("");
      }
      if (editingId === item.historyId) {
        handleCancelEdit();
      }
      setMessage("합성 기록을 삭제했습니다.");
      await loadHistory();
    } catch (err) {
      setMessage(extractErrorMessage(err));
    } finally {
      setHistoryBusyId(null);
    }
  };

  useEffect(() => {
    getMyPhoto()
      .then((data) => setMyPhotoFileName(data.photoFileName || ""))
      .catch(() => setMyPhotoFileName(""));
    loadHistory();
  }, []);

  useEffect(() => {
    setDressLoading(true);
    getDressList({ page, size: 12 })
      .then(setDressPage)
      .catch((err) => setMessage(extractErrorMessage(err)))
      .finally(() => setDressLoading(false));
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
        backgroundPrompt,
      });
      setResultImageUrl(data.resultImageUrl);
      setMessage(
        backgroundPrompt.trim()
          ? "드레스 합성 + 배경 적용이 완료되었습니다. 기록에 저장했습니다."
          : "AI 합성이 완료되었습니다. 기록에 저장했습니다.",
      );
      await loadHistory();
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
        {dressLoading && dressPage.dtoList.length === 0 ? (
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
        <label className="mb-2 block text-sm text-ink-muted">
          배경 프롬프트 (선택)
        </label>
        <textarea
          value={backgroundPrompt}
          onChange={(e) => setBackgroundPrompt(e.target.value)}
          rows={3}
          maxLength={800}
          placeholder="예: 석양이 지는 해변 웨딩홀, 부드러운 노을빛 / 벚꽃이 흩날리는 정원"
          className="mb-4 w-full resize-y rounded-xl border border-line px-3 py-2 text-sm outline-none focus:border-brand"
        />
        <p className="mb-4 text-xs text-ink-faint">
          비우면 CatVTON 드레스 합성만 합니다. 입력하면 합성 후 OpenAI로 배경을
          바꿉니다. (OPENAI_API_KEY 필요)
        </p>
        <button
          type="button"
          onClick={handleTryOn}
          disabled={loading}
          className="rounded-full bg-brand px-6 py-2.5 text-sm font-medium text-white disabled:opacity-50"
        >
          {loading ? "처리 중... (1~2분 소요될 수 있음)" : "입어보기"}
        </button>
        {message && <p className="mt-3 text-sm text-ink-muted">{message}</p>}
        {resultImageUrl && (
          <img
            src={resultImageUrl}
            alt="합성 결과"
            className="mt-4 max-h-[480px] rounded-2xl border border-line object-contain"
          />
        )}
      </section>

      <section className="rounded-2xl border border-line bg-white p-6">
        <TapeLabel className="mb-4">합성 기록</TapeLabel>
        {history.length === 0 ? (
          <p className="text-sm text-ink-faint">
            아직 저장된 합성 사진이 없습니다. 입어보기를 실행하면 여기에
            쌓입니다.
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {history.map((item) => {
              const busy = historyBusyId === item.historyId;
              const editing = editingId === item.historyId;
              return (
                <div
                  key={item.historyId}
                  className="rounded-2xl border border-line p-3"
                >
                  <button
                    type="button"
                    onClick={() => setResultImageUrl(item.resultImageUrl)}
                    className="w-full text-left"
                  >
                    <img
                      src={item.resultImageUrl}
                      alt={item.dressName || "합성 결과"}
                      className="mb-2 h-40 w-full rounded-xl object-cover"
                    />
                  </button>
                  <p className="truncate text-sm font-medium text-ink">
                    {item.dressName || "드레스"}
                  </p>
                  <p className="mt-1 text-[11px] text-ink-faint">
                    {formatDate(item.createdAt)}
                  </p>

                  {editing ? (
                    <div className="mt-3 space-y-2">
                      <textarea
                        value={editPrompt}
                        onChange={(e) => setEditPrompt(e.target.value)}
                        rows={3}
                        maxLength={800}
                        disabled={busy}
                        placeholder="새 배경 프롬프트 (비우면 원본으로 복원)"
                        className="w-full resize-y rounded-xl border border-line px-3 py-2 text-sm outline-none focus:border-brand"
                      />
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => handleSaveEdit(item.historyId)}
                          className="rounded-full bg-brand px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50"
                        >
                          {busy ? "적용 중..." : "저장"}
                        </button>
                        <button
                          type="button"
                          disabled={busy}
                          onClick={handleCancelEdit}
                          className="rounded-full border border-line px-3 py-1.5 text-xs disabled:opacity-50"
                        >
                          취소
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <p className="mt-1 line-clamp-2 text-xs text-ink-faint">
                        {item.backgroundPrompt
                          ? item.backgroundPrompt
                          : "배경 프롬프트 없음"}
                      </p>
                      <div className="mt-3 flex flex-wrap gap-2">
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => handleStartEdit(item)}
                          className="rounded-full border border-line px-3 py-1.5 text-xs hover:border-brand disabled:opacity-50"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => handleDeleteHistory(item)}
                          className="rounded-full border border-red-200 px-3 py-1.5 text-xs text-red-600 hover:border-red-400 disabled:opacity-50"
                        >
                          {busy ? "삭제 중..." : "삭제"}
                        </button>
                      </div>
                    </>
                  )}
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
