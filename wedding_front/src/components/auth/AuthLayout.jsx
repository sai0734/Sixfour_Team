// 로그인/회원가입 등 인증 화면 전용 공통 레이아웃
// 왼쪽: 핑크 그라데이션 카피 패널, 오른쪽: 흰색 입력 패널
const AuthLayout = ({ eyebrow, title, subtitle, footer, children }) => {
  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-blush-50 p-4 font-body">
      <div className="w-full max-w-4xl bg-white rounded-3xl shadow-2xl shadow-rose-200/60 overflow-hidden flex flex-col md:flex-row">
        {/* 왼쪽 그라데이션 패널 */}
        <div className="relative md:w-2/5 bg-rose-gradient text-white p-10 flex flex-col justify-between overflow-hidden min-h-[220px]">
          <div className="absolute -top-12 -right-12 w-48 h-48 bg-white/15 rounded-full blur-2xl"></div>
          <div className="absolute bottom-0 -left-16 w-56 h-56 bg-white/10 rounded-full blur-3xl"></div>
          <div className="absolute top-1/2 right-1/4 w-24 h-24 bg-white/10 rounded-full blur-xl"></div>

          <div className="relative z-10">
            {eyebrow && (
              <span className="inline-block text-xs tracking-[0.2em] uppercase bg-white/20 px-3 py-1 rounded-full mb-6 font-body font-semibold">
                {eyebrow}
              </span>
            )}
            <h1 className="font-display text-3xl md:text-4xl leading-tight mb-4">
              {title}
            </h1>
            {subtitle && (
              <p className="text-white/85 leading-relaxed">{subtitle}</p>
            )}
          </div>

          {footer && <div className="relative z-10 mt-10">{footer}</div>}
        </div>

        {/* 오른쪽 폼 패널 */}
        <div className="flex-1 p-8 md:p-12 flex flex-col justify-center">
          {children}
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
