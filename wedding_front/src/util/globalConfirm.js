// 브라우저 기본 confirm() 대체용 전역 확인 스토어.
// alert()용 globalAlert.js와 같은 pub/sub 구조. showConfirm은 Promise<boolean>을 반환하므로
// 호출부는 반드시 await(또는 .then)로 결과를 받아야 한다 (await 빠뜨리면 Promise 객체 자체가
// 항상 truthy라 취소를 눌러도 확인한 것처럼 동작하니 주의).
let listener = null;

export const setConfirmListener = (fn) => {
  listener = fn;
};

export const showConfirm = (message) => {
  return new Promise((resolve) => {
    if (listener) {
      listener(message, resolve);
    } else {
      // GlobalConfirmHost가 아직 마운트되지 않은 극히 예외적인 상황을 위한 안전장치
      resolve(window.confirm(message));
    }
  });
};
