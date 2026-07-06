// 상품 상세페이지의 "상세정보" 탭 내용 (설명 + 전체 이미지 나열)
const ProductDetailInfoComponent = ({ pdesc, uploadFileNames, host }) => {
  return (
    <div>
      <div className="bg-cream rounded-2xl p-6 text-sm text-ink-soft whitespace-pre-line mb-6">
        {pdesc}
      </div>

      {uploadFileNames?.length > 0 && (
        <div className="flex flex-col gap-3">
          {uploadFileNames.map((file, i) => (
            <img
              key={i}
              alt=""
              className="w-full rounded-2xl"
              src={`${host}/api/product/view/${file}`}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default ProductDetailInfoComponent;
