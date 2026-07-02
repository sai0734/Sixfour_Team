import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { getAccessToken, getAuthWithAccessToken } from "../../api/kakaoAuthApi";
import { login } from "../../slices/loginSlice";
import { useDispatch } from "react-redux";
import useCustomLogin from "../../hooks/useCustomLogin";

const KakaoRedirectPage = () => {
  const [searchParams] = useSearchParams();

  const { moveToPath } = useCustomLogin();

  const dispatch = useDispatch();

  const authCode = searchParams.get("code");

  useEffect(() => {
    getAccessToken(authCode).then((accessToken) => {
      console.log(accessToken);

      getAuthWithAccessToken(accessToken).then((authInfo) => {
        console.log("------------------");
        console.log(authInfo);

        dispatch(login(authInfo));

        if (authInfo && authInfo.social && !authInfo.profileComplete) {
          moveToPath("/auth/social-complete");
        } else {
          moveToPath("/");
        }
      });
    });
  }, [authCode]);
  return (
    <div>
      <div>Kakao Login Redirect</div>
      <div>{authCode}</div>
    </div>
  );
};

export default KakaoRedirectPage;
