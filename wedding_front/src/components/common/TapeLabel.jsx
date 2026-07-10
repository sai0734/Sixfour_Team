// 메인페이지의 D-day 위젯/후기 카드에 쓰인 "압정으로 꽂아둔 메모지" 장식을
// 재사용 가능하게 뽑은 컴포넌트. 둥근 알약 배지가 아니라, 살짝 기울어진
// 각진 메모지 + 위에 압정 점 하나가 붙어있는 형태.
//
// 색상은 Tailwind 토큰(brand-accent 등)이 아니라 메인페이지 .tape CSS의
// 실제 값(#FFE2E2 / #C06080)을 그대로 하드코딩함. 토큰을 거치면 그동안
// 몇 번이나 "적용이 안 된 것 같다"는 문제가 반복됐어서, 여기서만큼은
// 무조건 진하게 보이도록 확정값을 씀.
//
// 사용 예:
//   <TapeLabel>오늘의 준비 현황</TapeLabel>
//   <TapeLabel tone="white" rotate={3}>이야기 나누기</TapeLabel>
const TONE_STYLES = {
  // 연한 핑크 메모지 (기본 - 흰/크림 배경 위에)
  light: { background: "#FFE2E2", color: "#C06080" },
  // 흰 메모지 (색 있는 헤더 배경 위에 얹을 때)
  white: { background: "#FFFFFF", color: "#C06080" },
};

const TapeLabel = ({
  children,
  tone = "light",
  rotate = -3,
  className = "",
}) => {
  const style = TONE_STYLES[tone] || TONE_STYLES.light;

  return (
    <div
      className={`relative inline-block ${className}`}
      style={{ transform: `rotate(${rotate}deg)` }}
    >
      {/* 압정 */}
      <span
        className="absolute -top-1.5 left-1/2 -translate-x-1/2 w-2.5 h-2.5 rounded-full z-10"
        style={{
          background: "#C87070",
          boxShadow: "0 2px 5px rgba(0,0,0,0.35)",
        }}
      />
      {/* 메모지 본체 - rounded-sm으로 각지게, pill 아님 */}
      <span
        className="block font-hand text-sm font-bold px-5 py-2.5 rounded-sm shadow-[0_8px_18px_-8px_rgba(58,54,47,0.4)]"
        style={style}
      >
        {children}
      </span>
    </div>
  );
};

export default TapeLabel;
