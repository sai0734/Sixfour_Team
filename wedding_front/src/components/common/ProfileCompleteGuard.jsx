import { useEffect } from "react";
import { useSelector } from "react-redux";
import { useLocation, useNavigate } from "react-router-dom";

// 카카오 등 소셜 최초가입 직후에는 이미 로그인(JWT)은 되어있지만
// 이름/전화번호/주소/약관동의 같은 필수 정보(SocialCompletePage)를 아직 안 넣은 상태일 수 있음.
// 이 상태로 홈/다른 페이지를 자유롭게 돌아다니게 두면 "반쪽짜리 회원"이 그냥 사이트를 쓰게 되므로,
// 추가정보 입력 페이지 말고 다른 곳으로 가려고 하면 강제로 그 페이지로 돌려보냄.
const ProfileCompleteGuard = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const needsProfile =
      loginState.email &&
      loginState.social &&
      loginState.profileComplete === false;

    if (needsProfile && location.pathname !== "/auth/social-complete") {
      navigate("/auth/social-complete", { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    loginState.email,
    loginState.social,
    loginState.profileComplete,
    location.pathname,
  ]);

  return null;
};

export default ProfileCompleteGuard;
