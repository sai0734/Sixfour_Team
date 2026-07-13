import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";
import AdminOnly from "../components/common/AdminOnly";

const Loading = <div>Loading....</div>;
const CompanyList = lazy(() => import("../pages/company/CompanyListPage"));
const CompanyAdd = lazy(() => import("../pages/company/CompanyAddPage"));
const CompanyModify = lazy(() => import("../pages/company/CompanyModifyPage"));
const CompanyRead = lazy(() => import("../pages/company/CompanyReadPage"));
// 재원 추가 - 업체 상세페이지 "예약" 버튼 → 날짜/옵션 선택 → 결제
const ReservationReserve = lazy(
  () => import("../pages/reservation/ReservationReservePage"),
);
const ReservationPaymentSuccess = lazy(
  () => import("../pages/reservation/ReservationPaymentSuccessPage"),
);
const ReservationPaymentFail = lazy(
  () => import("../pages/reservation/ReservationPaymentFailPage"),
);

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
    // ↓↓↓ 재원 추가
    {
      path: "reserve/:cmno",
      element: (
        <Suspense fallback={Loading}>
          <ReservationReserve />
        </Suspense>
      ),
    },
    {
      path: "reserve/:cmno/success",
      element: (
        <Suspense fallback={Loading}>
          <ReservationPaymentSuccess />
        </Suspense>
      ),
    },
    {
      path: "reserve/:cmno/fail",
      element: (
        <Suspense fallback={Loading}>
          <ReservationPaymentFail />
        </Suspense>
      ),
    },
    // ↑↑↑ 재원 추가
  ];
};

export default companyRouter;
