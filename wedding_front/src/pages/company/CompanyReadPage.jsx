import BasicLayout from "../../layouts/BasicLayout";
import CompanyReadComponent from "../../components/company/CompanyReadComponent";

const CompanyReadPage = () => {
  return (
    <BasicLayout showCart={false}>
      <CompanyReadComponent />
    </BasicLayout>
  );
};

export default CompanyReadPage;
