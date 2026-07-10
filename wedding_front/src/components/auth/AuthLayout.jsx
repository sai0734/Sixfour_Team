// 로그인/회원가입 등 인증 화면 전용 공통 레이아웃
// 메인페이지와 같은 무드(크림+블러쉬 배경, 워시테이프, 폴라로이드, Gowun Batang/Gaegu)로 통일
const AuthLayout = ({
  eyebrow,
  title,
  subtitle,
  footer,
  stickerEmoji = "🤍",
  children,
}) => {
  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-blush-lavender p-4 font-body">
      <div className="w-full max-w-4xl bg-cream rounded-[28px] shadow-[0_30px_70px_-15px_rgba(150,120,180,0.35)] overflow-hidden flex flex-col md:flex-row">
        {/* 왼쪽 무드보드 패널 */}
        <div className="relative md:w-2/5 bg-gradient-to-br from-blush-100 via-blush-50 to-lavender-light p-10 flex flex-col justify-between overflow-hidden min-h-[260px]">
          {/* 은은한 원형 블러 장식 */}
          <div className="absolute -top-10 -right-10 w-40 h-40 bg-white/40 rounded-full blur-2xl"></div>
          <div className="absolute bottom-10 -left-10 w-44 h-44 bg-white/30 rounded-full blur-3xl"></div>

          <div className="relative z-10">
            {eyebrow && (
              <span className="inline-block font-handwrite text-sm bg-white/85 text-brand-deep px-4 py-1 -rotate-2 rounded-sm mb-6 shadow-sm">
                {eyebrow}
              </span>
            )}
            <h1 className="font-serifkr text-[30px] md:text-[34px] leading-snug text-ink mb-4">
              {title}
            </h1>
            {subtitle && (
              <p className="text-ink-soft leading-relaxed text-[14.5px] font-body">
                {subtitle}
              </p>
            )}
          </div>

          {/* 미니 폴라로이드 스티커 - 메인페이지 시그니처 모티프 재사용 */}
          <div className="relative z-10 mt-8">
            <div className="inline-block bg-white rounded-[2px] p-2.5 pb-6 shadow-[0_10px_24px_-6px_rgba(80,45,10,0.25)] -rotate-3">
              <div className="w-28 h-20 rounded-[1px] bg-gradient-to-br from-blush-200 to-lavender flex items-center justify-center text-2xl">
                {stickerEmoji}
              </div>
            </div>
          </div>

          {footer && <div className="relative z-10 mt-6">{footer}</div>}
        </div>

        {/* 오른쪽 폼 패널 */}
        <div className="flex-1 p-8 md:p-12 flex flex-col justify-center bg-cream">
          {children}
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
