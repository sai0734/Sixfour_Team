import PrepLayout from "../../layouts/PrepLayout";
import ListComponent from "../../components/checklist/ListComponent";

const ListPage = () => {
  return (
    <PrepLayout
      eyebrow="03 — READY TO SAY I DO"
      title="체크리스트"
      subtitle="결혼 준비, 놓치지 않고 하나씩"
    >
      <ListComponent />
    </PrepLayout>
  );
};

export default ListPage;
