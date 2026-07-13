import BasicLayout from "../../layouts/BasicLayout";
import ReservationReserveComponent from "../../components/reservation/ReservationReserveComponent";

// 재원 추가 - 업체 상세페이지 "예약" 버튼 진입 지점
const ReservationReservePage = () => {
  return (
    <BasicLayout showCart={false}>
      <ReservationReserveComponent />
    </BasicLayout>
  );
};

export default ReservationReservePage;
