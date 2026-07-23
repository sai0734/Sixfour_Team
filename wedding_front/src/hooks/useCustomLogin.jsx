import { useDispatch, useSelector } from "react-redux";
import { createSearchParams, Navigate, useNavigate } from "react-router-dom";
import { loginPostAsync, logout } from "../slices/loginSlice";
import { mergeGuestCartAsync } from "../slices/cartSlice";
import { showAlert } from "../util/globalAlert";

const LOGIN_REDIRECT_KEY = "loginRedirect";

const useCustomLogin = () => {
  const navigate = useNavigate();

  const dispatch = useDispatch();

  const loginState = useSelector((state) => state.loginSlice);
  const cartItems = useSelector((state) => state.cartSlice);

  const isLogin = loginState.email ? true : false;

  const doLogin = async (loginParam) => {
    const action = await dispatch(loginPostAsync(loginParam));

    const result = action.payload;

    // 병합이 완전히 끝날 때까지 기다린 후 doLogin이 종료되도록 await 추가
    // (이전엔 await 없이 그냥 dispatch만 해서, 병합 도중에 화면이 어중간한 상태를 보여주는 문제가 있었음)
    if (result && result.email) {
      const hasGuestItems = cartItems.some((item) => item.isGuest);
      if (hasGuestItems) {
        await dispatch(mergeGuestCartAsync(result.email));
      }
    }

    return result;
  };

  const doLogout = () => {
    dispatch(logout());
  };

  const moveToPath = (path) => {
    navigate({ pathname: path }, { replace: true });
  };

  const moveToLogin = (returnPath) => {
    if (returnPath && typeof returnPath === "string") {
      sessionStorage.setItem(LOGIN_REDIRECT_KEY, returnPath);
    } else if (returnPath) {
      // 문자열이 아닌 값(객체 등)이 실수로 들어온 경우 - 저장하면 "[object Object]"처럼
      // 의미 없는 문자열로 저장되어 나중에 엉뚱한 경로로 리다이렉트되므로 아예 무시
      console.warn(
        "moveToLogin: returnPath는 문자열이어야 합니다.",
        returnPath,
      );
    }
    navigate(
      { pathname: "/auth/login" },
      {
        replace: true,
        state:
          returnPath && typeof returnPath === "string"
            ? { from: returnPath }
            : undefined,
      },
    );
  };
  const getLoginRedirectPath = () => {
    const path = sessionStorage.getItem(LOGIN_REDIRECT_KEY);

    // 혹시 예전에 잘못 저장된("[object Object]" 같은) 값이 남아있으면 무시
    if (!path || !path.startsWith("/")) {
      return null;
    }

    return path;
  };
  const clearLoginRedirectPath = () => {
    sessionStorage.removeItem(LOGIN_REDIRECT_KEY);
  };

  const moveToLoginReturn = () => {
    return <Navigate replace to="/auth/login" />;
  };

  const exceptionHandle = (ex) => {
    console.log("Exception----------------------");

    console.log(ex);

    const errorMsg = ex.response.data.error;

    const errorStr = createSearchParams({ error: errorMsg }).toString();

    if (errorMsg === "REQUIRE_LOGIN") {
      showAlert("로그인 해야만 합니다.", () => {
        navigate({ pathname: "/auth/login", search: errorStr });
      });
      return;
    }

    if (ex.response.data.error === "ERROR_ACCESSDENIED") {
      showAlert("해당 메뉴를 사용할수 있는 권한이 없습니다.", () => {
        navigate({ pathname: "/auth/login", search: errorStr });
      });
      return;
    }
  };

  return {
    loginState,
    isLogin,
    doLogin,
    doLogout,
    moveToPath,
    moveToLogin,
    moveToLoginReturn,
    getLoginRedirectPath,
    clearLoginRedirectPath,
    exceptionHandle,
  };
};

export default useCustomLogin;
