// 마이페이지 헤더용 라인아트 - 하트 + 부케 모티프.
const MyPageHeaderArt = ({ className = "" }) => (
  <svg
    viewBox="0 0 120 80"
    fill="none"
    className={className}
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* 부케 - 작은 꽃송이들 */}
    <g transform="translate(30 44) rotate(-8)">
      <line
        x1="0"
        y1="0"
        x2="4"
        y2="20"
        stroke="#B8C8A4"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <circle cx="0" cy="-6" r="7" fill="#F5CBCB" />
      <circle cx="-9" cy="1" r="6" fill="#E8A8B8" />
      <circle cx="8" cy="0" r="6" fill="#F0D9C8" />
      <circle cx="-3" cy="-13" r="5" fill="#C5B3D3" />
      <circle cx="6" cy="-11" r="5" fill="#FFE2E2" />
    </g>

    {/* 하트 카드 */}
    <rect
      x="64"
      y="16"
      width="40"
      height="46"
      rx="4"
      transform="rotate(-5 84 39)"
      fill="#FFFDF9"
      stroke="#E4C9B8"
      strokeWidth="1.5"
    />
    <path
      transform="rotate(-5 84 39)"
      d="M84 46c-6-4.2-10-7.7-10-12.2 0-3 2.3-5.3 5.2-5.3 1.9 0 3.6.9 4.8 2.4 1.2-1.5 2.9-2.4 4.8-2.4 2.9 0 5.2 2.3 5.2 5.3 0 4.5-4 8-10 12.2z"
      fill="#C06080"
    />
  </svg>
);

export default MyPageHeaderArt;
