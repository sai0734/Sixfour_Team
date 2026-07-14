import { RouterProvider } from "react-router-dom";
import "./App.css";
import root from "./router/root";
import { InquiryChatProvider } from "./context/InquiryChatContext";

function App() {
  return (
    <InquiryChatProvider>
      <RouterProvider router={root} />
    </InquiryChatProvider>
  );
}

export default App;
