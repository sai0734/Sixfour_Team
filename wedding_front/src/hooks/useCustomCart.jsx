import { useDispatch, useSelector } from "react-redux";
import {
  getCartItemsAsync,
  postChangeCartAsync,
  addOrChangeGuestItem,
} from "../slices/cartSlice";
import useCustomLogin from "./useCustomLogin";
import { postChangeCart } from "../api/cartApi";

const useCustomCart = () => {
  const cartItems = useSelector((state) => state.cartSlice);

  const dispatch = useDispatch();

  const { isLogin } = useCustomLogin();

  const refreshCart = () => {
    if (isLogin) {
      dispatch(getCartItemsAsync());
    }
  };

  const changeCart = (param) => {
    if (isLogin) {
      dispatch(postChangeCartAsync(param));
    } else {
      dispatch(addOrChangeGuestItem(param));
    }
  };

  // 여러 아이템을 순서대로 삭제한 뒤, 마지막에 한 번만 최신 목록으로 갱신
  // (동시에 여러 요청을 날리면 응답 순서가 뒤섞여 화면이 잠깐 이상해지는 문제를 방지)
  const removeMultiple = async (items, email) => {
    if (!isLogin) {
      // 비회원(게스트)은 로컬 상태라 순서 문제 자체가 없어서 그냥 하나씩 반영
      items.forEach((item) => {
        dispatch(
          addOrChangeGuestItem({
            email,
            cino: item.cino,
            pno: item.pno,
            pono: item.pono,
            qty: 0,
          }),
        );
      });
      return;
    }

    // 회원은 서버 API를 직접, 순서대로(await) 호출해서 경쟁 상태를 없앰
    for (const item of items) {
      await postChangeCart({
        email,
        cino: item.cino,
        pno: item.pno,
        pono: item.pono,
        qty: 0,
      });
    }

    // 전부 끝난 뒤 딱 한 번만 서버 기준 최신 목록으로 갱신
    dispatch(getCartItemsAsync());
  };

  return { cartItems, refreshCart, changeCart, removeMultiple };
};

export default useCustomCart;
