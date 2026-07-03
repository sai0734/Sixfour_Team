import { Suspense, lazy } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import productRouter from "./productRouter";
import authRouter from "./authRouter";
import dressItemRouter from "./dressItemRouter";
import companyRouter from "./companyRouter";
import AdminOnly from "../components/common/AdminOnly";

const Loading = <div>Loading....</div>;
const Main = lazy(() => import("../pages/MainPage"));
const About = lazy(() => import("../pages/AboutPage"));
const ProductRouter = lazy(() => import("../pages/product/IndexPage"));
const DressItemRouter = lazy(
  () => import("../pages/dressItem/DressItemIndexPage"),
);
const AdminDashboard = lazy(() => import("../pages/admin/AdminDashboardPage"));
const ChecklistListPage = lazy(() => import("../pages/checklist/ListPage"));
const PrepHubPage = lazy(() => import("../pages/prep/HubPage"));
const BudgetListPage = lazy(() => import("../pages/budget/ListPage"));
const MyPageHub = lazy(() => import("../pages/mypage/HubPage"));
const BoardHubPage = lazy(() => import("../pages/board/HubPage"));
const FreeBoardPage = lazy(() => import("../pages/board/FreeBoardPage"));
const ReviewBoardPage = lazy(() => import("../pages/board/ReviewBoardPage"));
const SeniorMatchPage = lazy(() => import("../pages/board/SeniorMatchPage"));

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
    // 예약 관리 기능은 마이페이지 허브 "예약 현황" 탭으로 통합됨
    path: "/reservation",
    element: <Navigate replace to="/mypage" />,
  },
  {
    path: "product",
    element: (
      <Suspense fallback={Loading}>
        <ProductRouter />
      </Suspense>
    ),
    children: productRouter(),
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
  {
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
    ),
  },
  {
    path: "/board/senior",
    element: (
      <Suspense fallback={Loading}>
        <SeniorMatchPage />
      </Suspense>
    ),
  },
  {
    path: "/prep/hub",
    element: (
      <Suspense fallback={Loading}>
        <PrepHubPage />
      </Suspense>
    ),
  },
]);

export default root;
