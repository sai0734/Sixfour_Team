import { Outlet } from "react-router-dom";
import GlobalInquiryChatHost from "../components/chat/GlobalInquiryChatHost";

const AppShell = () => {
  return (
    <>
      <Outlet />
      <GlobalInquiryChatHost />
    </>
  );
};

export default AppShell;
