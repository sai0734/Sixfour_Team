import BasicMenu from "../components/menus/BasicMenu";
import MyPageSidebar from "./MyPageSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";

// PrepLayout과 동일한 껍데기지만 사이드바만 마이페이지 전용으로 분리.
const MyPageLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <>
      <ProfileCompleteGuard />
      <BasicMenu />

      <div className="bg-cream min-h-screen">
        {(eyebrow || title) && (
          <section className="text-center pt-12 pb-6 bg-brand-light">
            {eyebrow && (
              <p className="text-xs tracking-[0.15em] text-brand-accent mb-2.5">
                {eyebrow}
              </p>
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
  );
};

export default MyPageLayout;
