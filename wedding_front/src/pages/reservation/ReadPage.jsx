import { useParams } from "react-router-dom";
import ReadComponent from "../../components/reservation/ReadComponent";

const ReadPage = () => {
  const { reservationId } = useParams();

  return (
    <div className="font-extrabold w-full bg-white mt-6">
      <div className="text-2xl ">Reservation Read Page Component {reservationId}</div>

      <ReadComponent reservationId={reservationId}></ReadComponent>
    </div>
  );
};

export default ReadPage;
