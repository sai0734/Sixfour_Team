import { useState } from "react";
import PrepLayout from "../../layouts/PrepLayout";
import PlanComponent from "../../components/weddingplan/PlanComponent";
import WishTab from "../../components/companywish/WishTab";
import ReservationTab from "../../components/reservation/ReservationTab";

const TABS = [
  { key: "plan", label: "플랜", enabled: true },
  { key: "reservation", label: "예약 현황", enabled: true },
  { key: "payment", label: "결제 내역", enabled: false },
  { key: "wish", label: "찜 목록", enabled: true },
];

const HubPage = () => {
  const [activeTab, setActiveTab] = useState("plan");

  return (
    <PrepLayout
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
      {activeTab === "wish" && <WishTab />}
    </PrepLayout>
  );
};

export default HubPage;
