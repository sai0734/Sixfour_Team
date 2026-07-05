import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { getCartItems, postChangeCart } from "../api/cartApi";
import { logout } from "./loginSlice";

export const getCartItemsAsync = createAsyncThunk("getCartItemsAsync", () => {
  return getCartItems();
});

export const postChangeCartAsync = createAsyncThunk(
  "postCartItemsAsync",
  (param) => {
    return postChangeCart(param);
  },
);

// 로그인 시, 그동안 쌓아둔 비회원(게스트) 장바구니를 서버에 실제로 반영하고
// 서버 기준 장바구니로 새로 불러오는 액션
export const mergeGuestCartAsync = createAsyncThunk(
  "mergeGuestCartAsync",
  async (email, { getState }) => {
    const state = getState();
    const guestItems = state.cartSlice.filter((item) => item.isGuest);

    for (const item of guestItems) {
      await postChangeCart({
        email,
        pno: item.pno,
        pono: item.pono,
        qty: item.qty,
        cino: null,
      });
    }

    const serverItems = await getCartItems();
    return serverItems;
  },
);

const initState = [];

const makeGuestKey = (pno, pono) => `guest-${pno}-${pono ?? "none"}`;

const cartSlice = createSlice({
  name: "cartSlice",
  initialState: initState,

  reducers: {
    addOrChangeGuestItem: (state, action) => {
      const payload = action.payload;

      if (payload.cino) {
        const idx = state.findIndex((item) => item.cino === payload.cino);
        if (idx !== -1) {
          if (payload.qty <= 0) {
            state.splice(idx, 1);
          } else {
            state[idx] = { ...state[idx], qty: payload.qty };
          }
          return;
        }
      }

      const key = makeGuestKey(payload.pno, payload.pono);
      const existingIndex = state.findIndex(
        (item) => item.isGuest && makeGuestKey(item.pno, item.pono) === key,
      );

      if (payload.qty <= 0) {
        if (existingIndex !== -1) state.splice(existingIndex, 1);
        return;
      }

      if (existingIndex !== -1) {
        state[existingIndex] = { ...state[existingIndex], qty: payload.qty };
      } else {
        state.push({ ...payload, cino: key, isGuest: true });
      }
    },

    clearGuestCart: () => {
      return [];
    },
  },

  extraReducers: (builder) => {
    builder
      .addCase(getCartItemsAsync.fulfilled, (state, action) => {
        console.log("getCartItemsAsync fulfilled", action.payload);
        return Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(getCartItemsAsync.rejected, (state, action) => {
        console.error(
          "getCartItemsAsync REJECTED:",
          action.error,
          action.payload,
        );
        return state;
      })
      .addCase(postChangeCartAsync.fulfilled, (state, action) => {
        console.log("postCartItemsAsync fulfilled", action.payload);
        return Array.isArray(action.payload) ? action.payload : state;
      })
      .addCase(postChangeCartAsync.rejected, (state, action) => {
        console.error(
          "postChangeCartAsync REJECTED:",
          action.error,
          action.payload,
        );
        return state;
      })
      .addCase(mergeGuestCartAsync.fulfilled, (state, action) => {
        console.log("mergeGuestCartAsync fulfilled", action.payload);
        return Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(mergeGuestCartAsync.rejected, (state, action) => {
        console.error(
          "mergeGuestCartAsync REJECTED:",
          action.error,
          action.payload,
        );
        return state;
      })
      // 신규 추가: 로그아웃하면 장바구니(게스트든 회원이든) 상태를 완전히 비움
      // (다음에 로그인/비로그인 상태에서 새로 시작하도록)
      .addCase(logout, () => {
        console.log("cartSlice: logout 감지 -> 장바구니 초기화");
        return [];
      });
  },
});

export const { addOrChangeGuestItem, clearGuestCart } = cartSlice.actions;

export default cartSlice.reducer;
