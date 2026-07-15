import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import GlobalInquiryChatHost from "../components/chat/GlobalInquiryChatHost";

// MANAGER 역할 계정이 접근 가능한 경로 (업체페이지 + 로그인/로그아웃 등 인증 관련만)
const MANAGER_ALLOWED_PREFIXES = ["/manager", "/auth"];

const AppShell = () => {
  const location = useLocation();
  const roleNames = useSelector((state) => state.loginSlice?.roleNames);
  const isManagerOnly =
    roleNames?.some((roleName) =>
      ["MANAGER", "ROLE_MANAGER"].includes(roleName),
    ) &&
    !roleNames?.some((roleName) => ["ADMIN", "ROLE_ADMIN"].includes(roleName));
  const isAllowedForManager = MANAGER_ALLOWED_PREFIXES.some((prefix) =>
    location.pathname.startsWith(prefix),
  );

  if (isManagerOnly && !isAllowedForManager) {
    return <Navigate replace to="/manager/inquiries" />;
  }

  return (
    <>
      <Outlet />
      <GlobalInquiryChatHost />
    </>
  );
};

export default AppShell;
