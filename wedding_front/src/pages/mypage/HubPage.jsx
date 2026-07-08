import { useState } from "react";
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
  const [searchParams] = useSearchParams();

  // 예: /mypage?tab=account 로 들어오면 "계정 설정" 탭이 바로 열림
  // (탭 목록에 없는 값이 오면 무시하고 기본값인 "plan"으로)
  const requestedTab = searchParams.get("tab");
  const initialTab = TABS.some((tab) => tab.key === requestedTab)
    ? requestedTab
    : "plan";

  const [activeTab, setActiveTab] = useState(initialTab);

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
              onClick={() => setActiveTab(tab.key)}
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
