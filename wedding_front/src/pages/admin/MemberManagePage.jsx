import BasicLayout from "../../layouts/BasicLayout";
import MemberManageComponent from "../../components/admin/MemberManageComponent";

const MemberManagePage = () => {
  return (
    <BasicLayout showCart={false}>
      <MemberManageComponent />
    </BasicLayout>
  );
};

export default MemberManagePage;
