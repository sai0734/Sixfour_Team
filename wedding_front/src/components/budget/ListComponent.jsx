import { useEffect, useState } from "react";
import {
  getListByMember,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/budgetApi";
import { getByMember as getWeddingPlan } from "../../api/weddingplanApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import BudgetFormModal from "./BudgetFormModal";

const CATEGORY_COLOR = {
  홀: "#993556",
  스드메: "#185FA5",
  예복: "#854F0B",
  예물: "#534AB7",
  기타: "#5F5E5A",
};

const CATEGORY_ICON = {
  홀: <path d="M4 21V9l8-6 8 6v12M9 21v-6h6v6" />,
  스드메: (
    <path d="M12 22c4 0 6-3 6-6.5 0-2-1-3.5-2-5 0 2-1 3-2 3 .5-3-1-6-4-7 1 2 .5 4-.5 5.5C8.5 13 8 14 8 15.5 8 19 9 22 12 22Z" />
  ),
  예복: <path d="M6 4h12l-2 4 2 14H8L10 8Z" />,
  예물: <circle cx="12" cy="12" r="7" />,
  기타: <path d="M4 8h16v13H4zM8 8V5h8v3" />,
};

const ListComponent = () => {
  const { loginState } = useCustomLogin();

  const [budgetList, setBudgetList] = useState([]);
  const [plan, setPlan] = useState(null);
  const [refresh, setRefresh] = useState(false);

  const [modalMode, setModalMode] = useState(null); // null | "add" | "edit"
  const [editTarget, setEditTarget] = useState(null);
  const [openMenuId, setOpenMenuId] = useState(null);

  useEffect(() => {
    if (!loginState.email) {
      return;
    }

    getListByMember(loginState.email).then((data) => {
      setBudgetList(data);
    });
    getWeddingPlan(loginState.email)
      .then((data) => setPlan(data))
      .catch(() => setPlan(null));
  }, [loginState.email, refresh]);

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
      const payload = { ...formValues, memberEmail: loginState.email };

      postAdd(payload)
        .then(() => {
          closeModal();
          setRefresh(!refresh);
        })
        .catch((e) => console.error(e));
    } else {
      // 수정: 카테고리 상관없이 전체 목록 기준으로, 이미 그 순서를 쓰는 항목이 있으면 서로 스왑
      const targetOrder = formValues.sortOrder;

      const conflictItem = budgetList.find(
        (i) =>
          i.sortOrder === targetOrder && i.budgetId !== editTarget.budgetId,
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

  const handleDelete = (budgetId) => {
    setOpenMenuId(null);

    deleteOne(budgetId)
      .then(() => setRefresh(!refresh))
      .catch((e) => console.error(e));
  };

  if (!loginState.email) {
    return (
      <div className="p-10 text-center text-ink-faint">
        로그인 후 이용해주세요.
      </div>
    );
  }

  const itemizedBudget = budgetList.reduce(
    (s, i) => s + (i.budgetAmount || 0),
    0,
  );
  const totalBudget = plan?.totalBudget || itemizedBudget;
  const totalActual = budgetList.reduce((s, i) => s + (i.actualAmount || 0), 0);
  const remaining = totalBudget - totalActual;

  return (
    <div className="pb-20">
      <div className="flex items-center justify-between mb-6">
        <p className="text-lg font-medium text-ink">예산 관리</p>
        <button
          type="button"
          onClick={openAddModal}
          className="h-10 px-5 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
        >
          + 항목 추가
        </button>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-8">
        <div className="bg-white rounded-2xl border border-line p-5">
          <p className="text-xs text-ink-muted mb-1">총 예산</p>
          <p className="text-xl font-medium text-ink">
            {totalBudget.toLocaleString()}원
          </p>
          {!plan?.totalBudget && (
            <p className="text-[10px] text-ink-faint mt-1">
              마이페이지 웨딩플랜에서 설정 가능
            </p>
          )}
        </div>
        <div className="bg-white rounded-2xl border border-line p-5">
          <p className="text-xs text-ink-muted mb-1">현재 지출</p>
          <p className="text-xl font-medium text-brand">
            {totalActual.toLocaleString()}원
          </p>
        </div>
        <div className="bg-white rounded-2xl border border-line p-5">
          <p className="text-xs text-ink-muted mb-1">잔여 예산</p>
          <p
            className={`text-xl font-medium ${remaining < 0 ? "text-red-600" : "text-ink"}`}
          >
            {remaining.toLocaleString()}원
          </p>
        </div>
      </div>

      {budgetList.length === 0 && (
        <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
          등록된 예산 항목이 없습니다.
        </div>
      )}

      <div className="flex flex-col gap-3">
        {budgetList.map((item) => {
          const over = item.actualAmount > item.budgetAmount;
          const ratio =
            item.budgetAmount > 0
              ? Math.min(100, (item.actualAmount / item.budgetAmount) * 100)
              : 0;
          const color = CATEGORY_COLOR[item.category] || "#5F5E5A";
          const diff = item.actualAmount - item.budgetAmount;

          return (
            <div
              key={item.budgetId}
              className={`bg-white rounded-2xl border p-5 relative ${
                over ? "border-red-300" : "border-line"
              }`}
            >
              <div className="flex items-start gap-4">
                <div
                  className="w-10 h-10 rounded-full flex items-center justify-center shrink-0"
                  style={{ background: `${color}1A`, color }}
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.6"
                    className="w-5 h-5"
                  >
                    {CATEGORY_ICON[item.category] || CATEGORY_ICON["기타"]}
                  </svg>
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-ink">
                        {item.category}
                      </span>
                      {over && (
                        <span className="text-[11px] bg-red-50 text-red-600 px-2 py-0.5 rounded-full font-medium">
                          초과
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-ink-muted">
                      예산 {Number(item.budgetAmount).toLocaleString()}원 / 지출{" "}
                      <span className={over ? "text-red-600 font-medium" : ""}>
                        {Number(item.actualAmount).toLocaleString()}원
                        {over && ` (+${diff.toLocaleString()}원)`}
                      </span>
                    </div>
                  </div>

                  <div className="h-1.5 rounded-full bg-surface mb-2">
                    <div
                      className="h-1.5 rounded-full"
                      style={{
                        width: `${ratio}%`,
                        background: over ? "#DC2626" : "#D4537E",
                      }}
                    ></div>
                  </div>

                  <p className="text-xs text-ink-muted">
                    {item.memo || "메모 없음"}
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    setOpenMenuId(
                      openMenuId === item.budgetId ? null : item.budgetId,
                    )
                  }
                  className="w-7 h-7 flex items-center justify-center rounded-full text-ink-faint hover:bg-cream shrink-0"
                >
                  ⋯
                </button>
              </div>

              {openMenuId === item.budgetId && (
                <div className="absolute right-5 top-14 bg-white border border-line rounded-xl shadow-lg py-1 w-28 z-10">
                  <button
                    type="button"
                    onClick={() => openEditModal(item)}
                    className="w-full text-left px-4 py-2 text-xs text-ink hover:bg-cream"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(item.budgetId)}
                    className="w-full text-left px-4 py-2 text-xs text-red-600 hover:bg-cream"
                  >
                    삭제
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {modalMode && (
        <BudgetFormModal
          mode={modalMode}
          editTarget={editTarget}
          budgetList={budgetList}
          onSubmit={handleSubmitModal}
          onClose={closeModal}
        />
      )}
    </div>
  );
};

export default ListComponent;
