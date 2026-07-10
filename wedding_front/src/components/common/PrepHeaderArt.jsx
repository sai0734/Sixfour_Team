// 준비관리 헤더용 라인아트 - 웨딩링 + 체크리스트(캘린더) 모티프.
// 저작권 문제 없는 자체 제작 SVG. currentColor를 써서 className으로 색 조절 가능.
const PrepHeaderArt = ({ className = "" }) => (
  <svg
    viewBox="0 0 120 80"
    fill="none"
    className={className}
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* 겹친 웨딩링 두 개 */}
    <circle cx="38" cy="42" r="15" stroke="#C06080" strokeWidth="2.5" />
    <circle cx="52" cy="42" r="15" stroke="#E8A8B8" strokeWidth="2.5" />
    {/* 반짝임 */}
    <path
      d="M60 20l1.6 4.2L66 26l-4.4 1.8L60 32l-1.6-4.2L54 26l4.4-1.8L60 20z"
      fill="#F5CBCB"
    />

    {/* 체크리스트 카드 */}
    <rect
      x="72"
      y="18"
      width="34"
      height="44"
      rx="4"
      transform="rotate(6 89 40)"
      fill="#FFFDF9"
      stroke="#E4C9B8"
      strokeWidth="1.5"
    />
    <g transform="rotate(6 89 40)">
      <circle cx="80" cy="28" r="2.2" stroke="#C06080" strokeWidth="1.4" />
      <path
        d="M78.6 28l1 1 2-2.2"
        stroke="#C06080"
        strokeWidth="1.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <line
        x1="86"
        y1="28"
        x2="100"
        y2="28"
        stroke="#D9C4B0"
        strokeWidth="1.6"
        strokeLinecap="round"
      />

      <circle cx="80" cy="38" r="2.2" stroke="#C06080" strokeWidth="1.4" />
      <path
        d="M78.6 38l1 1 2-2.2"
        stroke="#C06080"
        strokeWidth="1.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <line
        x1="86"
        y1="38"
        x2="100"
        y2="38"
        stroke="#D9C4B0"
        strokeWidth="1.6"
        strokeLinecap="round"
      />

      <circle cx="80" cy="48" r="2.2" stroke="#D9C4B0" strokeWidth="1.4" />
      <line
        x1="86"
        y1="48"
        x2="97"
        y2="48"
        stroke="#D9C4B0"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
    </g>
  </svg>
);

export default PrepHeaderArt;
