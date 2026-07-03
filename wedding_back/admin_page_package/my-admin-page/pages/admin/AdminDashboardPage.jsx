import BasicLayout from "../../layouts/BasicLayout";
import AdminDashboardComponent from "../../components/admin/AdminDashboardComponent";

const AdminDashboardPage = () => {
  return (
    <BasicLayout showCart={false}>
      <AdminDashboardComponent />
    </BasicLayout>
  );
};

export default AdminDashboardPage;
