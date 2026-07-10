import BasicMenu from "../components/menus/BasicMenu";
import PrepSidebar from "./PrepSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";
import TapeLabel from "../components/common/TapeLabel";
import PrepHeaderArt from "../components/common/PrepHeaderArt";

const PrepLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <ProfileCompleteGuard>
      <>
        <BasicMenu />

        <div className="bg-cream min-h-screen">
          {(eyebrow || title) && (
            <section
              className="relative text-center pt-28 pb-10 bg-cover bg-center"
              style={{ backgroundImage: "url('/prep-hero.jpg')" }}
            >
              {/* 사진 위에 어두운 막을 씌워서 흰 글씨가 잘 읽히게 */}
              <div className="absolute inset-0 bg-black/45" />

              <div className="relative z-10">
                {eyebrow && (
                  <TapeLabel tone="white" className="mb-4">
                    {eyebrow}
                  </TapeLabel>
                )}
                {title && (
                  <p className="font-serif text-3xl text-white mb-2">{title}</p>
                )}
                {subtitle && (
                  <p className="text-sm text-white/85">{subtitle}</p>
                )}
              </div>
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
