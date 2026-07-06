import { useParams } from "react-router-dom";
import AdminOrderDetailComponent from "../../components/admin/AdminOrderDetailComponent";

const AdminOrderDetailPage = () => {
  const { ono } = useParams();

  return <AdminOrderDetailComponent ono={ono} />;
};

export default AdminOrderDetailPage;
