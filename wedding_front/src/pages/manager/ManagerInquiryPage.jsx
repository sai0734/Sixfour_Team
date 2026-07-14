import MyPageLayout from "../../layouts/MyPageLayout";
import ManagerInquiryInbox from "../../components/manager/ManagerInquiryInbox";

const ManagerInquiryPage = () => {
  return (
    <MyPageLayout
      eyebrow="COMPANY PAGE"
      title="업체페이지"
      subtitle="담당 업체로 들어온 회원 문의를 확인하고 답변하세요"
    >
      <ManagerInquiryInbox />
    </MyPageLayout>
  );
};

export default ManagerInquiryPage;
