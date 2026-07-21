import BasicMenu from "../components/menus/BasicMenu";
import TapeLabel from "../components/common/TapeLabel";

// 커뮤니티 4개 페이지(허브/자유게시판/후기게시판/FAQ)에 똑같이 복붙돼 있던 히어로를 한 곳으로 모음.
// MyPageLayout/PrepLayout과 동일한 이유로, BasicLayout의 mt-24 여백이 없는 채로 BasicMenu(고정
// 상단바) 바로 아래에서 시작하므로 음수 마진이 아니라 넉넉한 패딩으로 상단바를 피한다.
const BoardLayout = ({ eyebrow, title, subtitle, children }) => {
  return (
    <>
      <BasicMenu />

      <div className="min-h-screen bg-cream">
        {(eyebrow || title) && (
          <section
            className="relative bg-cover bg-center px-5 pb-12 pt-28 text-center md:px-8 md:pb-14 md:pt-36"
            style={{ backgroundImage: "url('/community-hero.jpg')" }}
          >
            <div className="absolute inset-0 bg-black/45" />

            <div className="relative z-10 mx-auto max-w-[720px]">
              {eyebrow && (
                <TapeLabel tone="white" className="mb-5">
                  {eyebrow}
                </TapeLabel>
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

        {children}
      </div>
    </>
  );
};

export default BoardLayout;
