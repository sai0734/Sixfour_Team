import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Navigate, useLocation } from "react-router-dom";
import { getMyManagedCompany } from "../../api/companyApi";

const MANAGER_PATH = "/manager/inquiries";

// 관리자가 회원관리에서 "업체 담당자"로 임명한 회원은, 로그인하면 이 전용 페이지 말고는
// 아무 것도 못 만지게 막아야 함(문의 답변만 하는 계정). 로그인 시 토큰에 이 정보를
// 안 담고, 대신 이 컴포넌트가 뜰 때마다 서버에 "나 담당 업체 있어?"를 물어봐서 판단함
// (토큰 발급 로직을 안 건드리기 위한 선택 - 회원가입/로그인 쪽 표준화 작업과 충돌 방지)
const CompanyManagerGuard = ({ children }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const location = useLocation();

  const [isManager, setIsManager] = useState(null); // null=확인 전, true/false=확인됨

  useEffect(() => {
    if (!loginState.email) {
      setIsManager(false);
      return;
    }

    getMyManagedCompany()
      .then((data) => setIsManager(Boolean(data.isManager)))
      .catch((err) => {
        console.error(err);
        setIsManager(false);
      });
  }, [loginState.email]);

  // 아직 확인 중이면(깜빡임 방지) 잠깐 아무것도 안 보여줌
  if (loginState.email && isManager === null) {
    return null;
  }

  if (isManager && location.pathname !== MANAGER_PATH) {
    return <Navigate replace to={MANAGER_PATH} />;
  }

  return children;
};

export default CompanyManagerGuard;
