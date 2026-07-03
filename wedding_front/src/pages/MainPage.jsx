import { Link } from "react-router-dom";

const features = [
  {
    eyebrow: "01 · AI WEDDING PLAN",
    title: "예산과 날짜만 알면\nAI가 웨딩 플랜을 짜드려요",
    desc: "예산, 날짜, 선호 스타일을 입력하면 일정과 예산 배분을 자동으로 설계하고, 꼭 맞는 웨딩홀과 스드메 업체를 추천합니다.",
    link: "AI 웨딩 플랜 만들어보기",
    tone: "from-pink-50 to-rose-100 text-rose-500",
    icon: "✦",
  },
  {
    eyebrow: "02 · PREPARATION",
    title: "D-day까지 해야 할 일을\n하나씩 정리해드려요",
    desc: "결혼식까지 남은 기간을 기준으로 체크리스트, 예산, 준비 일정을 한눈에 관리할 수 있습니다.",
    link: "준비 관리 둘러보기",
    tone: "from-emerald-50 to-teal-100 text-emerald-700",
    icon: "✓",
    reverse: true,
  },
  {
    eyebrow: "03 · GIFT SHOP",
    title: "하객에게 전하는 마음,\n답례품까지 한 번에",
    desc: "캔들, 디퓨저, 수건 세트 등 다양한 답례품을 취향대로 고르고 바로 주문할 수 있습니다.",
    link: "답례품 구경하기",
    tone: "from-amber-50 to-orange-100 text-amber-700",
    icon: "♡",
  },
];

const reviews = [
  {
    text: "예산이랑 결혼식 날짜만 입력했는데 일정이랑 예산 배분까지 한 번에 정리되어서 시작이 훨씬 가벼워졌어요.",
    author: "김O진 · 강남구 웨딩홀 예약",
  },
  {
    text: "체크리스트 덕분에 무엇을 놓치고 있는지 바로 보여서 든든했습니다. 계약일과 준비 일정 관리가 특히 편했어요.",
    author: "박O현 · 준비 관리 이용",
  },
  {
    text: "업체 비교와 답례품 주문까지 한 화면에서 이어져서 좋았어요. 여러 사이트를 오갈 필요가 줄었습니다.",
    author: "이O수 · 답례품 쇼핑 이용",
  },
];

const MainPage = () => {
  return (
    <div className="min-h-screen bg-white text-[#3d3d3a]">
      <header className="absolute left-0 right-0 top-0 z-20">
        <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-8 text-white">
          <Link to="/" className="flex items-center gap-2 text-lg font-semibold">
            <span className="text-xl">♡</span>
            <span>웨딩올인원</span>
          </Link>
          <nav className="hidden items-center gap-10 text-sm font-medium md:flex">
            <a href="#about" className="border-b border-white pb-1">홈</a>
            <a href="#plan">AI 웨딩플랜</a>
            <Link to="/companies">업체 찾기</Link>
            <a href="#gift">답례품</a>
            <a href="#reviews">후기</a>
          </nav>
          <div className="flex items-center gap-4 text-sm">
            <Link to="/auth/login">로그인</Link>
            <Link to="/companies" className="rounded-full bg-white px-5 py-2 font-medium text-[#4b1528]">
              시작하기
            </Link>
          </div>
        </div>
      </header>

      <section className="relative flex min-h-[720px] items-center justify-center overflow-hidden text-center text-white">
        <HeroBackground />
        <div className="relative z-10 px-6">
          <p className="mb-5 font-serif text-sm tracking-[0.22em] text-white/80">WEDDING ALL IN ONE</p>
          <h1 className="font-serif text-4xl leading-tight md:text-6xl">
            두 사람의 시작을
            <br />
            가장 가까이에서
          </h1>
          <p className="mx-auto mt-6 max-w-xl font-serif text-sm italic leading-7 text-white/75">
            예식장을 고르는 시간부터 답례품을 고르는 순간까지,
            <br />
            결혼이라는 가장 특별한 일을 함께 준비합니다.
          </p>
        </div>
        <div className="absolute bottom-10 left-1/2 z-10 flex -translate-x-1/2 flex-col items-center gap-2 text-xs tracking-wider text-white/60">
          <span>SCROLL</span>
          <span className="h-8 w-px bg-white/40" />
        </div>
      </section>

      <section id="about" className="mx-auto max-w-3xl px-6 py-24 text-center">
        <p className="mb-3 text-xs tracking-[0.18em] text-[#993556]">ABOUT US</p>
        <p className="text-2xl leading-10">
          웨딩올인원은 업체 탐색부터 준비 일정 관리,
          <br />
          답례품 준비까지 결혼을 준비하는 모든 과정을 하나로 모읍니다.
        </p>
        <div className="mt-8 flex justify-center gap-3">
          <Link to="/companies" className="rounded-full border border-[#d9d7cd] px-7 py-3 text-sm hover:bg-[#f7f6f3]">
            업체 둘러보기
          </Link>
          <Link to="/companies" className="rounded-full bg-[#d4537e] px-7 py-3 text-sm font-medium text-white hover:bg-[#b8436a]">
            지금 시작하기
          </Link>
        </div>
      </section>

      {features.map((feature, index) => (
        <FeatureSection key={feature.eyebrow} feature={feature} index={index} />
      ))}

      <section id="reviews" className="px-6 py-20">
        <div className="mx-auto max-w-6xl">
          <div className="mb-16 grid grid-cols-3 gap-6 text-center">
            <Stat value="12,400" unit="명" label="이번 달 AI 업체 추천을 받았어요" />
            <Stat value="3,800" unit="건" label="실제 예약과 계약으로 이어졌어요" />
            <Stat value="4.8" unit="/5" label="이용자 평균 만족도" />
          </div>

          <div className="mb-12 text-center">
            <p className="mb-3 text-xs tracking-[0.18em] text-[#993556]">REAL REVIEWS</p>
            <h2 className="text-2xl font-medium">먼저 준비한 분들의 이야기</h2>
          </div>

          <div className="grid gap-5 md:grid-cols-3">
            {reviews.map((review) => (
              <div key={review.author} className="rounded-2xl border border-[#e5e3dc] bg-white p-6">
                <div className="mb-3 text-sm text-amber-500">★★★★★</div>
                <p className="text-sm leading-7">{review.text}</p>
                <p className="mt-4 text-xs text-[#a8a6a0]">{review.author}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-[#fbeaf0] px-6 py-20 text-center">
        <h2 className="mb-3 text-2xl font-medium text-[#4b1528]">지금 결혼 준비를 시작해보세요</h2>
        <p className="mb-1 text-sm text-[#993556]">가입은 3초, 준비는 훨씬 가벼워집니다.</p>
        <p className="mb-8 text-xs text-[#993556]">지금 가입하면 AI 업체 추천 1회 무료</p>
        <Link to="/companies" className="inline-flex h-12 items-center rounded-full bg-[#d4537e] px-9 text-sm font-medium text-white hover:bg-[#b8436a]">
          무료로 시작하기
        </Link>
      </section>

      <footer className="border-t border-[#e5e3dc] px-8 py-12">
        <div className="mx-auto max-w-6xl">
          <div className="mb-7 flex flex-wrap items-start gap-12">
            <div>
              <p className="text-xl font-bold">웨딩올인원</p>
              <p className="text-xs italic text-[#a8a6a0]">wedding all in one</p>
            </div>
            <div className="flex gap-7 pt-1 text-sm text-[#5f5e5a]">
              <a href="#">회사소개</a>
              <a href="#">이용약관</a>
              <a href="#">개인정보처리방침</a>
            </div>
          </div>
          <div className="mb-6 text-xs leading-6 text-[#a8a6a0]">
            <p>(주)웨딩올인원 서울특별시 강남구 테헤란로 123 올인원빌딩 5층</p>
            <p>사업자등록번호 261-81-00000 · 대표전화 1588-0000 · 팩스 02-000-0000</p>
          </div>
          <p className="text-xs text-[#c9c6ba]">© 2026 웨딩올인원. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
};

const FeatureSection = ({ feature }) => (
  <section id={feature.eyebrow.includes("GIFT") ? "gift" : feature.eyebrow.includes("AI") ? "plan" : undefined} className={`py-10 ${feature.reverse ? "bg-[#f7f6f3]" : "bg-white"}`}>
    <div className={`mx-auto grid max-w-6xl items-center gap-6 px-6 md:grid-cols-2 ${feature.reverse ? "md:[&>*:first-child]:order-2" : ""}`}>
      <div className="px-4">
        <p className="mb-3 text-xs tracking-[0.18em] text-[#993556]">{feature.eyebrow}</p>
        <h2 className="whitespace-pre-line text-2xl font-medium leading-9">{feature.title}</h2>
        <p className="mt-4 text-sm leading-7 text-[#73726c]">{feature.desc}</p>
        <a href="#" className="mt-6 inline-block border-b border-[#993556] pb-1 text-sm text-[#993556]">
          {feature.link} →
        </a>
      </div>
      <div className={`flex aspect-[4/3] items-center justify-center rounded-2xl bg-gradient-to-br ${feature.tone}`}>
        <span className="text-7xl opacity-40">{feature.icon}</span>
      </div>
    </div>
  </section>
);

const Stat = ({ value, unit, label }) => (
  <div>
    <p className="mb-1 text-4xl font-semibold text-[#4b1528]">
      {value}
      <span className="text-xl">{unit}</span>
    </p>
    <p className="text-sm text-[#73726c]">{label}</p>
  </div>
);

const HeroBackground = () => (
  <svg className="absolute inset-0 h-full w-full" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice">
    <defs>
      <linearGradient id="mainSkyGrad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#3A2436" />
        <stop offset="45%" stopColor="#6B3F52" />
        <stop offset="75%" stopColor="#C97D8E" />
        <stop offset="100%" stopColor="#E8B8A8" />
      </linearGradient>
      <linearGradient id="mainFloorGrad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stopColor="#2A1B22" />
        <stop offset="100%" stopColor="#1A1015" />
      </linearGradient>
      <radialGradient id="mainArchGlow" cx="50%" cy="35%" r="55%">
        <stop offset="0%" stopColor="#FFE3D0" stopOpacity="0.9" />
        <stop offset="100%" stopColor="#FFE3D0" stopOpacity="0" />
      </radialGradient>
    </defs>
    <rect width="1440" height="900" fill="url(#mainSkyGrad)" />
    <rect x="0" y="640" width="1440" height="260" fill="url(#mainFloorGrad)" />
    <ellipse cx="720" cy="430" rx="420" ry="380" fill="url(#mainArchGlow)" />
    <path d="M520 660 V430 a200 200 0 0 1 400 0 V660" fill="none" stroke="#F7E7DC" strokeWidth="6" opacity="0.85" />
    <path d="M540 660 V435 a180 180 0 0 1 360 0 V660" fill="none" stroke="#F7E7DC" strokeWidth="2" opacity="0.5" />
    <g opacity="0.8">
      <circle cx="300" cy="160" r="2" fill="#FFE9B0" />
      <circle cx="380" cy="100" r="2.5" fill="#FFE9B0" />
      <circle cx="1100" cy="130" r="2" fill="#FFE9B0" />
      <circle cx="1180" cy="200" r="2.5" fill="#FFE9B0" />
      <circle cx="220" cy="320" r="1.8" fill="#FFE9B0" />
      <circle cx="1250" cy="350" r="1.8" fill="#FFE9B0" />
    </g>
    <rect width="1440" height="900" fill="#000000" opacity="0.18" />
  </svg>
);

export default MainPage;
