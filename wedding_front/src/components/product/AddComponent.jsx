import { useEffect, useRef, useState } from "react";
import { postAdd, getCategories, postOption } from "../../api/productApi";
import FetchingModal from "../common/FetchingModal";
import useCustomMove from "../../hooks/useCustomMove";

const initState = {
  pname: "",
  category: "",
  price: "",
  stockQty: "",
  pdesc: "",
};

const emptyNewOption = { optionName: "", optionValue: "", extraPrice: "" };

const AddComponent = () => {
  const [product, setProduct] = useState({ ...initState });
  const [categoryList, setCategoryList] = useState([]);
  const [previews, setPreviews] = useState([]);
  const uploadRef = useRef();

  const [pendingOptions, setPendingOptions] = useState([]);
  const [newOption, setNewOption] = useState({ ...emptyNewOption });

  const [fetching, setFetching] = useState(false);
  const { moveToList } = useCustomMove();

  useEffect(() => {
    getCategories().then((data) => setCategoryList(data));
  }, []);

  const handleChangeProduct = (e) => {
    setProduct({ ...product, [e.target.name]: e.target.value });
  };

  const handleChangeFiles = (e) => {
    const files = Array.from(e.target.files);
    setPreviews(files.map((file) => URL.createObjectURL(file)));
  };

  const handleChangeExtraPrice = (value) => {
    setNewOption({
      ...newOption,
      extraPrice: value.replace(/[^0-9]/g, ""),
    });
  };

  const handleAddPendingOption = () => {
    if (!newOption.optionName.trim() || !newOption.optionValue.trim()) {
      alert("옵션명과 옵션값을 입력해주세요.");
      return;
    }

    setPendingOptions((prev) => [
      ...prev,
      {
        tempId: Date.now(),
        optionName: newOption.optionName.trim(),
        optionValue: newOption.optionValue.trim(),
        extraPrice: newOption.extraPrice,
      },
    ]);
    setNewOption({ ...emptyNewOption });
  };

  const handleRemovePendingOption = (tempId) => {
    setPendingOptions((prev) => prev.filter((opt) => opt.tempId !== tempId));
  };

  const handleClickAdd = async () => {
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

    try {
      const data = await postAdd(formData);
      const pno = data.result;

      if (pendingOptions.length > 0) {
        await Promise.all(
          pendingOptions.map((opt) =>
            postOption(pno, {
              optionName: opt.optionName,
              optionValue: opt.optionValue,
              extraPrice: Number(opt.extraPrice) || 0,
            }),
          ),
        );
      }

      alert(
        pendingOptions.length > 0
          ? "상품과 옵션이 등록되었습니다."
          : "상품이 등록되었습니다.",
      );
      moveToList({ page: 1 });
    } catch (err) {
      alert("상품 등록에 실패했습니다.");
      console.error(err);
    } finally {
      setFetching(false);
    }
  };

  return (
    <div className="-mx-5 -mb-10 -mt-12 min-h-[calc(100vh-6rem)] bg-cream px-5 pt-16 text-ink">
      {fetching ? <FetchingModal /> : <></>}

      <div className="max-w-[900px] mx-auto px-6 pb-16">
        <span className="inline-block -rotate-2 bg-blush-100 px-3 py-1 mb-4 font-['Gaegu'] text-[13px] text-brand-deep">
          관리자
        </span>
        <p className="font-['Gowun_Batang'] text-2xl mb-8">상품 등록</p>

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

        <div className="bg-white rounded-2xl p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
          <p className="text-sm font-medium mb-4">옵션 관리</p>

          {pendingOptions.length > 0 && (
            <div className="flex flex-col gap-2 mb-4">
              {pendingOptions.map((opt) => (
                <div
                  key={opt.tempId}
                  className="flex flex-wrap gap-3 items-center justify-between border border-line rounded-lg p-3 text-sm"
                >
                  <div className="flex gap-3">
                    <span className="text-ink-soft">{opt.optionName}</span>
                    <span>{opt.optionValue}</span>
                    {Number(opt.extraPrice) > 0 && (
                      <span className="text-brand-deep">
                        +{Number(opt.extraPrice).toLocaleString()}원
                      </span>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => handleRemovePendingOption(opt.tempId)}
                    className="text-xs text-ink-faint underline"
                  >
                    삭제
                  </button>
                </div>
              ))}
            </div>
          )}

          <div className="flex flex-wrap gap-2 items-center bg-cream/60 border border-dashed border-line rounded-lg p-3">
            <input
              type="text"
              value={newOption.optionName}
              onChange={(e) =>
                setNewOption({ ...newOption, optionName: e.target.value })
              }
              placeholder="옵션명 (예: 포장)"
              className="h-9 px-2.5 border border-line-soft rounded-md text-xs w-28"
            />
            <input
              type="text"
              value={newOption.optionValue}
              onChange={(e) =>
                setNewOption({ ...newOption, optionValue: e.target.value })
              }
              placeholder="옵션값 (예: 고급포장)"
              className="h-9 px-2.5 border border-line-soft rounded-md text-xs flex-1 min-w-[120px]"
            />
            <input
              type="text"
              inputMode="numeric"
              value={newOption.extraPrice}
              onChange={(e) => handleChangeExtraPrice(e.target.value)}
              placeholder="추가금액"
              className="h-9 px-2.5 border border-line-soft rounded-md text-xs w-24"
            />
            <button
              type="button"
              onClick={handleAddPendingOption}
              className="h-9 px-4 rounded-full bg-lavender-dark text-white text-xs"
            >
              가격 추가
            </button>
          </div>
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
