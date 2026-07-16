import PrepLayout from "../../layouts/PrepLayout";
import AiDressTryOnComponent from "../../components/prep/AiDressTryOnComponent";

const AiDressPage = () => {
  return (
    <PrepLayout
      eyebrow="AI DRESS"
      title="AI 드레스 체험"
      subtitle="내 사진에 드레스를 입혀 미리 확인해보세요"
    >
      <AiDressTryOnComponent />
    </PrepLayout>
  );
};

export default AiDressPage;
