import { useSelector } from "react-redux";
import { Navigate, useLocation } from "react-router-dom";

// 예전 방식으로 만들어진 "로그인은 됐는데 추가정보가 없는" 레거시 계정을 위한 안전장치.
// (지금은 가입이 완료돼야만 로그인 토큰이 발급되므로 새로 이 상태가 될 일은 없지만,
//  혹시 남아있는 예전 데이터에 대비해 방어적으로 유지)
const ProfileCompleteGuard = ({ children }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const location = useLocation();

  const needsProfile =
    loginState.email &&
    loginState.social &&
    loginState.profileComplete === false;

  if (needsProfile && location.pathname !== "/auth/social-complete") {
    return <Navigate replace to="/auth/social-complete" />;
  }

  return children;
};

export default ProfileCompleteGuard;
