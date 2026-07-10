import { useSelector } from "react-redux";
import { Navigate } from "react-router-dom";

const AdminOnly = ({ children }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const isAdmin = loginState.roleNames?.some((roleName) =>
    ["ADMIN", "ROLE_ADMIN"].includes(roleName),
  );

  if (!loginState.email) {
    return <Navigate replace to="/auth/login" />;
  }

  if (!isAdmin) {
    alert("관리자만 접근할 수 있습니다.");
    return <Navigate replace to="/" />;
  }

  return children;
};

export default AdminOnly;
