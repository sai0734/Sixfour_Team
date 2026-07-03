import BasicLayout from "../../layouts/BasicLayout";
import CompanyModifyComponent from "../../components/company/CompanyModifyComponent";

const CompanyModifyPage = () => {
  return (
    <BasicLayout showCart={false}>
      <CompanyModifyComponent />
    </BasicLayout>
  );
};

export default CompanyModifyPage;
