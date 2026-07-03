import { Suspense, lazy } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import productRouter from "./productRouter";
import authRouter from "./authRouter";

const Loading = <div>Loading....</div>;
const Main = lazy(() => import("../pages/MainPage"));
const About = lazy(() => import("../pages/AboutPage"));
const ProductRouter = lazy(() => import("../pages/product/IndexPage"));
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
    path: "auth",
    children: authRouter(),
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
