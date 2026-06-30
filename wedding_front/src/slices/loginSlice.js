import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { loginPost } from "../api/authApi";
import { getCookie, removeCookie, setCookie } from "../util/cookieUtil";

const initState = {
  email: "",
};

const loadAuthCookie = () => {
  //쿠키에서 로그인 정보 로딩
  const authInfo = getCookie("auth");

  //닉네임 처리하여 사용자가 입력한 값중에 특수문자나 공백이 포함되면 디코딩하여 제대로 된 형태로 표시
  if (authInfo && authInfo.nickname) {
    authInfo.nickname = decodeURIComponent(authInfo.nickname);
  }
  return authInfo;
};
export const loginPostAsync = createAsyncThunk("loginPostAsync", (param) => {
  return loginPost(param);
});

const loginSlice = createSlice({
  name: "LoginSlice",
  initialState: loadAuthCookie() || initState, // 쿠키가 없다면 초깃값 사용
  reducers: {
    login: (state, action) => {
      console.log("login....");
      // 소셜 로그인 회원이 사용
      const payload = action.payload;
      setCookie("auth", JSON.stringify(payload), 1); // 1일

      return payload;
    },
    logout: (state, action) => {
      console.log("logout....");
      removeCookie("auth");
      return { ...initState };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginPostAsync.fulfilled, (state, action) => {
        console.log("fulfilled");
        const payload = action.payload;

        // 정상적인 로그인시에만 저장
        if (!payload.error) {
          setCookie("auth", JSON.stringify(payload), 1); //1일
          // setCookie("auth", JSON.stringify(payload),1/24)
        }

        return payload;
      })
      .addCase(loginPostAsync.pending, (state, action) => {
        console.log("pending");
      })
      .addCase(loginPostAsync.rejected, (state, action) => {
        console.log("rejected");
      });
  },
});
export const { login, logout } = loginSlice.actions;
export default loginSlice.reducer;
