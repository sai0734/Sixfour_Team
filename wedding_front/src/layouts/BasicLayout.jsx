import BasicMenu from "../components/menus/BasicMenu";
import CartComponent from "../components/menus/CartComponent";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";

const BasicLayout = ({ children, showCart = true }) => {
  return (
    <>
      <ProfileCompleteGuard />
      <BasicMenu />

      <div className="mt-24 mb-5 flex w-full flex-col bg-white md:flex-row md:space-x-1">
        {/* 페이지 탑바 수정 */}
        <main
          className={`${showCart ? "md:w-4/5 lg:w-3/4" : "w-full"} bg-white px-5 py-5`}
        >
          {children}
        </main>

        {showCart ? (
          <aside className="flex bg-gray-100 px-5 py-5 md:w-1/3 lg:w-1/4">
            <CartComponent />
          </aside>
        ) : (
          <></>
        )}
      </div>
    </>
  );
};

export default BasicLayout;
