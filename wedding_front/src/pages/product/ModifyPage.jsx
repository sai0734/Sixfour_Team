import { useParams } from "react-router-dom";
import ModifyComponent from "../../components/product/ModifyComponent";

const ModifyPage = () => {
  const { pno } = useParams();

  return (
    <div className="p-4 w-full bg-white">
      <div className="text-3xl font-extrabold">Product Modify Page</div>

      <ModifyComponent pno={pno} />
    </div>
  );
};

export default ModifyPage;
