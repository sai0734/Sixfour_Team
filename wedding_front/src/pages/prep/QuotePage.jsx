import PrepLayout from "../../layouts/PrepLayout";
import useCustomLogin from "../../hooks/useCustomLogin";
import QuoteCompareComponent from "../../components/prep/QuoteCompareComponent";

const QuotePage = () => {
  const { loginState } = useCustomLogin();

  if (!loginState.email) {
    return (
      <PrepLayout
        eyebrow="AI QUOTE"
        title="AI 견적서"
        subtitle="견적서 사진을 올리면 항목을 정리하고, 같은 종류끼리 비교해드려요"
      >
        <div className="p-10 text-center text-ink-faint">
          로그인 후 이용해주세요.
        </div>
      </PrepLayout>
    );
  }

  return (
    <PrepLayout
      eyebrow="AI QUOTE"
      title="AI 견적서"
      subtitle="견적서 사진을 올리면 항목을 정리하고, 같은 종류끼리 비교해드려요"
    >
      <QuoteCompareComponent />
    </PrepLayout>
  );
};

export default QuotePage;
