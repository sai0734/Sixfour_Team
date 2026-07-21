import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getListByMember,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/checklistApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import ChecklistFormModal, { STAGE_LABELS } from "./ChecklistFormModal";

const ListComponent = () => {
  const { loginState } = useCustomLogin();
  const navigate = useNavigate();

  const [checklist, setChecklist] = useState([]);
  const [refresh, setRefresh] = useState(false);

  const [modalMode, setModalMode] = useState(null); // null | "add" | "edit"
  const [editTarget, setEditTarget] = useState(null);
  const [openMenuId, setOpenMenuId] = useState(null);

  useEffect(() => {
    if (!loginState.email) {
      return;
    }

    getListByMember(loginState.email).then((data) => {
      setChecklist(data);
    });
  }, [loginState.email, refresh]);

  const handleToggleDone = (item) => {
    putOne({ ...item, done: !item.done })
      .then(() => setRefresh(!refresh))
      .catch((e) => console.error(e));
  };

  const handleDelete = (checklistId) => {
    setOpenMenuId(null);

    deleteOne(checklistId)
      .then(() => setRefresh(!refresh))
      .catch((e) => console.error(e));
  };

  const openAddModal = () => {
    setEditTarget(null);
    setModalMode("add");
  };

  const openEditModal = (item) => {
    setOpenMenuId(null);
    setEditTarget(item);
    setModalMode("edit");
  };

  const closeModal = () => {
    setModalMode(null);
    setEditTarget(null);
  };

  const handleSubmitModal = (formValues) => {
    if (modalMode === "add") {
      const payload = {
        ...formValues,
        memberEmail: loginState.email,
        done: false,
        dueDate: null,
      };

      postAdd(payload)
        .then(() => {
          closeModal();
          setRefresh(!refresh);
        })
        .catch((e) => console.error(e));
    } else {
      // 수정: 같은 단계 안에 이미 그 순서(sortOrder)를 쓰는 항목이 있으면
      // 그 항목한테 내 원래 순서를 넘겨주는 방식으로 서로 스왑
      const targetStage = formValues.stage;
      const targetOrder = formValues.sortOrder;

      const conflictItem = checklist.find(
        (i) =>
          i.stage === targetStage &&
          i.sortOrder === targetOrder &&
          i.checklistId !== editTarget.checklistId,
      );

      const mainPayload = { ...editTarget, ...formValues };

      if (conflictItem) {
        const conflictPayload = {
          ...conflictItem,
          sortOrder: editTarget.sortOrder,
        };

        Promise.all([putOne(mainPayload), putOne(conflictPayload)])
          .then(() => {
            closeModal();
            setRefresh(!refresh);
          })
          .catch((e) => console.error(e));
      } else {
        putOne(mainPayload)
          .then(() => {
            closeModal();
            setRefresh(!refresh);
          })
          .catch((e) => console.error(e));
      }
    }
  };

  if (!loginState.email) {
    return (
      <div className="p-10 text-center text-ink-faint">
        로그인 후 이용해주세요.
      </div>
    );
  }

  const doneCount = checklist.filter((i) => i.done).length;
  const total = checklist.length;
  const percent = total === 0 ? 0 : Math.round((doneCount / total) * 100);

  // stage별로 그룹핑 (없는 stage는 자동으로 안 나타남)
  const stageNumbers = Object.keys(STAGE_LABELS)
    .map(Number)
    .sort((a, b) => a - b);

  const grouped = stageNumbers.map((stageNum) => {
    const items = checklist
      .filter((item) => item.stage === stageNum)
      .sort((a, b) => a.sortOrder - b.sortOrder);
    const doneInStage = items.filter((i) => i.done).length;
    return { stageNum, items, doneInStage };
  });

  return (
    <div className="pb-20">
      <div className="flex items-center justify-between mb-6">
        <p className="text-lg font-medium text-ink">체크리스트</p>
        <button
          type="button"
          onClick={openAddModal}
          className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
        >
          + 항목 추가
        </button>
      </div>

      <p className="mb-6 rounded-xl bg-blush-50 px-4 py-3 text-xs text-brand-deep">
        마이페이지 · 예약 현황에서 업체 예약이 "결제대기" 상태가 되면, 2단계 업체 계약 항목이
        자동으로 여기에 추가돼요.
      </p>

      {/* 진행률 카드 */}
      <div className="bg-white rounded-2xl border border-line p-5 mb-8 flex items-center gap-5">
        <div className="relative w-16 h-16 shrink-0">
          <svg viewBox="0 0 36 36" className="w-16 h-16 -rotate-90">
            <circle
              cx="18"
              cy="18"
              r="16"
              fill="none"
              stroke="#F1EFE8"
              strokeWidth="4"
            />
            <circle
              cx="18"
              cy="18"
              r="16"
              fill="none"
              stroke="#D4537E"
              strokeWidth="4"
              strokeDasharray={`${percent} 100`}
              strokeLinecap="round"
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center text-xs font-medium text-ink">
            {percent}%
          </div>
        </div>
        <div>
          <p className="text-sm font-medium text-ink mb-1">
            전체 준비 진행률 {percent}%
          </p>
          <p className="text-xs text-ink-muted">
            완료 {doneCount}개 · 미완료 {total - doneCount}개 · 총 {total}개
            항목
          </p>
        </div>
      </div>

      {total === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          등록된 체크리스트가 없습니다.
        </div>
      )}

      {/* 단계별 그룹 */}
      <div className="flex flex-col gap-6">
        {grouped
          .filter((g) => g.items.length > 0)
          .map((g) => (
            <div key={g.stageNum}>
              <div className="flex items-center justify-between px-1 mb-2">
                <p className="text-sm font-medium text-ink">
                  {g.stageNum}단계 — {STAGE_LABELS[g.stageNum]}
                  {g.doneInStage === g.items.length && " (완료)"}
                </p>
                <span className="text-xs text-ink-muted">
                  {g.doneInStage}/{g.items.length}
                </span>
              </div>

              <div className="flex flex-col gap-2">
                {g.items.map((item) => (
                  <div
                    key={item.checklistId}
                    onClick={() => handleToggleDone(item)}
                    className="flex items-center bg-white rounded-2xl border border-line px-5 py-4 relative cursor-pointer transition-colors hover:bg-cream/60"
                  >
                    <input
                      type="checkbox"
                      checked={item.done}
                      onChange={() => handleToggleDone(item)}
                      onClick={(e) => e.stopPropagation()}
                      className="w-4 h-4 mr-4 accent-brand"
                    />

                    <div className="flex-1 min-w-0">
                      {item.reservationId ? (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate("/mypage?tab=reservation");
                          }}
                          title="마이페이지 예약 현황에서 보기"
                          className={`text-left text-sm hover:underline ${
                            item.done ? "line-through text-ink-faint" : "text-ink"
                          }`}
                        >
                          {item.title}
                        </button>
                      ) : (
                        <p
                          className={`text-sm ${
                            item.done ? "line-through text-ink-faint" : "text-ink"
                          }`}
                        >
                          {item.title}
                        </p>
                      )}
                      {item.dueDate && !item.done && (
                        <p
                          className={`mt-0.5 text-xs ${
                            new Date(item.dueDate) <
                            new Date(new Date().toDateString())
                              ? "text-red-600 font-medium"
                              : "text-ink-faint"
                          }`}
                        >
                          {item.dueDate}까지
                        </p>
                      )}
                    </div>

                    {item.done && (
                      <span className="text-[11px] bg-green-50 text-green-700 px-2.5 py-1 rounded-full font-medium mr-3">
                        완료
                      </span>
                    )}

                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setOpenMenuId(
                          openMenuId === item.checklistId
                            ? null
                            : item.checklistId,
                        );
                      }}
                      className="w-7 h-7 flex items-center justify-center rounded-full text-ink-faint hover:bg-cream"
                    >
                      ⋯
                    </button>

                    {openMenuId === item.checklistId && (
                      <div
                        onClick={(e) => e.stopPropagation()}
                        className="absolute right-4 top-12 bg-white border border-line rounded-xl shadow-lg py-1 w-28 z-10"
                      >
                        <button
                          type="button"
                          onClick={() => openEditModal(item)}
                          className="w-full text-left px-4 py-2 text-xs text-ink hover:bg-cream"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(item.checklistId)}
                          className="w-full text-left px-4 py-2 text-xs text-red-600 hover:bg-cream"
                        >
                          삭제
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
      </div>

      {modalMode && (
        <ChecklistFormModal
          mode={modalMode}
          editTarget={editTarget}
          checklist={checklist}
          onSubmit={handleSubmitModal}
          onClose={closeModal}
        />
      )}
    </div>
  );
};

export default ListComponent;
