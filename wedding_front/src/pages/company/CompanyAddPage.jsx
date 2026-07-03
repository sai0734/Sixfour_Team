import BasicLayout from "../../layouts/BasicLayout";
import CompanyAddComponent from "../../components/company/CompanyAddComponent";

const CompanyAddPage = () => {
  return (
    <BasicLayout showCart={false}>
      <CompanyAddComponent />
    </BasicLayout>
  );
};

export default CompanyAddPage;
