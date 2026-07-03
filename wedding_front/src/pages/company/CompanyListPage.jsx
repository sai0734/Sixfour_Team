import BasicLayout from "../../layouts/BasicLayout";
import CompanyListComponent from "../../components/company/CompanyListComponent";

const CompanyListPage = () => {
  return (
    <BasicLayout showCart={false}>
      <CompanyListComponent />
    </BasicLayout>
  );
};

export default CompanyListPage;
