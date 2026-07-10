import BasicMenu from "../components/menus/BasicMenu";
import PrepSidebar from "./PrepSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";

const PrepLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <ProfileCompleteGuard>
      <>
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
            <PrepSidebar />

            <main className="flex-1 py-8 min-w-0">{children}</main>
          </div>
        </div>
      </>
    </ProfileCompleteGuard>
  );
};

export default PrepLayout;
