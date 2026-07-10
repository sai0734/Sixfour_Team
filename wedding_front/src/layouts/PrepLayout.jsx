import BasicMenu from "../components/menus/BasicMenu";
import PrepSidebar from "./PrepSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";
import TapeLabel from "../components/common/TapeLabel";

const PrepLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <ProfileCompleteGuard>
      <>
        <BasicMenu />

        <div className="min-h-screen bg-[#FBF7F0]">
          {(eyebrow || title) && (
            <section
              className="relative bg-cover bg-center px-5 pt-24 pb-8 text-center md:px-8 md:pt-28 md:pb-10 lg:px-[60px]"
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
            <PrepSidebar />

            <main className="min-w-0">{children}</main>
          </div>
        </div>
      </>
    </ProfileCompleteGuard>
  );
};

export default PrepLayout;
