import { useParams } from "react-router-dom";
import ModifyComponent from "../../components/reservation/ModifyComponent";

const ModifyPage = () => {
  const { reservationId } = useParams();

  return (
    <div className="p-4 w-full bg-white">
      <div className="text-3xl font-extrabold">Reservation Modify Page</div>

      <ModifyComponent reservationId={reservationId} />
    </div>
  );
};

export default ModifyPage;
