import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";

const Loading = <div>Loading....</div>;
const ReservationList = lazy(() => import("../pages/reservation/ReservationListPage"));
const ReservationRead = lazy(() => import("../pages/reservation/ReservationReadPage"));
const ReservationAdd = lazy(() => import("../pages/reservation/ReservationAddPage"));
const ReservationModify = lazy(() => import("../pages/reservation/ReservationModifyPage"));

const reservationRouter = () => {
  return [
    {
      path: "list",
      element: (
        <Suspense fallback={Loading}>
          <ReservationList />
        </Suspense>
      ),
    },
    {
      path: "",
      element: <Navigate replace to="list" />,
    },
    {
      path: "read/:reservationId",
      element: (
        <Suspense fallback={Loading}>
          <ReservationRead />
        </Suspense>
      ),
    },
    {
      path: "add",
      element: (
        <Suspense fallback={Loading}>
          <ReservationAdd />
        </Suspense>
      ),
    },
    {
      path: "modify/:reservationId",
      element: (
        <Suspense fallback={Loading}>
          <ReservationModify />
        </Suspense>
      ),
    },
  ];
};

export default reservationRouter;
