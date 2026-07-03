import { useSelector } from "react-redux";
import { Link } from "react-router-dom";

const BasicMenu = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const isAdmin = loginState.roleNames?.some(
    (roleName) => roleName === "ADMIN" || roleName === "ROLE_ADMIN",
  );
  return (
    <nav id="navbar" className=" flex  bg-blue-300">
      <div className="w-4/5 bg-gray-500">
        <ul className="flex p-4 text-white font-bold">
          <li className="pr-6 text-2xl">
            <Link to={"/"}>메인</Link>
          </li>
          <li className="pr-6 text-2xl">
            <Link to={"/about"}>소개</Link>
          </li>
          {loginState.email ? (
            <>
              <li className="pr-6 text-2xl">
                <Link to={"/reservation/"}>예약</Link>
              </li>
              <li className="pr-6 text-2xl">
                <Link to={"/dress-items/"}>드레스 상품</Link>
              </li>
              <li className="pr-6 text-2xl">
                <Link to={"/companies/"}>업체</Link>
              </li>
              {isAdmin ? (
                <li className="pr-6 text-2xl">
                  <Link to={"/admin"}>관리자</Link>
                </li>
              ) : (
                <></>
              )}
            </>
          ) : (
            <></>
          )}
        </ul>
      </div>

      <div className="w-1/5 flex justify-end bg-orange-300 p-4 font-medium">
        {!loginState.email ? (
          <div className="text-white text-sm m-1 rounded">
            <Link to={"/auth/login"}>로그인</Link>
          </div>
        ) : (
          <div className="text-white text-sm m-1 rounded">
            <Link to={"/auth/logout"}>로그아웃</Link>
          </div>
        )}
      </div>
    </nav>
  );
};
export default BasicMenu;
