import { useParams } from "react-router-dom";
import DressItemReadComponent from "../../components/dressItem/DressItemReadComponent";

const DressItemReadPage = () => {
  const { dressItemId } = useParams();

  return (
    <div className="p-4 w-full bg-white">
      <div className="text-3xl font-extrabold">Dress Item Read Page</div>

      <DressItemReadComponent pno={dressItemId}></DressItemReadComponent>
    </div>
  );
};

export default DressItemReadPage;
