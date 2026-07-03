import { useParams } from "react-router-dom";
import ReservationModifyComponent from "../../components/reservation/ReservationModifyComponent";

const ReservationModifyPage = () => {
  const { reservationId } = useParams();

  return (
    <div className="p-4 w-full bg-white">
      <div className="text-3xl font-extrabold">Reservation Modify Page</div>

      <ReservationModifyComponent reservationId={reservationId} />
    </div>
  );
};

export default ReservationModifyPage;
