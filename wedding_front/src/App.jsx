import { RouterProvider } from "react-router-dom";
import "./App.css";
import root from "./router/root";
import { InquiryChatProvider } from "./context/InquiryChatContext";
import GlobalInquiryChatHost from "./components/chat/GlobalInquiryChatHost";

function App() {
  return (
    <InquiryChatProvider>
      <RouterProvider router={root} />
      <GlobalInquiryChatHost />
    </InquiryChatProvider>
  );
}

export default App;
