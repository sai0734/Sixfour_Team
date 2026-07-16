import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";

const Loading = <div>Loading....</div>;

// 3단계는 "빠르게 모드"만. 4단계에서 자세히 모드 추가되면 여기 path만 늘리면 됨.
const QuickPlan = lazy(() => import("../pages/aiplan/QuickPlanPage"));
const DetailPlan = lazy(() => import("../pages/aiplan/DetailPlanPage"));

const aiPlanRouter = () => {
  return [
    {
      path: "",
      element: <Navigate replace to="/aiplan/quick" />,
    },
    {
      path: "quick",
      element: (
        <Suspense fallback={Loading}>
          <QuickPlan />
        </Suspense>
      ),
    },
    {
      path: "detail",
      element: (
        <Suspense fallback={Loading}>
          <DetailPlan />
        </Suspense>
      ),
    },
  ];
};

export default aiPlanRouter;
