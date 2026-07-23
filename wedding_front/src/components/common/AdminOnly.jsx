import { useEffect } from "react";
import { useSelector } from "react-redux";
import { Navigate } from "react-router-dom";
import { showAlert } from "../../util/globalAlert";

const AdminOnly = ({ children }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const isAdmin = loginState.roleNames?.some((roleName) =>
    ["ADMIN", "ROLE_ADMIN"].includes(roleName),
  );

  // alert()는 렌더링 도중 호출해도(동기/블로킹) 문제가 없었지만,
  // 모달 방식은 다른 컴포넌트의 상태를 갱신하므로 렌더링 중이 아닌 useEffect에서 띄운다.
  useEffect(() => {
    if (loginState.email && !isAdmin) {
      showAlert("관리자만 접근할 수 있습니다.");
    }
  }, [loginState.email, isAdmin]);

  if (!loginState.email) {
    return <Navigate replace to="/auth/login" />;
  }

  if (!isAdmin) {
    return <Navigate replace to="/" />;
  }

  return children;
};

export default AdminOnly;
