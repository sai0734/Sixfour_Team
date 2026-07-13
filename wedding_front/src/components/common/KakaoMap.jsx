import { useEffect, useRef } from "react";
import useKakaoMap from "../../hooks/useKakaoMap";

const KakaoMap = ({ latitude, longitude, name, address }) => {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markerRef = useRef(null);
  const infowindowRef = useRef(null);
  const isLoaded = useKakaoMap();

  useEffect(() => {
    if (!isLoaded) return;
    if (!latitude || !longitude) return;

    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);
    if (isNaN(lat) || isNaN(lng)) return;

    const container = mapRef.current;
    if (!container) return;

    const position = new window.kakao.maps.LatLng(lat, lng);

    if (!mapInstanceRef.current) {
      // 지도 최초 생성
      mapInstanceRef.current = new window.kakao.maps.Map(container, {
        center: position,
        level: 3,
      });

      markerRef.current = new window.kakao.maps.Marker({ position });
      markerRef.current.setMap(mapInstanceRef.current);

      if (name) {
        infowindowRef.current = new window.kakao.maps.InfoWindow({
          content: `<div style="padding:6px 10px; font-size:13px; font-weight:600;">${name}</div>`,
        });
        infowindowRef.current.open(mapInstanceRef.current, markerRef.current);
      }
    } else {
      // 위치만 업데이트
      mapInstanceRef.current.setCenter(position);
      markerRef.current.setPosition(position);

      if (infowindowRef.current) {
        infowindowRef.current.close();
      }
      if (name) {
        infowindowRef.current = new window.kakao.maps.InfoWindow({
          content: `<div style="padding:6px 10px; font-size:13px; font-weight:600;">${name}</div>`,
        });
        infowindowRef.current.open(mapInstanceRef.current, markerRef.current);
      }
    }
  }, [isLoaded, latitude, longitude, name]);

  if (!latitude || !longitude) return null;

  return (
    <div className="mt-6">
      <p className="text-sm font-medium mb-2">위치</p>
      <div
        ref={mapRef}
        className="w-full h-64 rounded-xl overflow-hidden border border-line"
      />
      {address && <p className="mt-2 text-xs text-ink-faint">{address}</p>}
    </div>
  );
};

export default KakaoMap;
