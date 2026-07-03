import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";

const Loading = <div>Loading....</div>;
const DressItemList = lazy(() => import("../pages/dressItem/DressItemListPage"));

const DressItemAdd = lazy(() => import("../pages/dressItem/DressItemAddPage"));

const DressItemRead = lazy(() => import("../pages/dressItem/DressItemReadPage"));

const DressItemModify = lazy(() => import("../pages/dressItem/DressItemModifyPage"));

const dressItemRouter = () => {
  return [
    {
      path: "list",
      element: (
        <Suspense fallback={Loading}>
          <DressItemList />
        </Suspense>
      ),
    },
    {
      path: "",
      element: <Navigate replace to="/dress-items/list" />,
    },
    {
      path: "add",
      element: (
        <Suspense fallback={Loading}>
          <DressItemAdd />
        </Suspense>
      ),
    },
    {
      path: "read/:dressItemId",
      element: (
        <Suspense fallback={Loading}>
          <DressItemRead />
        </Suspense>
      ),
    },
    {
      path: "modify/:dressItemId",
      element: (
        <Suspense fallback={Loading}>
          <DressItemModify />
        </Suspense>
      ),
    },
  ];
};

export default dressItemRouter;
