import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import reservationRouter from "./reservationRouter";
import productRouter from "./productRouter";
import authRouter from "./authRouter";

const Loading = <div>Loading....</div>;
const Main = lazy(() => import("../pages/MainPage"));
const About = lazy(() => import("../pages/AboutPage"));
const ReservationIndex = lazy(() => import("../pages/reservation/IndexPage"));
const ReservationList = lazy(() => import("../pages/reservation/ListPage"));
const ProductRouter = lazy(() => import("../pages/product/IndexPage"));

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
]);

export default root;
