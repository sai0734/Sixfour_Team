// 메인페이지의 "우리가 도와줄게요" 같은 포스트잇/마스킹테이프 느낌 라벨.
// 준비관리/커뮤니티/마이페이지 전반의 섹션 소제목에 재사용.
//
// 사용 예:
//   <TapeLabel>오늘의 준비 현황</TapeLabel>
//   <TapeLabel tone="white" rotate="right">이야기 나누기</TapeLabel>
const TONE_CLASSES = {
  // 연한 브랜드컬러 배경 (섹션 헤더용 - PrepLayout/MyPageLayout과 통일)
  light: "bg-brand-light text-brand-accent",
  // 흰 배경 + 그림자 (밝은/색 있는 배경 위에 얹을 때용)
  white:
    "bg-white text-brand-accent shadow-[0_4px_10px_-6px_rgba(58,54,47,0.3)]",
};

const TapeLabel = ({
  children,
  tone = "light",
  rotate = "left",
  className = "",
}) => {
  const rotateClass = rotate === "right" ? "rotate-2" : "-rotate-2";

  return (
    <span
      className={`inline-block font-hand text-sm px-4 py-1.5 rounded-full ${rotateClass} ${TONE_CLASSES[tone]} ${className}`}
    >
      {children}
    </span>
  );
};

export default TapeLabel;
