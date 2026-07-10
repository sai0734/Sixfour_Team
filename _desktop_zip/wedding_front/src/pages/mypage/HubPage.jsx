import { useSearchParams } from "react-router-dom";
import MyPageLayout from "../../layouts/MyPageLayout";
import PlanComponent from "../../components/weddingplan/PlanComponent";
import WishTab from "../../components/companywish/WishTab";
import ReservationTab from "../../components/reservation/ReservationTab";
import PaymentTab from "../../components/mypage/PaymentTab";
import MyPostsTab from "../../components/mypage/MyPostsTab";
import AccountSettingsTab from "../../components/mypage/AccountSettingsTab";

const TABS = [
  { key: "plan", label: "플랜", enabled: true },
  { key: "reservation", label: "예약 현황", enabled: true },
  { key: "payment", label: "결제 내역", enabled: true },
  { key: "wish", label: "찜 목록", enabled: true },
  { key: "myposts", label: "내가 쓴 글", enabled: true },
  { key: "account", label: "계정 설정", enabled: true },
];

const HubPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // 탭 상태를 리액트 state가 아니라 URL(?tab=)에 직접 둠.
  // 그래야 결제내역 -> 상품 상세로 이동했다가 "뒤로가기" 눌렀을 때
  // 브라우저 히스토리에 남은 이 URL을 그대로 읽어서 있던 탭으로 복원됨.
  const requestedTab = searchParams.get("tab");
  const activeTab = TABS.some((tab) => tab.key === requestedTab)
    ? requestedTab
    : "plan";

  const handleTabClick = (key) => {
    // replace: 탭 클릭마다 히스토리를 쌓지 않음 (그래야 뒤로가기 한 번에
    // 마이페이지 밖으로 바로 나가지, 탭 전환 기록을 하나씩 되짚지 않음)
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set("tab", key);
        return next;
      },
      { replace: true },
    );
  };

  return (
    <MyPageLayout
      eyebrow="MY WEDDING PAGE"
      title="마이페이지"
      subtitle="내 결혼 준비 현황을 한 곳에서"
    >
      <nav className="flex gap-8 text-sm font-medium border-b border-line mb-8">
        {TABS.map((tab) => {
          if (!tab.enabled) {
            return (
              <span
                key={tab.key}
                title="준비 중인 기능입니다"
                className="pb-3 border-b border-transparent text-ink-faint cursor-not-allowed select-none"
              >
                {tab.label}
              </span>
            );
          }

          return (
            <span
              key={tab.key}
              onClick={() => handleTabClick(tab.key)}
              className={`pb-3 border-b cursor-pointer ${
                activeTab === tab.key
                  ? "text-brand border-brand"
                  : "text-ink-soft border-transparent hover:text-ink"
              }`}
            >
              {tab.label}
            </span>
          );
        })}
      </nav>

      {activeTab === "plan" && <PlanComponent />}
      {activeTab === "reservation" && <ReservationTab />}
      {activeTab === "payment" && <PaymentTab />}
      {activeTab === "wish" && <WishTab />}
      {activeTab === "myposts" && <MyPostsTab />}
      {activeTab === "account" && <AccountSettingsTab />}
    </MyPageLayout>
  );
};

export default HubPage;
