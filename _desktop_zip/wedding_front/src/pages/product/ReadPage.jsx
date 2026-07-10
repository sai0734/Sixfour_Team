import { useParams } from "react-router-dom";
import ReadComponent from "../../components/product/ReadComponent";

const ReadPage = () => {
  const { pno } = useParams();

  return (
    <div className="p-4 w-full bg-white">
      <ReadComponent pno={pno}></ReadComponent>
    </div>
  );
};

export default ReadPage;
