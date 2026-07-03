import { useParams } from "react-router-dom";
import ReservationReadComponent from "../../components/reservation/ReservationReadComponent";

const ReservationReadPage = () => {
  const { reservationId } = useParams();

  return (
    <div className="font-extrabold w-full bg-white mt-6">
      <div className="text-2xl ">Reservation Read Page Component {reservationId}</div>

      <ReservationReadComponent reservationId={reservationId}></ReservationReadComponent>
    </div>
  );
};

export default ReservationReadPage;
