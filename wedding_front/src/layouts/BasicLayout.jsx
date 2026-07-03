import BasicMenu from "../components/menus/BasicMenu";
import CartComponent from "../components/menus/CartComponent";

const BasicLayout = ({ children, showCart = true }) => {
  return (
    <>
      <BasicMenu />

      <div className="my-5 flex w-full flex-col bg-white md:flex-row md:space-x-1">
        <main className={`${showCart ? "md:w-4/5 lg:w-3/4" : "w-full"} bg-white px-5 py-5`}>
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
