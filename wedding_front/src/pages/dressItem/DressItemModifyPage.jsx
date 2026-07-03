import { useParams } from "react-router-dom";
import DressItemModifyComponent from "../../components/dressItem/DressItemModifyComponent";

const DressItemModifyPage = () => {
  const { dressItemId } = useParams();

  return (
    <div className="p-4 w-full bg-white">
      <div className="text-3xl font-extrabold">Dress Item Modify Page</div>

      <DressItemModifyComponent pno={dressItemId} />
    </div>
  );
};

export default DressItemModifyPage;
