import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import useCustomCart from "../../hooks/useCustomCart";
import useCustomLogin from "../../hooks/useCustomLogin";
import { API_SERVER_HOST } from "../../api/reservationApi";
import FetchingModal from "../../components/common/FetchingModal";
import { calculateShippingFee } from "../../util/shippingPolicy";
import BasicLayout from "../../layouts/BasicLayout";
import ShopTapeLabel from "../../components/product/ShopTapeLabel";
import { showAlert } from "../../util/globalAlert";
import { showConfirm } from "../../util/globalConfirm";

const host = API_SERVER_HOST;

const CartPage = () => {
  const { cartItems, refreshCart, changeCart, removeMultiple } =
    useCustomCart();
  const { loginState, moveToLogin } = useCustomLogin();
  const navigate = useNavigate();
  const [fetching, setFetching] = useState(false);

  const safeCartItems = Array.isArray(cartItems) ? cartItems : [];

  const [selectedCinos, setSelectedCinos] = useState([]);

  useEffect(() => {
    if (loginState.email) {
      setFetching(true);
      refreshCart();
    }
  }, [loginState.email]);

  useEffect(() => {
    setFetching(false);
    setSelectedCinos(safeCartItems.map((item) => item.cino));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cartItems]);

  const handleToggleSelect = (cino) => {
    setSelectedCinos((prev) =>
      prev.includes(cino) ? prev.filter((c) => c !== cino) : [...prev, cino],
    );
  };

  const isAllSelected =
    safeCartItems.length > 0 && selectedCinos.length === safeCartItems.length;

  const handleToggleSelectAll = () => {
    if (isAllSelected) {
      setSelectedCinos([]);
    } else {
      setSelectedCinos(safeCartItems.map((item) => item.cino));
    }
  };

  const handleChangeQty = (item, amount) => {
    const nextQty = item.qty + amount;
    if (nextQty < 1) return;

    changeCart({
      email: loginState.email,
      cino: item.cino,
      pno: item.pno,
      pono: item.pono,
      qty: nextQty,
      pname: item.pname,
      price: item.price,
      imageFile: item.imageFile,
      optionName: item.optionName,
      optionValue: item.optionValue,
      extraPrice: item.extraPrice,
    });
  };

  const handleRemove = (item) => {
    changeCart({
      email: loginState.email,
      cino: item.cino,
      pno: item.pno,
      pono: item.pono,
      qty: 0,
    });
  };

  const handleClickDeleteSelected = async () => {
    if (selectedCinos.length === 0) {
      showAlert("삭제할 상품을 선택해주세요.");
      return;
    }
    if (
      !(await showConfirm(
        `선택한 ${selectedCinos.length}개 상품을 삭제하시겠습니까?`,
      ))
    )
      return;

    const targets = safeCartItems.filter((item) =>
      selectedCinos.includes(item.cino),
    );

    setFetching(true);
    await removeMultiple(targets, loginState.email);
    setFetching(false);
  };

  const selectedItems = safeCartItems.filter((item) =>
    selectedCinos.includes(item.cino),
  );

  const productSubtotal = selectedItems.reduce(
    (sum, item) => sum + (item.price + (item.extraPrice || 0)) * item.qty,
    0,
  );

  const shippingFee =
    productSubtotal > 0 ? calculateShippingFee(productSubtotal) : 0;
  const grandTotal = productSubtotal + shippingFee;

  const handleClickCheckout = () => {
    if (!loginState.email) {
      showAlert(
        "결제하려면 로그인이 필요합니다. 담아둔 장바구니는 로그인 후에도 그대로 유지됩니다.",
        () => {
          moveToLogin("/cart");
        },
      );
      return;
    }
    if (selectedCinos.length === 0) {
      showAlert("결제할 상품을 선택해주세요.");
      return;
    }

    navigate("/checkout", { state: { selectedCinos } });
  };

  return (
    <BasicLayout showCart={false}>
      <div className="-mx-5 -mb-10 -mt-12 min-h-[calc(100vh-6rem)] bg-cream px-5 pt-16">
        {fetching ? <FetchingModal /> : <></>}

        <div className="max-w-[900px] mx-auto px-6 pb-20">
          <ShopTapeLabel className="mb-4">CART</ShopTapeLabel>
          <p className="font-['Gowun_Batang'] text-2xl mb-8 text-ink">
            장바구니
          </p>

          {!loginState.email && safeCartItems.length > 0 && (
            <p className="text-xs text-ink-faint mb-6">
              비회원 장바구니입니다. 로그인하면 지금 담긴 상품이 그대로
              이어집니다.
            </p>
          )}

          {safeCartItems.length === 0 ? (
            <div className="bg-white rounded-2xl py-20 text-center text-ink-faint text-sm shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
              장바구니가 비어있습니다.
            </div>
          ) : (
            <div className="grid grid-cols-[1fr_300px] gap-10">
              <div className="bg-white rounded-2xl p-5 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] h-fit">
                <div className="flex justify-between items-center pb-3 border-b border-line">
                  <label className="flex items-center gap-2 text-sm cursor-pointer">
                    <input
                      type="checkbox"
                      checked={isAllSelected}
                      onChange={handleToggleSelectAll}
                      className="accent-brand"
                    />
                    전체선택 ({selectedCinos.length}/{safeCartItems.length})
                  </label>
                  <button
                    onClick={handleClickDeleteSelected}
                    className="text-xs text-ink-faint underline"
                  >
                    선택삭제
                  </button>
                </div>

                {safeCartItems.map((item) => (
                  <div
                    key={item.cino}
                    className="flex items-center gap-4 py-4 border-b border-line"
                  >
                    <input
                      type="checkbox"
                      checked={selectedCinos.includes(item.cino)}
                      onChange={() => handleToggleSelect(item.cino)}
                      className="accent-brand flex-shrink-0"
                    />

                    <div className="w-16 h-16 rounded-xl overflow-hidden bg-surface flex-shrink-0">
                      <img
                        alt={item.pname}
                        className="w-full h-full object-cover"
                        src={`${host}/api/product/view/s_${item.imageFile}`}
                      />
                    </div>

                    <div className="flex-1">
                      <p className="text-sm">{item.pname}</p>
                      {item.optionValue && (
                        <p className="text-xs text-ink-faint mt-0.5">
                          {item.optionName}: {item.optionValue}
                          {item.extraPrice > 0 &&
                            ` (+${item.extraPrice.toLocaleString()}원)`}
                        </p>
                      )}
                    </div>

                    <div className="flex items-center gap-3 border border-line-soft rounded-full px-3 py-1 text-sm">
                      <button onClick={() => handleChangeQty(item, -1)}>
                        −
                      </button>
                      <span>{item.qty}</span>
                      <button onClick={() => handleChangeQty(item, 1)}>
                        +
                      </button>
                    </div>

                    <p className="w-24 text-right text-sm font-medium">
                      {(
                        (item.price + (item.extraPrice || 0)) *
                        item.qty
                      ).toLocaleString()}
                      원
                    </p>

                    <button
                      onClick={() => handleRemove(item)}
                      className="text-ink-faint text-xs"
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>

              <div className="bg-white rounded-2xl p-6 h-fit shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)]">
                <p className="text-sm font-medium text-brand-deep mb-4">
                  결제 예정 금액
                </p>
                <div className="flex justify-between text-sm text-ink-soft mb-2">
                  <span>상품금액</span>
                  <span>{productSubtotal.toLocaleString()}원</span>
                </div>
                <div className="flex justify-between text-sm text-ink-soft mb-2">
                  <span>배송비</span>
                  <span>
                    {shippingFee === 0
                      ? "무료"
                      : `${shippingFee.toLocaleString()}원`}
                  </span>
                </div>
                <div className="border-t border-line mt-3 pt-3 flex justify-between items-baseline">
                  <span className="text-sm text-ink-soft">총액</span>
                  <span className="text-xl font-medium text-brand-deep">
                    {grandTotal.toLocaleString()}원
                  </span>
                </div>
                <button
                  onClick={handleClickCheckout}
                  className="group relative w-full h-11 mt-4 overflow-hidden rounded-full bg-brand text-sm font-medium text-white transition-all duration-200 ease-out hover:-translate-y-0.5 hover:bg-brand-deep hover:shadow-[0_10px_28px_-10px_rgba(198,112,112,0.55)] active:translate-y-0 active:scale-[0.98]"
                >
                  <span className="relative z-10">
                    결제하기 ({selectedCinos.length})
                  </span>
                  <span
                    aria-hidden="true"
                    className="pointer-events-none absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/20 to-transparent transition-transform duration-500 ease-out group-hover:translate-x-full"
                  />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </BasicLayout>
  );
};

export default CartPage;
