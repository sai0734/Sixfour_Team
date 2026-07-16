import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { getCookie } from "./cookieUtil";
import { API_SERVER_HOST } from "../api/reservationApi";

// destination(주소)별로 등록된 콜백들을 관리 - 여러 컴포넌트가 같은 destination을 구독해도
// 실제 STOMP 구독은 1번만 하고, 온 메시지를 콜백들에게 나눠서 전달한다
const listenersByDestination = new Map(); // destination -> Set<callback>
const liveSubscriptions = new Map(); // destination -> StompSubscription

let client = null;

// 연결 직전에 매번 최신 accessToken을 쿠키에서 다시 읽어서 STOMP 헤더에 실음
// (재연결 시에도 이 함수가 다시 호출되므로, 그 사이 jwtAxios가 토큰을 갱신했다면 최신 값을 씀)
const buildConnectHeaders = () => {
  const authInfo = getCookie("auth");
  if (!authInfo?.accessToken) return {};
  return { Authorization: `Bearer ${authInfo.accessToken}` };
};

// 이미 연결됐다면 그 destination을, 아직이면 onConnect 때 한꺼번에 구독하도록 등록만 해둠
const resubscribe = (destination) => {
  if (liveSubscriptions.has(destination)) return; // 이미 구독중
  if (!client?.connected) {
    console.log("[InquiryWS] 아직 연결 전이라 구독 보류:", destination);
    return; // 아직 연결 전이면 onConnect에서 처리됨
  }

  const subscription = client.subscribe(destination, (message) => {
    const callbacks = listenersByDestination.get(destination);
    if (!callbacks) return;
    const payload = JSON.parse(message.body);
    console.log("[InquiryWS] 메시지 수신:", destination, payload);
    callbacks.forEach((cb) => cb(payload));
  });

  console.log("[InquiryWS] 구독 성공:", destination);
  liveSubscriptions.set(destination, subscription);
};

const ensureClient = () => {
  if (client) return client;

  client = new Client({
    webSocketFactory: () => new SockJS(`${API_SERVER_HOST}/ws-inquiry`),
    reconnectDelay: 5000, // 끊기면 5초마다 재연결 시도
    beforeConnect: () => {
      client.connectHeaders = buildConnectHeaders();
      console.log("[InquiryWS] 연결 시도, 헤더:", client.connectHeaders);
    },
    onConnect: () => {
      console.log("[InquiryWS] 연결 성공! 등록된 구독 재개:", [...listenersByDestination.keys()]);
      // 최초 연결이든 재연결이든, 연결될 때마다 등록된 구독을 전부 다시 걸어준다
      liveSubscriptions.clear();
      listenersByDestination.forEach((_callbacks, destination) => {
        resubscribe(destination);
      });
    },
    onDisconnect: () => {
      console.log("[InquiryWS] 연결 끊김");
    },
    onWebSocketError: (event) => {
      console.error("[InquiryWS] WebSocket 자체 에러:", event);
    },
    onStompError: (frame) => {
      console.error("[InquiryWS] STOMP 에러:", frame.headers?.message, frame.body);
    },
  });

  return client;
};

// 로그인 후 한 번 호출 - 이미 연결돼 있으면 아무것도 안 함
export const connectInquiryWs = () => {
  const c = ensureClient();
  if (!c.active) {
    c.activate();
  }
};

// 로그아웃 시 호출 - 연결 종료 + 구독 상태 초기화
export const disconnectInquiryWs = () => {
  if (client?.active) {
    client.deactivate();
  }
  listenersByDestination.clear();
  liveSubscriptions.clear();
};

// destination을 구독하고, 그 주소로 메시지가 올 때마다 onMessage(payload)를 호출한다.
// 반환값(unsubscribe 함수)을 컴포넌트가 useEffect의 cleanup에서 꼭 호출해야 함
export const subscribeInquiryTopic = (destination, onMessage) => {
  if (!listenersByDestination.has(destination)) {
    listenersByDestination.set(destination, new Set());
  }
  listenersByDestination.get(destination).add(onMessage);

  resubscribe(destination); // 이미 연결돼있으면 바로 구독 시도

  return () => {
    const callbacks = listenersByDestination.get(destination);
    if (!callbacks) return;
    callbacks.delete(onMessage);

    if (callbacks.size === 0) {
      listenersByDestination.delete(destination);
      const subscription = liveSubscriptions.get(destination);
      subscription?.unsubscribe();
      liveSubscriptions.delete(destination);
    }
  };
};
