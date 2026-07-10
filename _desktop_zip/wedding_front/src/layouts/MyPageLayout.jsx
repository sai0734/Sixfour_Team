import BasicMenu from "../components/menus/BasicMenu";
import MyPageSidebar from "./MyPageSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";
import TapeLabel from "../components/common/TapeLabel";
import MyPageHeaderArt from "../components/common/MyPageHeaderArt";

// PrepLayout과 동일한 껍데기지만 사이드바만 마이페이지 전용으로 분리.
const MyPageLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <ProfileCompleteGuard>
      <>
        <BasicMenu />

        <div className="bg-cream min-h-screen">
          {(eyebrow || title) && (
            <section className="text-center pt-28 pb-8 bg-brand-light">
              <MyPageHeaderArt className="w-28 h-20 mx-auto mb-1" />
              {eyebrow && (
                <TapeLabel tone="white" className="mb-4">
                  {eyebrow}
                </TapeLabel>
              )}
              {title && (
                <p className="font-serif text-3xl text-brand-deep mb-2">
                  {title}
                </p>
              )}
              {subtitle && (
                <p className="text-sm text-brand-accent">{subtitle}</p>
              )}
            </section>
          )}

          <div className="max-w-[1140px] mx-auto px-6 flex">
            <MyPageSidebar />

            <main className="flex-1 py-8 min-w-0">{children}</main>
          </div>
        </div>
      </>
    </ProfileCompleteGuard>
  );
};

export default MyPageLayout;
