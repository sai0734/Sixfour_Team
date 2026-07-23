// 브라우저 기본 alert() 대체용 전역 알림 스토어.
// React 컴포넌트 밖(axios 인터셉터 등)에서도 호출할 수 있도록 pub/sub 방식으로 구현.
// 실제 렌더링은 GlobalAlertHost가 담당한다.
let listener = null;

export const setAlertListener = (fn) => {
  listener = fn;
};

// onClose: 사용자가 확인 버튼을 눌러 모달을 닫은 뒤 실행할 콜백 (예: 로그인 페이지로 이동)
export const showAlert = (message, onClose) => {
  if (listener) {
    listener(message, onClose);
  } else {
    // GlobalAlertHost가 아직 마운트되지 않은 극히 예외적인 상황을 위한 안전장치
    window.alert(message);
    onClose?.();
  }
};
