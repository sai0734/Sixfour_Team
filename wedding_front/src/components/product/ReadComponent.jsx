import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getOne, getOptions } from "../../api/productApi";
import { postAdd, deleteWish, isWished } from "../../api/wishApi";
import { API_SERVER_HOST } from "../../api/reservationApi";
import useCustomMove from "../../hooks/useCustomMove";
import FetchingModal from "../common/FetchingModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import useCustomCart from "../../hooks/useCustomCart";
import ProductGalleryComponent from "./ProductGalleryComponent";
import ProductDetailInfoComponent from "./ProductDetailInfoComponent";
import ReviewSectionComponent from "./ReviewSectionComponent";
import QnaSectionComponent from "./QnaSectionComponent";
import ShippingPolicyComponent from "./ShippingPolicyComponent";
import RelatedProductsComponent from "./RelatedProductsComponent";

const initState = {
  pno: 0,
  pname: "",
  pdesc: "",
  price: 0,
  category: "",
  uploadFileNames: [],
};

const host = API_SERVER_HOST;

const SECTIONS = [
  { key: "desc", label: "상세정보" },
  { key: "review", label: "리뷰" },
  { key: "qna", label: "Q&A" },
  { key: "shipping", label: "배송안내" },
];

const ReadComponent = ({ pno }) => {
  const [product, setProduct] = useState(initState);
  const [options, setOptions] = useState([]);
  const [selectedOption, setSelectedOption] = useState(null);
  const [qty, setQty] = useState(1);
  const [activeSection, setActiveSection] = useState("desc");

  const [wished, setWished] = useState(false);
  const [fetching, setFetching] = useState(false);

  const [headerHeight, setHeaderHeight] = useState(0);

  useEffect(() => {
    const headerEl = document.getElementById("mainNav");
    if (!headerEl) return;

    const updateHeight = () => {
      setHeaderHeight(headerEl.getBoundingClientRect().height);
    };

    updateHeight();

    const observer = new ResizeObserver(updateHeight);
    observer.observe(headerEl);

    return () => observer.disconnect();
  }, []);

  const { moveToList, moveToModify } = useCustomMove();
  const { changeCart } = useCustomCart();
  const { loginState } = useCustomLogin();
  const navigate = useNavigate();

  const isAdmin = loginState.roleNames?.includes("ADMIN");

  const sectionRefs = useRef({});

  const loadProduct = () => {
    return getOne(pno).then((data) => setProduct(data));
  };

  useEffect(() => {
    setFetching(true);

    Promise.all([getOne(pno), getOptions(pno)]).then(
      ([productData, optionData]) => {
        setProduct(productData);
        setOptions(optionData);
        setSelectedOption(optionData.length > 0 ? optionData[0] : null);
        setFetching(false);
      },
    );
  }, [pno]);

  useEffect(() => {
    window.scrollTo(0, 0);
    setActiveSection("desc");
  }, [pno]);

  useEffect(() => {
    if (!loginState.email) return;

    isWished(pno).then((data) => setWished(data.wished));
  }, [pno, loginState.email]);

  const totalPrice = useMemo(() => {
    const extra = selectedOption ? selectedOption.extraPrice : 0;
    return (product.price + extra) * qty;
  }, [product.price, selectedOption, qty]);

  const handleClickWish = () => {
    if (!loginState.email) {
      alert("로그인이 필요한 기능입니다.");
      return;
    }

    const action = wished ? deleteWish(pno) : postAdd(pno);

    action.then(() => setWished(!wished));
  };

  const handleClickAddCart = () => {
    changeCart({
      email: loginState.email,
      pno: pno,
      pono: selectedOption ? selectedOption.pono : null,
      qty: qty,
      pname: product.pname,
      price: product.price,
      imageFile: product.uploadFileNames?.[0],
      optionName: selectedOption?.optionName ?? null,
      optionValue: selectedOption?.optionValue ?? null,
      extraPrice: selectedOption?.extraPrice ?? 0,
    });

    const goToCart = window.confirm(
      "장바구니에 담았습니다. 장바구니로 이동하시겠습니까?",
    );

    if (goToCart) {
      navigate("/cart");
    }
  };

  const handleClickBuyNow = () => {
    if (!loginState.email) {
      alert("로그인이 필요한 기능입니다.");
      return;
    }

    const directItem = {
      cino: null,
      pno: pno,
      pono: selectedOption ? selectedOption.pono : null,
      pname: product.pname,
      price: product.price,
      qty: qty,
      imageFile: product.uploadFileNames?.[0],
      optionName: selectedOption?.optionName ?? null,
      optionValue: selectedOption?.optionValue ?? null,
      extraPrice: selectedOption?.extraPrice ?? 0,
    };

    navigate("/checkout", { state: { directItem } });
  };

  const handleClickSection = (key) => {
    setActiveSection(key);

    const el = sectionRefs.current[key];
    if (el) {
      const top =
        el.getBoundingClientRect().top + window.scrollY - (headerHeight + 50);
      window.scrollTo({ top, behavior: "smooth" });
    }
  };

  return (
    <div className="-mx-5 -mb-10 -mt-12 min-h-[calc(100vh-6rem)] bg-cream pt-16 text-ink">
      {fetching ? <FetchingModal /> : <></>}

      <p className="max-w-[1320px] mx-auto px-6 text-xs text-ink-faint">
        답례품 쇼핑몰 {product.category ? `> ${product.category}` : ""} {" > "}
        <span className="text-ink-soft">{product.pname}</span>
      </p>

      <div className="max-w-[1320px] mx-auto px-6 pt-5 grid grid-cols-1 gap-8 lg:grid-cols-[460px_1fr] lg:gap-14">
        <ProductGalleryComponent
          pname={product.pname}
          uploadFileNames={product.uploadFileNames}
          host={host}
        />

        <div>
          <span className="inline-block -rotate-2 bg-blush-100 px-3 py-1 mb-3 font-['Gaegu'] text-[13px] text-brand-deep">
            {product.category}
          </span>

          <p className="font-['Gowun_Batang'] text-2xl mb-1.5">
            {product.pname}
          </p>

          <p className="text-xs text-ink-faint flex items-center gap-1 mb-3">
            {"★".repeat(Math.round(product.ratingAvg || 0))}
            <span className="text-line-soft">
              {"★".repeat(5 - Math.round(product.ratingAvg || 0))}
            </span>
            {product.ratingAvg?.toFixed(1)} · 후기 {product.reviewCount}개
          </p>
          <p className="text-2xl font-medium mb-5">
            {product.price?.toLocaleString()}원
          </p>

          {options.length > 0 && (
            <div className="border-t border-line pt-4 mb-4">
              <p className="text-xs text-ink-soft mb-2">
                {options[0]?.optionName ?? "옵션"}
              </p>
              <div className="flex gap-2 flex-wrap mb-4">
                {options.map((opt) => (
                  <span
                    key={opt.pono}
                    onClick={() => setSelectedOption(opt)}
                    className={`border rounded-full px-3.5 py-1.5 text-xs cursor-pointer ${
                      selectedOption?.pono === opt.pono
                        ? "border-brand text-brand-deep bg-brand-light font-medium"
                        : "border-line-soft"
                    }`}
                  >
                    {opt.optionValue}
                    {opt.extraPrice > 0 &&
                      ` +${opt.extraPrice.toLocaleString()}원`}
                  </span>
                ))}
              </div>
            </div>
          )}

          <p className="text-xs text-ink-soft mb-2">수량</p>
          <div className="flex items-center gap-3.5 border border-line-soft rounded-full w-fit px-4 py-1.5 text-sm mb-5">
            <button onClick={() => setQty((q) => Math.max(1, q - 1))}>−</button>
            <span>{qty}</span>
            <button onClick={() => setQty((q) => q + 1)}>+</button>
          </div>

          <div className="border-t border-line pt-4 mb-4 flex justify-between items-baseline">
            <span className="text-sm text-ink-soft">총 상품금액</span>
            <span className="text-xl font-medium">
              {totalPrice.toLocaleString()}원
            </span>
          </div>

          <div className="flex gap-2.5">
            <button
              onClick={handleClickWish}
              className="w-[46px] h-[46px] shrink-0 border border-line-soft rounded-full flex items-center justify-center"
            >
              <svg
                viewBox="0 0 24 24"
                className="w-[18px] h-[18px]"
                fill={wished ? "#C87070" : "none"}
                stroke="#C87070"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M19.5 12.572 12 20l-7.5-7.428a5 5 0 1 1 7.5-6.566 5 5 0 1 1 7.5 6.566Z" />
              </svg>
            </button>
            <button
              onClick={handleClickAddCart}
              className="flex-1 h-[46px] rounded-full border border-line-soft text-sm font-medium"
            >
              장바구니 담기
            </button>
            <button
              onClick={handleClickBuyNow}
              className="flex-1 h-[46px] rounded-full bg-brand text-white text-sm font-medium"
            >
              바로 구매
            </button>
          </div>

          {isAdmin && (
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => moveToModify(pno)}
                className="text-xs text-ink-faint underline"
              >
                상품 수정 (관리자)
              </button>
            </div>
          )}
        </div>
      </div>

      <div
        className="sticky bg-white z-10 border-b border-line mt-14 -mx-5"
        style={{ top: `${headerHeight}px` }}
      >
        <div className="max-w-[1320px] mx-auto px-6 flex gap-5 md:gap-7 text-sm overflow-x-auto">
          {SECTIONS.map((sec) => (
            <span
              key={sec.key}
              onClick={() => handleClickSection(sec.key)}
              className={`py-3.5 cursor-pointer border-b-2 whitespace-nowrap ${
                activeSection === sec.key
                  ? "text-ink font-medium border-brand"
                  : "text-ink-faint border-transparent"
              }`}
            >
              {sec.label}
            </span>
          ))}
        </div>
      </div>

      <div className="max-w-[1320px] mx-auto px-6">
        <div
          ref={(el) => (sectionRefs.current.desc = el)}
          className="py-8 md:py-10 border-b border-line"
        >
          <p className="text-sm font-medium mb-4">상세정보</p>
          <ProductDetailInfoComponent
            pdesc={product.pdesc}
            uploadFileNames={product.uploadFileNames}
            host={host}
          />
        </div>

        <div
          ref={(el) => (sectionRefs.current.review = el)}
          className="py-8 md:py-10 border-b border-line"
        >
          <p className="text-sm font-medium mb-4">
            리뷰 ({product.reviewCount || 0})
          </p>
          <ReviewSectionComponent
            pno={pno}
            host={host}
            isLoggedIn={!!loginState.email}
            isAdmin={isAdmin}
            myEmail={loginState.email}
            onStatsChange={loadProduct}
          />
        </div>

        <div
          ref={(el) => (sectionRefs.current.qna = el)}
          className="py-8 md:py-10 border-b border-line"
        >
          <p className="text-sm font-medium mb-4">Q&A</p>
          <QnaSectionComponent
            pno={pno}
            isLoggedIn={!!loginState.email}
            isAdmin={isAdmin}
            myEmail={loginState.email}
          />
        </div>

        <div
          ref={(el) => (sectionRefs.current.shipping = el)}
          className="py-8 md:py-10 mb-10"
        >
          <p className="text-sm font-medium mb-4">배송안내</p>
          <ShippingPolicyComponent />
        </div>
      </div>

      <RelatedProductsComponent
        currentPno={pno}
        category={product.category}
        host={host}
        onClickProduct={(newPno) => navigate(`/product/read/${newPno}`)}
      />

      <div className="max-w-[1320px] mx-auto px-6 pb-16 flex justify-end">
        <button
          onClick={moveToList}
          className="h-11 px-6 rounded-full border border-line-soft text-sm"
        >
          목록으로
        </button>
      </div>
    </div>
  );
};

export default ReadComponent;
