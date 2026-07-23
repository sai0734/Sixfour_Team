import { useEffect, useState } from "react";
import AlertModal from "../auth/AlertModal";
import { setAlertListener } from "../../util/globalAlert";

// 사이트 전역에서 showAlert()로 띄우는 알림을, 로그인 페이지와 동일한 AlertModal 디자인으로 렌더링한다.
const GlobalAlertHost = () => {
  const [message, setMessage] = useState("");
  const [pendingClose, setPendingClose] = useState(null);

  useEffect(() => {
    setAlertListener((msg, onClose) => {
      setMessage(msg);
      setPendingClose(() => onClose || null);
    });
    return () => setAlertListener(null);
  }, []);

  const handleClose = () => {
    setMessage("");
    pendingClose?.();
    setPendingClose(null);
  };

  return <AlertModal message={message} onClose={handleClose} />;
};

export default GlobalAlertHost;
