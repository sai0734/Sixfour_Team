import BasicMenu from "../components/menus/BasicMenu";
import MyPageSidebar from "./MyPageSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";
import TapeLabel from "../components/common/TapeLabel";

// PrepLayout과 동일한 껍데기지만 사이드바만 마이페이지 전용으로 분리.
const MyPageLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <ProfileCompleteGuard>
      <>
        <BasicMenu />

        <div className="min-h-screen bg-[#FBF7F0]">
          {(eyebrow || title) && (
            <section
              className="relative bg-cover bg-center px-5 pt-24 pb-8 text-center md:px-8 md:pt-28 md:pb-10 lg:px-[60px]"
              // TODO: 마이페이지 전용 사진 준비되면 /mypage-hero.jpg 같은 걸로 교체
              // (지금은 준비관리랑 같은 사진으로 임시로 넣어둔 상태)
              style={{ backgroundImage: "url('/prep-hero.jpg')" }}
            >
              <div className="absolute inset-0 bg-black/45" />

              <div className="relative z-10">
                {eyebrow && (
                  <TapeLabel tone="white" className="mb-4">
                    {eyebrow}
                  </TapeLabel>
                )}
                {title && (
                  <p className="mb-2 font-['Gowun_Batang'] text-2xl text-white md:text-3xl">
                    {title}
                  </p>
                )}
                {subtitle && (
                  <p className="text-sm text-white/85">{subtitle}</p>
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
