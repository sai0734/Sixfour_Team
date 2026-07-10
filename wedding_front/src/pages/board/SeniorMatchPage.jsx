import { useEffect, useMemo, useState } from "react";
import BasicMenu from "../../components/menus/BasicMenu";
import BoardTopTabs from "../../components/board/BoardTopTabs";
import BoardFilterSidebar from "../../components/board/BoardFilterSidebar";
import TapeLabel from "../../components/common/TapeLabel";
import CoupleProfileFormModal, {
  REGION_OPTIONS,
  STYLE_OPTIONS,
} from "../../components/board/CoupleProfileFormModal";
import CoupleProfileDetailModal from "../../components/board/CoupleProfileDetailModal";
import {
  getByMember,
  listOthers,
  postAdd,
  putOne,
} from "../../api/coupleProfileApi";
import { calcMatchScore } from "../../util/matchScore";
import useCustomLogin from "../../hooks/useCustomLogin";

const BUDGET_BUCKETS = [
  { value: "under2000", label: "2천만원 미만", test: (avg) => avg < 2000 },
  {
    value: "2000to4000",
    label: "2~4천만원",
    test: (avg) => avg >= 2000 && avg < 4000,
  },
  {
    value: "4000to6000",
    label: "4~6천만원",
    test: (avg) => avg >= 4000 && avg < 6000,
  },
  { value: "over6000", label: "6천만원 이상", test: (avg) => avg >= 6000 },
];

const YEAR_OPTIONS = ["2025", "2026", "2027"];

const SeniorMatchPage = () => {
  const { loginState } = useCustomLogin();

  const [myProfile, setMyProfile] = useState(null);
  const [profileLoaded, setProfileLoaded] = useState(false);
  const [others, setOthers] = useState([]);
  const [refresh, setRefresh] = useState(false);

  const [budgetFilter, setBudgetFilter] = useState(null);
  const [regionFilter, setRegionFilter] = useState(null);
  const [styleFilter, setStyleFilter] = useState(null);
  const [yearFilter, setYearFilter] = useState(null);
  const [sort, setSort] = useState("match");

  const [formMode, setFormMode] = useState(null);
  const [detailProfile, setDetailProfile] = useState(null);

  useEffect(() => {
    if (!loginState.email) return;

    getByMember(loginState.email)
      .then((data) => setMyProfile(data))
      .catch(() => setMyProfile(null))
      .finally(() => setProfileLoaded(true));

    listOthers(loginState.email).then((data) => setOthers(data));
  }, [loginState.email, refresh]);

  const visibleProfiles = useMemo(() => {
    let result = others;

    if (budgetFilter) {
      const bucket = BUDGET_BUCKETS.find((b) => b.value === budgetFilter);
      result = result.filter((p) =>
        bucket.test((p.budgetMin + p.budgetMax) / 2),
      );
    }
    if (regionFilter) {
      result = result.filter((p) => p.region === regionFilter);
    }
    if (styleFilter) {
      result = result.filter((p) => p.weddingStyle === styleFilter);
    }
    if (yearFilter) {
      result = result.filter(
        (p) => p.weddingDate && p.weddingDate.startsWith(yearFilter),
      );
    }

    const withScore = result.map((p) => ({
      ...p,
      __score: calcMatchScore(myProfile, p),
    }));

    withScore.sort((a, b) => {
      if (sort === "match" && myProfile) {
        return (b.__score || 0) - (a.__score || 0);
      }
      return b.profileId - a.profileId;
    });

    return withScore;
  }, [
    others,
    budgetFilter,
    regionFilter,
    styleFilter,
    yearFilter,
    sort,
    myProfile,
  ]);

  const handleSubmitForm = (formValues) => {
    if (formMode === "add") {
      postAdd({ ...formValues, memberEmail: loginState.email })
        .then(() => {
          setFormMode(null);
          setRefresh((r) => !r);
        })
        .catch((e) => {
          console.error(e);
          alert("등록에 실패했습니다.");
        });
    } else {
      putOne({ ...myProfile, ...formValues })
        .then(() => {
          setFormMode(null);
          setRefresh((r) => !r);
        })
        .catch((e) => console.error(e));
    }
  };

  if (!loginState.email) {
    return (
      <>
        <BasicMenu />
        <div className="p-10 text-center text-ink-faint">
          로그인 후 이용해주세요.
        </div>
      </>
    );
  }

  return (
    <>
      <BasicMenu />

      <div className="min-h-screen bg-[#FBF7F0]">
        <section
          className="relative bg-cover bg-center px-5 pt-24 pb-8 text-center md:px-8 md:pt-28 md:pb-10 lg:px-[60px]"
          // TODO: 선배부부매칭 전용 사진 준비되면 /senior-match-hero.jpg 같은 걸로 교체
          style={{ backgroundImage: "url('/prep-hero.jpg')" }}
        >
          <div className="absolute inset-0 bg-black/45" />

          <div className="relative z-10">
            <TapeLabel tone="white" className="mb-3">
              SENIOR COUPLE MATCHING
            </TapeLabel>
            <p className="mb-2 font-['Gowun_Batang'] text-2xl text-white md:text-3xl">
              선배 부부 매칭
            </p>
            <p className="text-sm text-white/85">
              비슷한 조건의 선배 부부에게 노하우를 물어보세요
            </p>
          </div>
        </section>

        <div className="mx-auto max-w-[1140px] px-5 pt-6 md:px-8 lg:px-6">
          <BoardTopTabs active="SENIOR" />
        </div>

        <div className="mx-auto grid max-w-[1140px] grid-cols-1 items-start gap-6 px-5 py-6 md:px-8 lg:grid-cols-[240px_1fr] lg:gap-8 lg:px-6 lg:py-8">
          <BoardFilterSidebar
            groups={[
              {
                title: "예산 범위",
                options: BUDGET_BUCKETS,
                activeValue: budgetFilter,
                onSelect: setBudgetFilter,
              },
              {
                title: "지역",
                options: REGION_OPTIONS.map((r) => ({ value: r, label: r })),
                activeValue: regionFilter,
                onSelect: setRegionFilter,
              },
              {
                title: "웨딩 스타일",
                options: STYLE_OPTIONS.map((s) => ({ value: s, label: s })),
                activeValue: styleFilter,
                onSelect: setStyleFilter,
              },
              {
                title: "웨딩 시기",
                options: YEAR_OPTIONS.map((y) => ({
                  value: y,
                  label: y + "년",
                })),
                activeValue: yearFilter,
                onSelect: setYearFilter,
              },
            ]}
          />

          <main className="min-w-0 pb-20">
            <div className="flex flex-col gap-3 mb-2 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-ink-muted">
                비슷한 조건의 선배 부부 {visibleProfiles.length}명을 찾았어요
              </p>

              <div className="flex items-center gap-2">
                <div className="flex items-center gap-1 bg-surface rounded-full p-1">
                  <button
                    type="button"
                    onClick={() => setSort("match")}
                    className={`h-8 px-4 rounded-full text-xs font-medium ${
                      sort === "match"
                        ? "bg-white text-brand shadow-sm"
                        : "text-ink-muted"
                    }`}
                  >
                    매칭도순
                  </button>
                  <button
                    type="button"
                    onClick={() => setSort("recent")}
                    className={`h-8 px-4 rounded-full text-xs font-medium ${
                      sort === "recent"
                        ? "bg-white text-brand shadow-sm"
                        : "text-ink-muted"
                    }`}
                  >
                    최신순
                  </button>
                </div>
              </div>
            </div>

            {profileLoaded && !myProfile && (
              <div className="bg-brand-light rounded-2xl p-4 mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-xs text-brand-accent">
                  내 프로필을 등록하면 매칭도를 확인할 수 있어요.
                </p>
                <button
                  type="button"
                  onClick={() => setFormMode("add")}
                  className="h-8 px-4 rounded-full bg-brand text-white text-xs font-medium hover:bg-brand-dark shrink-0 self-start sm:ml-4 sm:self-auto"
                >
                  프로필 등록하기
                </button>
              </div>
            )}

            {profileLoaded && myProfile && (
              <div className="flex justify-end mb-4">
                <button
                  type="button"
                  onClick={() => setFormMode("edit")}
                  className="h-8 px-4 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
                >
                  내 프로필 수정
                </button>
              </div>
            )}

            {visibleProfiles.length === 0 && (
              <div className="text-center text-ink-faint py-16 bg-white rounded-2xl border border-line">
                조건에 맞는 선배 부부가 없습니다.
              </div>
            )}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {visibleProfiles.map((p) => {
                const initials = p.memberEmail?.[0]?.toUpperCase() || "?";

                return (
                  <div
                    key={p.profileId}
                    className="bg-white rounded-2xl border border-line p-5"
                  >
                    <div className="flex items-center gap-3 mb-3">
                      <div className="w-10 h-10 rounded-full bg-brand-light text-brand-accent flex items-center justify-center text-sm font-medium shrink-0">
                        {initials}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-ink truncate">
                          {p.memberEmail}
                        </p>
                        {p.weddingDate && (
                          <p className="text-[11px] text-ink-faint">
                            {p.weddingDate} 결혼
                          </p>
                        )}
                      </div>
                      {p.__score !== null && (
                        <span className="text-sm font-medium text-brand shrink-0">
                          {p.__score}%
                        </span>
                      )}
                    </div>

                    <div className="flex flex-wrap gap-1.5 mb-3">
                      {p.region && (
                        <span className="text-[11px] bg-surface text-ink-muted px-2 py-0.5 rounded-full">
                          {p.region}
                        </span>
                      )}
                      {p.budgetMin != null && (
                        <span className="text-[11px] bg-surface text-ink-muted px-2 py-0.5 rounded-full">
                          {p.budgetMin.toLocaleString()}~
                          {p.budgetMax.toLocaleString()}만원
                        </span>
                      )}
                      {p.weddingStyle && (
                        <span className="text-[11px] bg-surface text-ink-muted px-2 py-0.5 rounded-full">
                          {p.weddingStyle}
                        </span>
                      )}
                    </div>

                    <p className="text-xs text-ink-muted line-clamp-2 mb-3">
                      {p.bio || "자기소개가 없습니다."}
                    </p>

                    <button
                      type="button"
                      onClick={() => setDetailProfile(p)}
                      className="w-full h-9 rounded-full border border-line-soft text-xs text-ink-soft hover:bg-cream"
                    >
                      프로필 보기
                    </button>
                  </div>
                );
              })}
            </div>
          </main>
        </div>
      </div>

      {formMode && (
        <CoupleProfileFormModal
          mode={formMode}
          editTarget={formMode === "edit" ? myProfile : null}
          onSubmit={handleSubmitForm}
          onClose={() => setFormMode(null)}
        />
      )}

      {detailProfile && (
        <CoupleProfileDetailModal
          profile={detailProfile}
          matchScore={detailProfile.__score}
          onClose={() => setDetailProfile(null)}
        />
      )}
    </>
  );
};

export default SeniorMatchPage;
