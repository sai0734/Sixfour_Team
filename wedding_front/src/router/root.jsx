<<<<<<< HEAD
﻿import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
=======
import { Suspense, lazy } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
>>>>>>> 70c73be166854e72c3573de7861da3374a964347
import reservationRouter from "./reservationRouter";
import dressItemRouter from "./dressItemRouter";
import authRouter from "./authRouter";
import companyRouter from "./companyRouter";
import AdminOnly from "../components/common/AdminOnly";

const Loading = <div>Loading....</div>;
const Main = lazy(() => import("../pages/MainPage"));
const About = lazy(() => import("../pages/AboutPage"));
<<<<<<< HEAD
const ReservationIndex = lazy(() => import("../pages/reservation/ReservationIndexPage"));
const DressItemRouter = lazy(() => import("../pages/dressItem/DressItemIndexPage"));
const AdminDashboard = lazy(() => import("../pages/admin/AdminDashboardPage"));
=======
const ReservationIndex = lazy(() => import("../pages/reservation/IndexPage"));
const ReservationList = lazy(() => import("../pages/reservation/ListPage"));
const ProductRouter = lazy(() => import("../pages/product/IndexPage"));
const ChecklistListPage = lazy(() => import("../pages/checklist/ListPage"));
const BudgetListPage = lazy(() => import("../pages/budget/ListPage"));
const MyPageHub = lazy(() => import("../pages/mypage/HubPage"));
const BoardHubPage = lazy(() => import("../pages/board/HubPage"));
const FreeBoardPage = lazy(() => import("../pages/board/FreeBoardPage"));
const ReviewBoardPage = lazy(() => import("../pages/board/ReviewBoardPage"));
>>>>>>> 70c73be166854e72c3573de7861da3374a964347

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
<<<<<<< HEAD
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
=======
    path: "/checklist",
    element: <Navigate replace to="/checklist/list" />,
  },
  {
    path: "/checklist/list",
    element: (
      <Suspense fallback={Loading}>
        <ChecklistListPage />
      </Suspense>
    ),
  },
  {
    path: "/budget",
    element: <Navigate replace to="/budget/list" />,
  },
  {
    path: "/budget/list",
    element: (
      <Suspense fallback={Loading}>
        <BudgetListPage />
      </Suspense>
    ),
  },
  {
    path: "/mypage",
    element: (
      <Suspense fallback={Loading}>
        <MyPageHub />
      </Suspense>
    ),
  },
  {
    path: "/companywish",
    element: <Navigate replace to="/mypage" />,
  },
  {
    path: "/weddingplan",
    element: <Navigate replace to="/mypage" />,
  },
  {
    path: "/board",
    element: <Navigate replace to="/board/list" />,
  },
  {
    path: "/board/list",
    element: (
      <Suspense fallback={Loading}>
        <BoardHubPage />
      </Suspense>
    ),
  },
  {
    path: "/board/free",
    element: (
      <Suspense fallback={Loading}>
        <FreeBoardPage />
      </Suspense>
    ),
  },
  {
    path: "/board/review",
    element: (
      <Suspense fallback={Loading}>
        <ReviewBoardPage />
      </Suspense>
>>>>>>> 70c73be166854e72c3573de7861da3374a964347
    ),
  },
]);

export default root;
