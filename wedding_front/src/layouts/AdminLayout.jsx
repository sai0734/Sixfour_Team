import BasicMenu from "../components/menus/BasicMenu";
import AdminSidebar from "./AdminSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";
import { useSelector } from "react-redux";

const AdminLayout = ({ children }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const isAdmin = loginState.roleNames?.some((r) =>
    ["ADMIN", "ROLE_ADMIN"].includes(r)
  );

  return (
    <ProfileCompleteGuard>
      <>
        <BasicMenu />
        <div className="bg-white min-h-screen">
          <div className="max-w-[1200px] mx-auto px-3 sm:px-6 pt-16 sm:pt-20 flex">
            {isAdmin && <AdminSidebar />}
            <main className="flex-1 py-4 sm:py-8 min-w-0 overflow-hidden">{children}</main>
          </div>
        </div>
      </>
    </ProfileCompleteGuard>
  );
};

export default AdminLayout;
