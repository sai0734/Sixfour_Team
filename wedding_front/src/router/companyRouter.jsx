import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";
import AdminOnly from "../components/common/AdminOnly";

const Loading = <div>Loading....</div>;
const CompanyList = lazy(() => import("../pages/company/CompanyListPage"));
const CompanyAdd = lazy(() => import("../pages/company/CompanyAddPage"));
const CompanyModify = lazy(() => import("../pages/company/CompanyModifyPage"));
const CompanyRead = lazy(() => import("../pages/company/CompanyReadPage"));

const companyRouter = () => {
  return [
    {
      path: "list",
      element: (
        <Suspense fallback={Loading}>
          <CompanyList />
        </Suspense>
      ),
    },
    {
      path: "",
      element: <Navigate replace to="/companies/list" />,
    },
    {
      path: "add",
      element: (
        <AdminOnly>
          <Suspense fallback={Loading}>
            <CompanyAdd />
          </Suspense>
        </AdminOnly>
      ),
    },
    {
      path: "modify/:cmno",
      element: (
        <AdminOnly>
          <Suspense fallback={Loading}>
            <CompanyModify />
          </Suspense>
        </AdminOnly>
      ),
    },
    {
      path: "read/:cmno",
      element: (
        <Suspense fallback={Loading}>
          <CompanyRead />
        </Suspense>
      ),
    },
  ];
};

export default companyRouter;
