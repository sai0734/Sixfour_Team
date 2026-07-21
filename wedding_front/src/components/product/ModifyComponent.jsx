import { useEffect, useRef, useState } from "react";
import {
  deleteOne,
  getOne,
  getOptions,
  getCategories,
  putOne,
  postOption,
  putOption,
  deleteOption,
} from "../../api/productApi";
import FetchingModal from "../common/FetchingModal";
import { API_SERVER_HOST } from "../../api/reservationApi";
import useCustomMove from "../../hooks/useCustomMove";
import ShopTapeLabel from "./ShopTapeLabel";

const host = API_SERVER_HOST;

const initState = {
  pno: 0,
  pname: "",
  category: "",
  price: 0,
  stockQty: 0,
  pdesc: "",
  delFlag: false,
  uploadFileNames: [],
};

const emptyNewOption = { optionName: "", optionValue: "", extraPrice: "" };

const ModifyComponent = ({ pno }) => {
  const [product, setProduct] = useState(initState);
  const [categoryList, setCategoryList] = useState([]);
  const [options, setOptions] = useState([]);
  const [newOption, setNewOption] = useState({ ...emptyNewOption });
  const [editingPono, setEditingPono] = useState(null);
  const [editingOption, setEditingOption] = useState(null);

  const { moveToRead, moveToList } = useCustomMove();
  const [fetching, setFetching] = useState(false);
  const uploadRef = useRef();

  const fetchAll = () => {
    setFetching(true);
    Promise.all([getOne(pno), getOptions(pno), getCategories()]).then(
      ([productData, optionData, categoryData]) => {
        setProduct(productData);
        setOptions(optionData);
        setCategoryList(categoryData);
        setFetching(false);
      },
    );
  };

  useEffect(() => {
    fetchAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pno]);

  const handleChangeProduct = (e) => {
    setProduct({ ...product, [e.target.name]: e.target.value });
  };

  const deleteOldImages = (imageName) => {
    setProduct({
      ...product,
      uploadFileNames: product.uploadFileNames.filter(
        (fileName) => fileName !== imageName,
      ),
    });
  };

  const handleClickModify = () => {
    const files = uploadRef.current.files;
    const formData = new FormData();

    for (let i = 0; i < files.length; i++) {
      formData.append("files", files[i]);
    }

    formData.append("pname", product.pname);
    formData.append("pdesc", product.pdesc);
    formData.append("price", product.price);
    formData.append("category", product.category);
    formData.append("stockQty", product.stockQty);
    formData.append("delFlag", product.delFlag);

    for (let i = 0; i < product.uploadFileNames.length; i++) {
      formData.append("uploadFileNames", product.uploadFileNames[i]);
    }

    setFetching(true);

    putOne(pno, formData).then(() => {
      setFetching(false);
      alert("수정되었습니다.");
      moveToRead(pno);
    });
  };

  const handleClickDelete = () => {
    if (!window.confirm("정말 이 상품을 삭제(숨김) 처리하시겠습니까?")) return;

    setFetching(true);
    deleteOne(pno).then(() => {
      setFetching(false);
      alert("삭제되었습니다.");
      moveToList({ page: 1 });
    });
  };

  const handleChangeNewExtraPrice = (value) => {
    setNewOption({
      ...newOption,
      extraPrice: value.replace(/[^0-9]/g, ""),
    });
  };

  const handleChangeEditingExtraPrice = (value) => {
    setEditingOption({
      ...editingOption,
      extraPrice: value.replace(/[^0-9]/g, ""),
    });
  };

  const handleAddOption = () => {
    if (!newOption.optionName.trim() || !newOption.optionValue.trim()) {
      alert("옵션명과 옵션값을 입력해주세요.");
      return;
    }

    postOption(pno, {
      optionName: newOption.optionName,
      optionValue: newOption.optionValue,
      extraPrice: Number(newOption.extraPrice) || 0,
    }).then(() => {
      setNewOption({ ...emptyNewOption });
      getOptions(pno).then((data) => setOptions(data));
    });
  };

  const handleSaveEditOption = (opt) => {
    putOption(pno, opt.pono, {
      optionName: editingOption.optionName,
      optionValue: editingOption.optionValue,
      extraPrice: Number(editingOption.extraPrice) || 0,
    }).then(() => {
      setEditingPono(null);
      getOptions(pno).then((data) => setOptions(data));
    });
  };

  const handleDeleteOption = (pono) => {
    if (!window.confirm("이 옵션을 삭제하시겠습니까?")) return;

    deleteOption(pno, pono).then(() => {
      getOptions(pno).then((data) => setOptions(data));
    });
  };

  return (
    <div className="-mx-5 -mb-10 -mt-12 min-h-[calc(100vh-6rem)] bg-cream px-5 pt-16 text-ink">
      {fetching ? <FetchingModal /> : <></>}

      <div className="max-w-[900px] mx-auto px-6 pb-16">
        <ShopTapeLabel className="mb-4">관리자</ShopTapeLabel>
        <p className="font-['Gowun_Batang'] text-2xl mb-8">
          상품 수정 <span className="text-ink-faint text-base">#{pno}</span>
        </p>

        <div className="bg-white rounded-2xl p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
          <p className="text-sm font-medium mb-4">기본 정보</p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="text-xs text-ink-soft mb-1.5 block">
                상품명
              </label>
              <input
                name="pname"
                type="text"
                value={product.pname}
                onChange={handleChangeProduct}
                className="w-full h-10 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
              />
            </div>

            <div>
              <label className="text-xs text-ink-soft mb-1.5 block">
                카테고리
              </label>
              <input
                name="category"
                type="text"
                list="category-options-modify"
                value={product.category}
                onChange={handleChangeProduct}
                className="w-full h-10 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
              />
              <datalist id="category-options-modify">
                {categoryList.map((cat) => (
                  <option key={cat} value={cat} />
                ))}
              </datalist>
            </div>

            <div>
              <label className="text-xs text-ink-soft mb-1.5 block">
                판매 상태
              </label>
              <select
                name="delFlag"
                value={product.delFlag}
                onChange={handleChangeProduct}
                className="w-full h-10 px-3 border border-line-soft rounded-lg text-sm focus:outline-none focus:border-brand"
              >
                <option value={false}>판매중</option>
                <option value={true}>숨김</option>
              </select>
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
                className="w-full p-3 border border-line-soft rounded-lg text-sm resize-none focus:outline-none focus:border-brand"
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
          <p className="text-sm font-medium mb-4">상품 이미지</p>

          {product.uploadFileNames.length > 0 && (
            <div className="grid grid-cols-3 md:grid-cols-4 gap-3 mb-4">
              {product.uploadFileNames.map((imgFile, i) => (
                <div key={i} className="relative">
                  <div className="aspect-square rounded-lg overflow-hidden bg-surface">
                    <img
                      alt=""
                      src={`${host}/api/product/view/s_${imgFile}`}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <button
                    onClick={() => deleteOldImages(imgFile)}
                    className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-ink text-white text-xs flex items-center justify-center"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}

          <p className="text-xs text-ink-soft mb-2">새 이미지 추가</p>
          <input
            ref={uploadRef}
            type="file"
            multiple
            accept="image/*"
            className="text-sm"
          />
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-[0_8px_24px_-12px_rgba(58,54,47,0.15)] mb-6">
          <p className="text-sm font-medium mb-4">옵션 관리</p>

          {options.length > 0 && (
            <div className="flex flex-col gap-2 mb-4">
              {options.map((opt) =>
                editingPono === opt.pono ? (
                  <div
                    key={opt.pono}
                    className="flex flex-wrap gap-2 items-center bg-cream rounded-lg p-3"
                  >
                    <input
                      value={editingOption.optionName}
                      onChange={(e) =>
                        setEditingOption({
                          ...editingOption,
                          optionName: e.target.value,
                        })
                      }
                      placeholder="옵션명"
                      className="h-9 px-2.5 border border-line-soft rounded-md text-xs w-24"
                    />
                    <input
                      value={editingOption.optionValue}
                      onChange={(e) =>
                        setEditingOption({
                          ...editingOption,
                          optionValue: e.target.value,
                        })
                      }
                      placeholder="옵션값"
                      className="h-9 px-2.5 border border-line-soft rounded-md text-xs flex-1 min-w-[100px]"
                    />
                    <input
                      type="text"
                      inputMode="numeric"
                      value={editingOption.extraPrice}
                      onChange={(e) =>
                        handleChangeEditingExtraPrice(e.target.value)
                      }
                      placeholder="추가금액"
                      className="h-9 px-2.5 border border-line-soft rounded-md text-xs w-24"
                    />
                    <button
                      onClick={() => handleSaveEditOption(opt)}
                      className="h-9 px-3 rounded-full bg-brand text-white text-xs"
                    >
                      저장
                    </button>
                    <button
                      onClick={() => setEditingPono(null)}
                      className="h-9 px-3 rounded-full border border-line-soft text-xs"
                    >
                      취소
                    </button>
                  </div>
                ) : (
                  <div
                    key={opt.pono}
                    className="flex flex-wrap gap-3 items-center justify-between border border-line rounded-lg p-3 text-sm"
                  >
                    <div className="flex gap-3">
                      <span className="text-ink-soft">{opt.optionName}</span>
                      <span>{opt.optionValue}</span>
                      {opt.extraPrice > 0 && (
                        <span className="text-brand-deep">
                          +{opt.extraPrice.toLocaleString()}원
                        </span>
                      )}
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => {
                          setEditingPono(opt.pono);
                          setEditingOption({
                            ...opt,
                            extraPrice: String(opt.extraPrice ?? ""),
                          });
                        }}
                        className="text-xs text-ink-faint underline"
                      >
                        수정
                      </button>
                      <button
                        onClick={() => handleDeleteOption(opt.pono)}
                        className="text-xs text-ink-faint underline"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                ),
              )}
            </div>
          )}

          <div className="flex flex-wrap gap-2 items-center bg-cream/60 border border-dashed border-line rounded-lg p-3">
            <input
              value={newOption.optionName}
              onChange={(e) =>
                setNewOption({ ...newOption, optionName: e.target.value })
              }
              placeholder="옵션명 (예: 포장)"
              className="h-9 px-2.5 border border-line-soft rounded-md text-xs w-28"
            />
            <input
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
              onChange={(e) => handleChangeNewExtraPrice(e.target.value)}
              placeholder="추가금액"
              className="h-9 px-2.5 border border-line-soft rounded-md text-xs w-24"
            />
            <button
              onClick={handleAddOption}
              className="h-9 px-4 rounded-full bg-lavender-dark text-white text-xs"
            >
              가격 추가
            </button>
          </div>
        </div>

        <div className="flex flex-wrap justify-end gap-2">
          <button
            onClick={() => moveToList({ page: 1 })}
            className="h-11 px-6 rounded-full border border-line-soft text-sm"
          >
            목록으로
          </button>
          <button
            onClick={handleClickDelete}
            className="h-11 px-6 rounded-full border border-red-300 text-red-600 text-sm"
          >
            상품 삭제
          </button>
          <button
            onClick={handleClickModify}
            className="h-11 px-6 rounded-full bg-brand text-white text-sm font-medium"
          >
            수정 완료
          </button>
        </div>
      </div>
    </div>
  );
};

export default ModifyComponent;
