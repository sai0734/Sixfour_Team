import MyPageLayout from "../../layouts/MyPageLayout";
import ManagerReservationInbox from "../../components/manager/ManagerReservationInbox";

const ManagerReservationPage = () => {
  return (
    <MyPageLayout
      eyebrow="COMPANY PAGE"
      title="업체 예약관리"
      subtitle="캘린더에서 예약 현황을 확인하고 결제대기로 전환하세요"
    >
      <ManagerReservationInbox />
    </MyPageLayout>
  );
};

export default ManagerReservationPage;
