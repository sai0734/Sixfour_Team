import { useEffect, useRef, useState } from "react";
import { postAdd, getCategories } from "../../api/productApi";
import FetchingModal from "../common/FetchingModal";
import useCustomMove from "../../hooks/useCustomMove";

const initState = {
  pname: "",
  category: "",
  price: "",
  stockQty: "",
  pdesc: "",
};

const AddComponent = () => {
  const [product, setProduct] = useState({ ...initState });
  const [categoryList, setCategoryList] = useState([]);
  const [previews, setPreviews] = useState([]);
  const uploadRef = useRef();

  const [fetching, setFetching] = useState(false);
  const { moveToModify, moveToList } = useCustomMove();

  useEffect(() => {
    getCategories().then((data) => setCategoryList(data));
  }, []);

  const handleChangeProduct = (e) => {
    setProduct({ ...product, [e.target.name]: e.target.value });
  };

  // 선택한 이미지 파일들을 즉시 미리보기로 보여줌
  const handleChangeFiles = (e) => {
    const files = Array.from(e.target.files);
    setPreviews(files.map((file) => URL.createObjectURL(file)));
  };

  const handleClickAdd = () => {
    if (!product.pname.trim()) {
      alert("상품명을 입력해주세요.");
      return;
    }
    if (!product.category.trim()) {
      alert("카테고리를 입력해주세요.");
      return;
    }

    const files = uploadRef.current.files;
    const formData = new FormData();

    for (let i = 0; i < files.length; i++) {
      formData.append("files", files[i]);
    }

    formData.append("pname", product.pname);
    formData.append("pdesc", product.pdesc);
    formData.append("price", product.price || 0);
    formData.append("category", product.category);
    formData.append("stockQty", product.stockQty || 0);

    setFetching(true);

    postAdd(formData)
      .then((data) => {
        setFetching(false);
        alert(
          "상품이 등록되었습니다. 이어서 옵션을 추가하려면 수정 페이지로 이동합니다.",
        );
        // 옵션은 pno가 있어야 등록 가능해서(백엔드 /{pno}/options 구조),
        // 등록 직후 바로 수정페이지로 보내서 옵션까지 이어서 넣을 수 있게 함
        moveToModify(data.result);
      })
      .catch((err) => {
        setFetching(false);
        alert("상품 등록에 실패했습니다.");
        console.error(err);
      });
  };

  return (
    <div className="-mx-5 -mb-10 -mt-12 min-h-[calc(100vh-6rem)] bg-cream px-5 pt-16 text-ink">
      {fetching ? <FetchingModal /> : <></>}

      <div className="max-w-[900px] mx-auto px-6 pb-16">
        <span className="inline-block -rotate-2 bg-blush-100 px-3 py-1 mb-4 font-['Gaegu'] text-[13px] text-brand-deep">
          관리자
        </span>
        <p className="font-['Gowun_Batang'] text-2xl mb-8">상품 등록</p>

        {/* 기본 정보 카드 */}
        <div className="bg-white rounded-2xl p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
          <p className="text-sm font-medium mb-4">기본 정보</p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="text-xs text-ink-soft mb-1.5 block">
                상품명 *
              </label>
              <input
                name="pname"
                type="text"
                value={product.pname}
                onChange={handleChangeProduct}
                placeholder="예: 송월 수건 & 주방타올 답례품 세트"
                className="w-full h-10 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
              />
            </div>

            <div>
              <label className="text-xs text-ink-soft mb-1.5 block">
                카테고리 *
              </label>
              <input
                name="category"
                type="text"
                list="category-options"
                value={product.category}
                onChange={handleChangeProduct}
                placeholder="기존 카테고리 선택 또는 새로 입력"
                className="w-full h-10 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
              />
              <datalist id="category-options">
                {categoryList.map((cat) => (
                  <option key={cat} value={cat} />
                ))}
              </datalist>
            </div>

            <div>
              <label className="text-xs text-ink-soft mb-1.5 block">
                가격(원)
              </label>
              <input
                name="price"
                type="number"
                min="0"
                value={product.price}
                onChange={handleChangeProduct}
                placeholder="0"
                className="w-full h-10 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
              />
            </div>

            <div>
              <label className="text-xs text-ink-soft mb-1.5 block">
                재고 수량
              </label>
              <input
                name="stockQty"
                type="number"
                min="0"
                value={product.stockQty}
                onChange={handleChangeProduct}
                placeholder="0"
                className="w-full h-10 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
              />
            </div>

            <div className="md:col-span-2">
              <label className="text-xs text-ink-soft mb-1.5 block">
                상품 설명
              </label>
              <textarea
                name="pdesc"
                rows={5}
                value={product.pdesc}
                onChange={handleChangeProduct}
                placeholder="상품에 대한 설명을 입력해주세요."
                className="w-full p-3 border border-line-soft rounded-lg text-sm resize-none focus:outline-none focus:border-brand"
              />
            </div>
          </div>
        </div>

        {/* 이미지 카드 */}
        <div className="bg-white rounded-2xl p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
          <p className="text-sm font-medium mb-4">상품 이미지</p>
          <input
            ref={uploadRef}
            type="file"
            multiple
            accept="image/*"
            onChange={handleChangeFiles}
            className="text-sm mb-4"
          />

          {previews.length > 0 && (
            <div className="grid grid-cols-3 md:grid-cols-4 gap-3">
              {previews.map((src, i) => (
                <div
                  key={i}
                  className="aspect-square rounded-lg overflow-hidden bg-surface"
                >
                  <img
                    alt=""
                    src={src}
                    className="w-full h-full object-cover"
                  />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 옵션 안내 */}
        <div className="bg-lavender-light/40 rounded-2xl p-4 mb-6 text-xs text-lavender-dark">
          💡 포장/색상 같은 옵션은 상품을 먼저 등록한 뒤, 이어지는 수정
          페이지에서 추가할 수 있어요.
        </div>

        <div className="flex justify-end gap-2">
          <button
            onClick={() => moveToList({ page: 1 })}
            className="h-11 px-6 rounded-full border border-line-soft text-sm"
          >
            취소
          </button>
          <button
            onClick={handleClickAdd}
            className="h-11 px-6 rounded-full bg-brand text-white text-sm font-medium"
          >
            등록하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default AddComponent;
