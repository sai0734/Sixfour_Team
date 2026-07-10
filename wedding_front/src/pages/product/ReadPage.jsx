import { useParams } from "react-router-dom";
import ReadComponent from "../../components/product/ReadComponent";

const ReadPage = () => {
  const { pno } = useParams();

  return <ReadComponent pno={pno}></ReadComponent>;
};

export default ReadPage;
