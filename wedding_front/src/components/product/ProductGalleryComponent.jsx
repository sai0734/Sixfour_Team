import { useEffect, useState } from "react";

// 상품 상세페이지의 메인 이미지 + 썸네일 스트립
const ProductGalleryComponent = ({ pname, uploadFileNames, host }) => {
  const [selectedImageIdx, setSelectedImageIdx] = useState(0);

  useEffect(() => {
    setSelectedImageIdx(0);
  }, [uploadFileNames]);

  return (
    <div>
      <div className="aspect-square rounded-2xl overflow-hidden bg-surface">
        <img
          alt={pname}
          className="w-full h-full object-cover"
          src={`${host}/api/product/view/${uploadFileNames?.[selectedImageIdx]}`}
        />
      </div>
      {uploadFileNames?.length > 1 && (
        <div className="flex gap-2.5 mt-3">
          {uploadFileNames.map((file, i) => (
            <div
              key={i}
              onClick={() => setSelectedImageIdx(i)}
              className={`w-16 h-16 rounded-lg overflow-hidden bg-surface cursor-pointer ${
                selectedImageIdx === i ? "outline outline-2 outline-brand" : ""
              }`}
            >
              <img
                alt=""
                className="w-full h-full object-cover"
                src={`${host}/api/product/view/s_${file}`}
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ProductGalleryComponent;
