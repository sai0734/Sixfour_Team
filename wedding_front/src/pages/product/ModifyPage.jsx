import { useParams } from "react-router-dom";
import ModifyComponent from "../../components/product/ModifyComponent";

const ModifyPage = () => {
  const { pno } = useParams();

  return <ModifyComponent pno={pno} />;
};

export default ModifyPage;
