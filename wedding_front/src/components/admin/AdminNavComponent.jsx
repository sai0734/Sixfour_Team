import { Link, useLocation } from "react-router-dom";

const AdminNavComponent = () => {
  const location = useLocation();

  const tabs = [
    { path: "/admin/products", label: "상품 관리" },
    { path: "/admin/orders", label: "주문 관리" },
  ];

  return (
    <div className="border-b border-line mb-6">
      <div className="flex items-center justify-between max-w-[1200px] mx-auto px-6">
        <div className="flex gap-6">
          {tabs.map((tab) => (
            <Link
              key={tab.path}
              to={tab.path}
              className={`py-4 text-sm border-b-2 ${
                location.pathname.startsWith(tab.path)
                  ? "text-ink font-medium border-brand"
                  : "text-ink-faint border-transparent"
              }`}
            >
              {tab.label}
            </Link>
          ))}
        </div>
        <Link to="/admin" className="text-xs text-ink-faint underline">
          관리자 홈
        </Link>
      </div>
    </div>
  );
};

export default AdminNavComponent;
