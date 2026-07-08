import { useEffect, useMemo, useState } from "react";

const ProductGalleryComponent = ({ pname, uploadFileNames, host }) => {
  const [selectedImageIdx, setSelectedImageIdx] = useState(0);

  const validFileNames = useMemo(
    () => (uploadFileNames ?? []).filter((file) => file),
    [uploadFileNames],
  );

  useEffect(() => {
    setSelectedImageIdx(0);
  }, [uploadFileNames]);

  const selectedFile = validFileNames[selectedImageIdx];

  return (
    <div>
      <div className="aspect-square rounded-2xl overflow-hidden bg-surface">
        {selectedFile ? (
          <img
            alt={pname}
            className="w-full h-full object-cover"
            src={`${host}/api/product/view/${selectedFile}`}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-sm text-ink-faint">
            이미지 없음
          </div>
        )}
      </div>

      {validFileNames.length > 1 && (
        <div className="flex gap-2.5 mt-3">
          {validFileNames.map((file, i) => (
            <div
              key={file}
              onMouseEnter={() => setSelectedImageIdx(i)}
              onClick={() => setSelectedImageIdx(i)}
              className={`w-16 h-16 rounded-lg overflow-hidden bg-surface cursor-pointer ${
                selectedImageIdx === i ? "outline outline-2 outline-brand" : ""
              }`}
            >
              <img
                alt=""
                className="w-full h-full object-cover pointer-events-none"
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
