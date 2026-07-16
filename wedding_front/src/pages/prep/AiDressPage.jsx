import PrepLayout from "../../layouts/PrepLayout";
import useCustomLogin from "../../hooks/useCustomLogin";
import AiDressTryOnComponent from "../../components/prep/AiDressTryOnComponent";

const AiDressPage = () => {
  const { loginState } = useCustomLogin();

  if (!loginState.email) {
    return (
      <PrepLayout
        eyebrow="AI DRESS"
        title="AI 드레스"
        subtitle="내 사진으로 드레스를 미리 입어보세요"
      >
        <div className="p-10 text-center text-ink-faint">
          로그인 후 이용해주세요.
        </div>
      </PrepLayout>
    );
  }

  return (
    <PrepLayout
      eyebrow="AI DRESS"
      title="AI 드레스"
      subtitle="내 사진으로 드레스를 미리 입어보세요"
    >
      <AiDressTryOnComponent />
    </PrepLayout>
  );
};

export default AiDressPage;
