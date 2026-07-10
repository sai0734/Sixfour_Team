import BasicMenu from "../components/menus/BasicMenu";
import AdminSidebar from "./AdminSidebar";
import ProfileCompleteGuard from "../components/common/ProfileCompleteGuard";

const AdminLayout = ({ children }) => {
  return (
    <ProfileCompleteGuard>
      <>
        <BasicMenu />

        <div className="bg-white min-h-screen">
          <div className="max-w-[1200px] mx-auto px-6 pt-20 flex">
            <AdminSidebar />

            <main className="flex-1 py-8 min-w-0">{children}</main>
          </div>
        </div>
      </>
    </ProfileCompleteGuard>
  );
};

export default AdminLayout;
