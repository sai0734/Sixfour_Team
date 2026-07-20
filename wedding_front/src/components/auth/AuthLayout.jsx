import { useLocation, useSearchParams } from "react-router-dom";

const AuthLayout = ({ eyebrow, title, subtitle, children }) => {
  const [searchParams] = useSearchParams();
  const location = useLocation();

  const handleImageError = (e) => {
    e.target.style.display = "none";
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-blush-50/50 p-4 sm:p-6 lg:p-12">
      <div className="w-full max-w-6xl bg-white rounded-3xl shadow-xl overflow-hidden flex flex-col md:flex-row md:min-h-[750px]">
        {/* 왼쪽: 사진 및 문구 영역 */}
        <div className="w-full md:w-1/2 relative overflow-hidden bg-stone-900 min-h-[400px] md:min-h-full m-0 md:m-4 md:rounded-2xl flex flex-col justify-between p-8 md:p-12 text-white">
          {/* 배경 이미지 */}
          <img
            src="/auth-bg.jpg"
            alt="Auth background"
            className="absolute inset-0 w-full h-full object-cover opacity-85 scale-105 pointer-events-none"
            onError={handleImageError}
          />

          {/* 어두운 오버레이 */}
          <div className="absolute inset-0 bg-stone-950/40" />
          <div className="absolute inset-0 bg-gradient-to-t from-stone-950/90 via-stone-950/30 to-transparent" />

          {/* 상단 뱃지 및 메인 타이틀 (z-index로 이미지 위로 배치) */}
          <div className="relative z-10">
            {eyebrow && (
              <span className="inline-block px-3.5 py-1.5 rounded-full bg-white/10 backdrop-blur-md text-xs font-semibold tracking-wide uppercase mb-4 md:mb-6 text-rose-200 border border-white/15">
                {eyebrow}
              </span>
            )}
            <h1 className="font-display text-3xl md:text-4xl font-bold leading-tight tracking-tight text-white drop-shadow-md">
              {title}
            </h1>
          </div>

          {/* 하단 서브타이틀 */}
          <div className="relative z-10 mt-12">
            {subtitle && (
              <p className="text-stone-100 text-base font-normal leading-relaxed max-w-sm drop-shadow">
                {subtitle}
              </p>
            )}
          </div>
        </div>

        {/* 오른쪽: 입력 폼 영역 */}
        <div className="flex-1 flex flex-col justify-center px-6 py-10 sm:px-12 md:px-16 bg-white overflow-y-auto">
          <div className="w-full max-w-sm mx-auto">{children}</div>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
