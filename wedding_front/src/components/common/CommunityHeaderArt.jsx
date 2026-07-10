// 커뮤니티(자유게시판/후기게시판/FAQ) 헤더용 라인아트 - 대화하는 말풍선 + 웨딩링 포인트.
const CommunityHeaderArt = ({ className = "" }) => (
  <svg
    viewBox="0 0 120 80"
    fill="none"
    className={className}
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* 말풍선 큰 것 */}
    <path
      d="M28 20h40a6 6 0 0 1 6 6v16a6 6 0 0 1-6 6H46l-8 8v-8h-10a6 6 0 0 1-6-6V26a6 6 0 0 1 6-6z"
      fill="#FFFDF9"
      stroke="#E4C9B8"
      strokeWidth="1.5"
      transform="rotate(-3 48 38)"
    />
    <g transform="rotate(-3 48 38)">
      <line
        x1="34"
        y1="30"
        x2="62"
        y2="30"
        stroke="#D9C4B0"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <line
        x1="34"
        y1="37"
        x2="56"
        y2="37"
        stroke="#D9C4B0"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </g>

    {/* 말풍선 작은 것 (뒤에 겹쳐서) */}
    <path
      d="M0 0h26a5 5 0 0 1 5 5v10a5 5 0 0 1-5 5H10l-6 6v-6H0a5 5 0 0 1-5-5V5A5 5 0 0 1 0 0z"
      fill="#F5CBCB"
      opacity="0.6"
      transform="translate(78 12) rotate(6)"
    />

    {/* 겹친 웨딩링 포인트 */}
    <circle cx="90" cy="56" r="9" stroke="#C06080" strokeWidth="2" />
    <circle cx="100" cy="56" r="9" stroke="#E8A8B8" strokeWidth="2" />
  </svg>
);

export default CommunityHeaderArt;
