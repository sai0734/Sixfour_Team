import { useEffect, useState } from "react";
import {
  getByMember,
  postAdd,
  putOne,
  deleteOne,
} from "../../api/weddingplanApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const initState = {
  groomName: "",
  brideName: "",
  weddingDate: "",
  weddingLocation: "",
  totalBudget: 0,
  memo: "",
};

const fields = [
  { name: "groomName", label: "신랑", type: "text" },
  { name: "brideName", label: "신부", type: "text" },
  { name: "weddingDate", label: "예식일", type: "date" },
  { name: "weddingLocation", label: "예식장", type: "text" },
  { name: "totalBudget", label: "총 예산", type: "number" },
  { name: "memo", label: "메모", type: "text" },
];

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

  const handleClickRegister = () => {
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

  return (
    <div className="bg-white rounded-2xl border border-line p-8 max-w-2xl mx-auto mb-20">
      {!plan && (
        <p className="text-center text-sm text-ink-muted mb-6">
          아직 등록된 웨딩플랜이 없습니다. 아래 정보를 입력해주세요.
        </p>
      )}

      {showForm ? (
        <>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            {fields.map((f) => (
              <label
                key={f.name}
                className={`flex flex-col gap-2 ${
                  f.name === "memo" ? "col-span-2" : ""
                }`}
              >
                <span className="text-xs font-medium text-ink-soft">
                  {f.label}
                </span>
                <input
                  className="h-11 px-4 rounded-lg border border-line-soft text-sm focus:outline-none focus:border-brand"
                  name={f.name}
                  type={f.type}
                  value={editForm[f.name] ?? ""}
                  onChange={handleChange}
                />
              </label>
            ))}
          </div>

          <div className="flex justify-end gap-2 mt-6">
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
        </>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-y-5 gap-x-8 sm:grid-cols-2">
            {fields.map((f) => (
              <div key={f.name}>
                <p className="text-xs text-ink-muted mb-1">{f.label}</p>
                <p className="text-sm text-ink font-medium">
                  {f.name === "totalBudget"
                    ? Number(plan[f.name]).toLocaleString() + "원"
                    : plan[f.name] || "-"}
                </p>
              </div>
            ))}
          </div>

          <div className="flex justify-end gap-2 mt-8">
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
        </>
      )}
    </div>
  );
};

export default PlanComponent;
