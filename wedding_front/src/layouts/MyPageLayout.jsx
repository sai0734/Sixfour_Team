import BasicMenu from "../components/menus/BasicMenu";
import MyPageSidebar from "./MyPageSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";
import ShopTapeLabel from "../components/product/ShopTapeLabel";

// PrepLayout과 동일한 껍데기지만 사이드바만 마이페이지 전용으로 분리.
const MyPageLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <ProfileCompleteGuard>
      <>
        <BasicMenu />

        <div className="min-h-screen bg-cream">
          {/* 답례품/AI 웨딩플랜 히어로와 같은 색감·폰트를 쓰되, 이 레이아웃은 BasicLayout의
              mt-24 여백 없이 BasicMenu(고정 상단바) 바로 아래에서 시작하므로 음수 마진 대신
              위쪽에 넉넉한 패딩을 직접 줘서 고정 상단바에 안 가리게 한다. */}
          {(eyebrow || title) && (
            <section
              className="relative bg-cover bg-center px-5 pb-12 pt-28 text-center md:px-8 md:pb-14 md:pt-36"
              style={{ backgroundImage: "url('/mypage-hero.jpg')" }}
            >
              <div className="absolute inset-0 bg-black/45" />

              <div className="relative z-10 mx-auto max-w-[720px]">
                {eyebrow && (
                  <ShopTapeLabel tone="white" className="mb-5">
                    {eyebrow}
                  </ShopTapeLabel>
                )}
                {title && (
                  <h1 className="mb-2.5 font-['Gowun_Batang'] text-2xl leading-snug text-white md:mb-3.5 md:text-4xl">
                    {title}
                  </h1>
                )}
                {subtitle && (
                  <p className="text-sm leading-relaxed text-white/85 md:text-[15px]">
                    {subtitle}
                  </p>
                )}
              </div>
            </section>
          )}

          <div className="mx-auto grid max-w-[1140px] grid-cols-1 items-start gap-6 px-5 py-6 md:px-8 lg:grid-cols-[240px_1fr] lg:gap-8 lg:px-6 lg:py-8">
            <MyPageSidebar />

            <main className="min-w-0">{children}</main>
          </div>
        </div>
      </>
    </ProfileCompleteGuard>
  );
};

export default MyPageLayout;
