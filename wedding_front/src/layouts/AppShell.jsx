import { useEffect } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import GlobalInquiryChatHost from "../components/chat/GlobalInquiryChatHost";
import GlobalAlertHost from "../components/common/GlobalAlertHost";
import GlobalConfirmHost from "../components/common/GlobalConfirmHost";

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

  // 경로가 바뀔 때마다 스크롤을 맨 위로 리셋한다.
  // (예: 관리자 페이지에서 아래로 스크롤한 뒤 다른 메뉴로 이동했다가 돌아와도
  //  이전 스크롤 위치가 그대로 남아있던 문제)
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  if (isManagerOnly && !isAllowedForManager) {
    return <Navigate replace to="/manager/inquiries" />;
  }

  return (
    <>
      <Outlet />
      <GlobalInquiryChatHost />
      <GlobalAlertHost />
      <GlobalConfirmHost />
    </>
  );
};

export default AppShell;
