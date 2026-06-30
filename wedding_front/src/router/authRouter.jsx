import { Suspense, lazy } from "react";
const Loading = <div>Loading....</div>;
const Login = lazy(() => import("../pages/auth/LoginPage"));
const LogoutPage = lazy(() => import("../pages/auth/LogoutPage"));
const KakaoRedirect = lazy(() => import("../pages/auth/KakaoRedirectPage"));
const AuthModify = lazy(() => import("../pages/auth/ModifyPage"));

const authRouter = () => {
  return [
    {
      path: "login",
      element: (
        <Suspense fallback={Loading}>
          <Login />
        </Suspense>
      ),
    },
    {
      path: "logout",
      element: (
        <Suspense fallback={Loading}>
          <LogoutPage />
        </Suspense>
      ),
    },
    {
      path: "kakao",
      element: (
        <Suspense fallback={Loading}>
          <KakaoRedirect />
        </Suspense>
      ),
    },
    {
      path: "modify",
      element: (
        <Suspense fallback={Loading}>
          <AuthModify />
        </Suspense>
      ),
    },
  ];
};

export default authRouter;
