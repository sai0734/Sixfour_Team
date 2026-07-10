import MyPageLayout from "../../layouts/MyPageLayout";
import ModifyComponent from "../../components/auth/ModifyComponent";

const ModifyPage = () => {
  return (
    <MyPageLayout
      eyebrow="ACCOUNT"
      title="회원정보 수정"
      subtitle="닉네임과 비밀번호를 관리해요"
    >
      <ModifyComponent />
    </MyPageLayout>
  );
};

export default ModifyPage;
