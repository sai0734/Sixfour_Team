import { useEffect, useState } from "react";
import ConfirmModal from "./ConfirmModal";
import { setConfirmListener } from "../../util/globalConfirm";

// 사이트 전역에서 showConfirm()으로 띄우는 확인창을, AlertModal과 같은 카드 디자인(취소/확인 버튼)으로 렌더링한다.
const GlobalConfirmHost = () => {
  const [message, setMessage] = useState("");
  const [resolver, setResolver] = useState(null);

  useEffect(() => {
    setConfirmListener((msg, resolve) => {
      setMessage(msg);
      setResolver(() => resolve);
    });
    return () => setConfirmListener(null);
  }, []);

  const handle = (result) => {
    resolver?.(result);
    setMessage("");
    setResolver(null);
  };

  return (
    <ConfirmModal
      message={message}
      onCancel={() => handle(false)}
      onConfirm={() => handle(true)}
    />
  );
};

export default GlobalConfirmHost;
