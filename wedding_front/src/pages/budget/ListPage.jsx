import PrepLayout from "../../layouts/PrepLayout";
import ListComponent from "../../components/budget/ListComponent";

const ListPage = () => {
  return (
    <PrepLayout
      eyebrow="BUDGET PLANNING"
      title="예산 관리"
      subtitle="항목별 예산과 실지출을 한눈에"
    >
      <ListComponent />
    </PrepLayout>
  );
};

export default ListPage;
