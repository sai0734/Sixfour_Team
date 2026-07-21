import { useEffect, useState } from "react";
import {
  getByMember,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/weddingplanApi";
import useCustomLogin from "../../hooks/useCustomLogin";
import TapeLabel from "../common/TapeLabel";

const initState = {
  groomName: "",
  brideName: "",
  weddingDate: "",
  totalBudget: 0,
  memo: "",
};

const REQUIRED_FIELDS = [
  { name: "groomName", label: "신랑 이름", type: "text", placeholder: "예: 김철수" },
  { name: "brideName", label: "신부 이름", type: "text", placeholder: "예: 이영희" },
  { name: "weddingDate", label: "예식일", type: "date" },
  { name: "totalBudget", label: "총 예산 (원)", type: "number" },
];

const DAY_LABEL = ["일", "월", "화", "수", "목", "금", "토"];

// 결과 화면(ResultCards)의 D-day 배지와 같은 계산식 - 마이페이지에서도 같은 감각으로 보여줌
const calcDday = (dateStr) => {
  if (!dateStr) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(dateStr);
  const diff = Math.ceil((target - today) / 86400000);
  if (diff > 0) return `D-${diff}`;
  if (diff === 0) return "D-DAY";
  return `D+${Math.abs(diff)}`;
};

const formatInvitationDate = (dateStr) => {
  if (!dateStr) return "예식일 미정";
  const d = new Date(dateStr);
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 ${DAY_LABEL[d.getDay()]}요일`;
};

const PlanComponent = () => {
  const { loginState } = useCustomLogin();

  const [plan, setPlan] = useState(null);
  const [editForm, setEditForm] = useState({ ...initState });
  const [editMode, setEditMode] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const fetchPlan = () => {
    if (!loginState.email) {
      return;
    }

    getByMember(loginState.email)
      .then((data) => {
        setPlan(data);
        setEditForm(data);
        setLoaded(true);
      })
      .catch(() => {
        setPlan(null);
        setLoaded(true);
      });
  };

  useEffect(() => {
    fetchPlan();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loginState.email]);

  const handleChange = (e) => {
    editForm[e.target.name] = e.target.value;
    setEditForm({ ...editForm });
  };

  // 신랑/신부/예식일/총예산은 마이페이지·준비관리 전반에서 기준으로 쓰는 필수값이라 여기서 막아둠
  const validateRequired = () => {
    if (
      !editForm.groomName?.trim() ||
      !editForm.brideName?.trim() ||
      !editForm.weddingDate ||
      !Number(editForm.totalBudget)
    ) {
      alert("신랑, 신부, 예식일, 총예산은 필수로 입력해주세요.");
      return false;
    }
    return true;
  };

  const handleClickRegister = () => {
    if (!validateRequired()) return;

    const payload = {
      ...editForm,
      memberEmail: loginState.email,
      totalBudget: Number(editForm.totalBudget) || 0,
    };

    postAdd(payload)
      .then(() => fetchPlan())
      .catch((e) => {
        console.error(e);
        alert(
          "등록에 실패했습니다. 이미 등록된 웨딩플랜이 있는지 확인해주세요.",
        );
      });
  };

  const handleClickSave = () => {
    if (!validateRequired()) return;

    const payload = {
      ...editForm,
      weddingPlanId: plan.weddingPlanId,
      memberEmail: loginState.email,
      totalBudget: Number(editForm.totalBudget) || 0,
    };

    putOne(payload)
      .then(() => {
        setEditMode(false);
        fetchPlan();
      })
      .catch((e) => console.error(e));
  };

  const handleClickDelete = () => {
    if (!window.confirm("웨딩플랜을 삭제하시겠습니까?")) {
      return;
    }

    deleteOne(plan.weddingPlanId)
      .then(() => {
        setPlan(null);
        setEditForm({ ...initState });
      })
      .catch((e) => console.error(e));
  };

  if (!loginState.email) {
    return (
      <div className="p-10 text-center text-ink-faint">
        로그인 후 이용해주세요.
      </div>
    );
  }

  if (!loaded) {
    return (
      <div className="p-10 text-center text-ink-faint">불러오는 중...</div>
    );
  }

  const showForm = !plan || editMode;

  // ── 등록/수정 폼 ──────────────────────────────────────────────
  if (showForm) {
    return (
      <div className="mx-auto mb-20 max-w-lg">
        <div className="rounded-[28px] border border-line bg-white p-8 shadow-[0_20px_50px_-24px_rgba(58,54,47,0.25)] sm:p-10">
          <p className="mb-1 text-center font-['Gowun_Batang'] text-xl text-ink">
            {plan ? "웨딩플랜 수정" : "웨딩플랜 등록"}
          </p>
          <p className="mb-8 text-center text-xs text-ink-faint">
            {plan
              ? "정보를 고치면 준비관리 곳곳에 바로 반영돼요"
              : "이 정보가 마이페이지와 준비관리 전반의 기준이 돼요"}
          </p>

          <div className="flex flex-col gap-5">
            {REQUIRED_FIELDS.map((f) => (
              <label key={f.name} className="flex flex-col gap-2">
                <span className="text-xs font-medium text-ink-soft">
                  {f.label} <span className="text-brand-deep">*</span>
                </span>
                <input
                  className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                  name={f.name}
                  type={f.type}
                  placeholder={f.placeholder}
                  min={f.type === "number" ? 0 : undefined}
                  value={editForm[f.name] ?? ""}
                  onChange={handleChange}
                />
              </label>
            ))}

            <label className="flex flex-col gap-2">
              <span className="text-xs font-medium text-ink-soft">
                메모 <span className="text-ink-faint">(선택)</span>
              </span>
              <textarea
                className="min-h-[72px] px-4 py-3 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                name="memo"
                placeholder="둘만의 짧은 다짐이나 메모를 남겨보세요"
                value={editForm.memo ?? ""}
                onChange={handleChange}
              />
            </label>
          </div>

          <div className="flex justify-end gap-2 mt-8">
            {editMode && (
              <button
                type="button"
                onClick={() => {
                  setEditMode(false);
                  setEditForm(plan);
                }}
                className="h-11 px-6 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream"
              >
                취소
              </button>
            )}
            <button
              type="button"
              onClick={plan ? handleClickSave : handleClickRegister}
              className="h-11 px-6 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
            >
              {plan ? "저장" : "등록"}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── 청첩장 스타일 카드 ─────────────────────────────────────────
  const dday = calcDday(plan.weddingDate);

  return (
    <div className="mx-auto mb-20 max-w-lg">
      <div className="flex justify-center">
        <TapeLabel className="relative z-10 -mb-4">우리 결혼합니다</TapeLabel>
      </div>

      {/* 이중 테두리로 청첩장 카드 느낌을 냄 */}
      <div className="rounded-[30px] border border-brand-dark/40 bg-white p-2 shadow-[0_24px_60px_-28px_rgba(58,54,47,0.35)]">
        <div className="rounded-[24px] border border-brand-dark/25 bg-blush-lavender px-8 py-12 text-center sm:px-12">
          <p className="mb-5 font-['Gowun_Batang'] text-[11px] tracking-[0.35em] text-brand-deep">
            WEDDING INVITATION
          </p>

          <p className="font-['Gowun_Batang'] text-2xl leading-relaxed text-ink sm:text-[28px]">
            {plan.groomName || "신랑"}
            <span className="mx-2 font-serif text-2xl italic text-brand-deep">
              &
            </span>
            {plan.brideName || "신부"}
          </p>

          <div className="mx-auto my-6 h-px w-14 bg-brand-dark/40" />

          <div className="flex flex-col items-center gap-2">
            {dday && (
              <span className="rounded-full bg-brand-deep px-3 py-1 text-xs font-bold text-white">
                {dday}
              </span>
            )}
            <p className="font-['Gowun_Batang'] text-sm text-ink-soft sm:text-base">
              {formatInvitationDate(plan.weddingDate)}
            </p>
          </div>

          {plan.memo && (
            <p className="mt-6 font-['Gowun_Batang'] text-xs italic leading-relaxed text-ink-muted">
              "{plan.memo}"
            </p>
          )}
        </div>
      </div>

      {/* 실용 정보(총 예산)는 청첩장 밖, 별도 카드로 */}
      <div className="mt-5 flex items-center justify-between rounded-2xl border border-line bg-white px-6 py-4">
        <span className="text-xs text-ink-muted">총 예산</span>
        <span className="text-base font-medium text-ink">
          {Number(plan.totalBudget).toLocaleString()}원
        </span>
      </div>

      <div className="flex justify-center gap-2 mt-6">
        <button
          type="button"
          onClick={() => setEditMode(true)}
          className="h-11 px-6 rounded-full bg-brand text-white text-sm font-medium hover:bg-brand-dark"
        >
          수정
        </button>
        <button
          type="button"
          onClick={handleClickDelete}
          className="h-11 px-6 rounded-full border border-line-soft text-sm text-ink-soft hover:bg-cream"
        >
          삭제
        </button>
      </div>
    </div>
  );
};

export default PlanComponent;
