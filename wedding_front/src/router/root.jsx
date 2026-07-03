import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import reservationRouter from "./reservationRouter";
import dressItemRouter from "./dressItemRouter";
import authRouter from "./authRouter";
import companyRouter from "./companyRouter";
import AdminOnly from "../components/common/AdminOnly";

const Loading = <div>Loading....</div>;
const Main = lazy(() => import("../pages/MainPage"));
const About = lazy(() => import("../pages/AboutPage"));
const ReservationIndex = lazy(() => import("../pages/reservation/ReservationIndexPage"));
const DressItemRouter = lazy(() => import("../pages/dressItem/DressItemIndexPage"));
const AdminDashboard = lazy(() => import("../pages/admin/AdminDashboardPage"));

const root = createBrowserRouter([
  {
    path: "/",
    element: (
      <Suspense fallback={Loading}>
        <Main />
      </Suspense>
    ),
  },
  {
    path: "/about",
    element: (
      <Suspense fallback={Loading}>
        <About />
      </Suspense>
    ),
  },
  {
    path: "/reservation",
    element: (
      <Suspense fallback={Loading}>
        <ReservationIndex />
      </Suspense>
    ),
    children: reservationRouter(),
  },
  {
    path: "dress-items",
    element: (
      <Suspense fallback={Loading}>
        <DressItemRouter />
      </Suspense>
    ),
    children: dressItemRouter(),
  },
  {
    path: "auth",
    children: authRouter(),
  },
  {
    path: "companies",
    children: companyRouter(),
  },
  {
    path: "admin",
    element: (
      <AdminOnly>
        <Suspense fallback={Loading}>
          <AdminDashboard />
        </Suspense>
      </AdminOnly>
    ),
  },
]);

export default root;
