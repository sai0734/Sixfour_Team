import { Suspense, lazy } from "react";
const Loading = <div>Loading....</div>;
const Login = lazy(() => import("../pages/auth/LoginPage"));
const Join = lazy(() => import("../pages/auth/JoinPage"));
const SocialComplete = lazy(() => import("../pages/auth/SocialCompletePage"));
const LogoutPage = lazy(() => import("../pages/auth/LogoutPage"));
const KakaoRedirect = lazy(() => import("../pages/auth/KakaoRedirectPage"));
const AuthModify = lazy(() => import("../pages/auth/ModifyPage"));
const PasswordResetRequest = lazy(
  () => import("../pages/auth/PasswordResetRequestPage"),
);
const PasswordResetConfirm = lazy(
  () => import("../pages/auth/PasswordResetConfirmPage"),
);

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
      path: "join",
      element: (
        <Suspense fallback={Loading}>
          <Join />
        </Suspense>
      ),
    },
    {
      path: "social-complete",
      element: (
        <Suspense fallback={Loading}>
          <SocialComplete />
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
    {
      path: "password-reset",
      element: (
        <Suspense fallback={Loading}>
          <PasswordResetRequest />
        </Suspense>
      ),
    },
    {
      path: "reset-password",
      element: (
        <Suspense fallback={Loading}>
          <PasswordResetConfirm />
        </Suspense>
      ),
    },
  ];
};

export default authRouter;
