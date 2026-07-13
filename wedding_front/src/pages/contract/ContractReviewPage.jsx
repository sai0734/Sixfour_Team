import BasicLayout from "../../layouts/BasicLayout";
import ContractReviewComponent from "../../components/contract/ContractReviewComponent";

const ContractReviewPage = () => {
  return (
    <BasicLayout showCart={false}>
      <ContractReviewComponent />
    </BasicLayout>
  );
};

export default ContractReviewPage;
