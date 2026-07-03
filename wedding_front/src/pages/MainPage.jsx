<<<<<<< HEAD
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
=======
import BasicMenu from "../components/menus/BasicMenu";

const MainPage = () => {
  return (
    <>
      <BasicMenu />

      <section className="hero">
        <svg
          className="hero-bg"
          viewBox="0 0 1440 900"
          preserveAspectRatio="xMidYMid slice"
        >
          <defs>
            <linearGradient id="skyGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#3A2436" />
              <stop offset="45%" stopColor="#6B3F52" />
              <stop offset="75%" stopColor="#C97D8E" />
              <stop offset="100%" stopColor="#E8B8A8" />
            </linearGradient>
            <linearGradient id="floorGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#2A1B22" />
              <stop offset="100%" stopColor="#1A1015" />
            </linearGradient>
            <radialGradient id="archGlow" cx="50%" cy="35%" r="55%">
              <stop offset="0%" stopColor="#FFE3D0" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#FFE3D0" stopOpacity="0" />
            </radialGradient>
          </defs>
          <rect x="0" y="0" width="1440" height="900" fill="url(#skyGrad)" />
          <rect
            x="0"
            y="640"
            width="1440"
            height="260"
            fill="url(#floorGrad)"
          />
          <ellipse cx="720" cy="430" rx="420" ry="380" fill="url(#archGlow)" />

          <g opacity="0.5">
            <line
              x1="160"
              y1="240"
              x2="160"
              y2="660"
              stroke="#F4D9C8"
              strokeWidth="2"
            />
            <line
              x1="1280"
              y1="240"
              x2="1280"
              y2="660"
              stroke="#F4D9C8"
              strokeWidth="2"
            />
            <circle cx="160" cy="240" r="5" fill="#FFE9B0" />
            <circle cx="1280" cy="240" r="5" fill="#FFE9B0" />
          </g>

          <path
            d="M520 660 V430 a200 200 0 0 1 400 0 V660"
            fill="none"
            stroke="#F7E7DC"
            strokeWidth="6"
            opacity="0.85"
          />
          <path
            d="M540 660 V435 a180 180 0 0 1 360 0 V660"
            fill="none"
            stroke="#F7E7DC"
            strokeWidth="2"
            opacity="0.5"
          />

          <g opacity="0.9">
            <path
              d="M560 420 q40 -60 80 0"
              stroke="#E7A0B0"
              strokeWidth="3"
              fill="none"
            />
            <path
              d="M800 420 q40 -60 80 0"
              stroke="#E7A0B0"
              strokeWidth="3"
              fill="none"
            />
            <circle cx="600" cy="400" r="6" fill="#F0C2CC" />
            <circle cx="840" cy="400" r="6" fill="#F0C2CC" />
          </g>

          <g>
            <ellipse cx="660" cy="560" rx="3" ry="40" fill="#2A1B22" />
            <ellipse cx="660" cy="600" rx="34" ry="14" fill="#241620" />
            <ellipse cx="780" cy="555" rx="3" ry="45" fill="#2A1B22" />
            <ellipse cx="780" cy="600" rx="22" ry="40" fill="#241620" />
          </g>

          <g opacity="0.8">
            <circle cx="300" cy="160" r="2" fill="#FFE9B0" />
            <circle cx="380" cy="100" r="2.5" fill="#FFE9B0" />
            <circle cx="1100" cy="130" r="2" fill="#FFE9B0" />
            <circle cx="1180" cy="200" r="2.5" fill="#FFE9B0" />
            <circle cx="220" cy="320" r="1.8" fill="#FFE9B0" />
            <circle cx="1250" cy="350" r="1.8" fill="#FFE9B0" />
          </g>

          <rect
            x="0"
            y="0"
            width="1440"
            height="900"
            fill="#000000"
            opacity="0.18"
          />
        </svg>

        <div className="hero-content">
          <p className="hero-eyebrow serif">WEDDING ALL IN ONE</p>
          <p className="hero-title serif">
            두 사람의 시작을
            <br />
            가장 가까이에서
          </p>
          <p className="hero-sub serif">
            예식장을 고르는 순간부터 답례품을 고민하는 순간까지
            <br />
            결혼이라는 가장 어려운 일을, 함께 준비합니다
          </p>
        </div>

        <div className="scroll-indicator">
          <span>SCROLL</span>
          <div className="scroll-line"></div>
        </div>
      </section>

      <section className="about">
        <p className="eyebrow" style={{ textAlign: "center" }}>
          ABOUT US
        </p>
        <p className="about-text">
          웨딩올인원은 업체 탐색부터 준비 일정 관리,
          <br />
          답례품 준비까지 결혼을 준비하는 모든 과정을 하나로 모았습니다
        </p>
        <div className="about-buttons">
          <a href="#" className="btn btn-outline">
            업체 둘러보기
          </a>
          <a href="#" className="btn btn-brand">
            지금 시작하기
          </a>
        </div>
      </section>

      <section className="feature-section">
        <div className="feature-grid">
          <div className="feature-text">
            <p className="eyebrow">01 — AI WEDDING PLAN</p>
            <p className="feature-title">
              예산과 날짜만 알면
              <br />
              AI가 웨딩플랜을 짜드려요
            </p>
            <p className="feature-desc">
              예산, 날짜, 하객 수, 원하는 스타일을 입력하면
              <br />
              일정과 예산 배분까지 자동으로 설계하고, 꼭 맞는
              웨딩홀·스드메·예복까지 추천해드립니다
            </p>
            <a href="#" className="feature-link">
              AI 웨딩플랜 만들어보기 →
            </a>
          </div>
          <div
            className="feature-image"
            style={{ background: "linear-gradient(135deg, #FBEAF0, #EEDFE8)" }}
          >
            <svg
              viewBox="0 0 24 24"
              fill="currentColor"
              style={{ color: "#D4537E", opacity: "0.4" }}
            >
              <path d="M12 2l1.6 5.4L19 9l-5.4 1.6L12 16l-1.6-5.4L5 9l5.4-1.6z" />
              <path d="M19 14l.8 2.7 2.7.8-2.7.8-.8 2.7-.8-2.7-2.7-.8 2.7-.8z" />
            </svg>
>>>>>>> 70c73be166854e72c3573de7861da3374a964347
          </div>
        </div>
      </section>

<<<<<<< HEAD
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
=======
      <section className="feature-section alt">
        <div className="feature-grid reverse">
          <div
            className="feature-image"
            style={{ background: "linear-gradient(135deg, #E1F5EE, #D4ECE3)" }}
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="#0F6E56"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              style={{ opacity: "0.4" }}
            >
              <path d="M9 6h11M9 12h11M9 18h11" />
              <path d="m3 6 1 1 2-2M3 12l1 1 2-2M3 18l1 1 2-2" />
            </svg>
          </div>
          <div className="feature-text">
            <p className="eyebrow">02 — PREPARATION</p>
            <p className="feature-title">
              D-day까지 해야 할 일을
              <br />
              하나씩 정리해드려요
            </p>
            <p className="feature-desc">
              예식까지 남은 시간을 기준으로
              <br />
              체크리스트와 예산, 납부 일정을 자동으로 관리합니다
            </p>
            <a href="#" className="feature-link">
              준비관리 둘러보기 →
            </a>
          </div>
        </div>
      </section>

      <section className="feature-section">
        <div className="feature-grid">
          <div className="feature-text">
            <p className="eyebrow">03 — GIFT SHOP</p>
            <p className="feature-title">
              하객들에게 전하는
              <br />
              마음, 답례품 쇼핑몰
            </p>
            <p className="feature-desc">
              캔들, 디퓨저, 수건 세트까지
              <br />
              취향대로 고르고 바로 주문할 수 있어요
            </p>
            <a href="#" className="feature-link">
              답례품 구경하기 →
            </a>
          </div>
          <div
            className="feature-image"
            style={{ background: "linear-gradient(135deg, #FAEEDA, #F3E0C2)" }}
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="#854F0B"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              style={{ opacity: "0.4" }}
            >
              <circle cx="9" cy="21" r="1" />
              <circle cx="19" cy="21" r="1" />
              <path d="M2.5 3h2l2.4 12.4a2 2 0 0 0 2 1.6h8.6a2 2 0 0 0 2-1.6L21.5 7H6" />
            </svg>
          </div>
        </div>
      </section>

      <section className="trust-section">
        <div className="trust-inner">
          <div className="stats-grid">
            <div>
              <p className="stat-num">
                12,400<span>명+</span>
              </p>
              <p className="stat-label">이번 달 AI 업체 추천을 받았어요</p>
            </div>
            <div>
              <p className="stat-num">
                3,800<span>건+</span>
              </p>
              <p className="stat-label">실제 예약·계약으로 이어졌어요</p>
            </div>
            <div>
              <p className="stat-num">
                4.8<span>/5</span>
              </p>
              <p className="stat-label">이용자 평균 만족도 평점</p>
            </div>
          </div>

          <div className="reviews-heading">
            <p className="eyebrow">REAL REVIEWS</p>
            <p>먼저 준비한 분들의 이야기</p>
          </div>

          <div className="review-grid">
            <div className="review-card">
              <div className="review-stars">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
              </div>
              <p className="review-text">
                예산이랑 결혼식 날짜만 입력했는데 일정이랑 예산 배분까지 한 번에
                짜주고, 그 안에서 딱 맞는 웨딩홀까지 추천해줬어요. 막막했던
                시작이 훨씬 가벼워졌습니다.
              </p>
              <p className="review-author">김O진 · 강남구 웨딩홀 예약</p>
            </div>
            <div className="review-card">
              <div className="review-stars">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
              </div>
              <p className="review-text">
                체크리스트 덕분에 뭘 놓치고 있는지 한눈에 보여서 정말
                든든했어요. D-day 기준으로 알림까지 와서 계약금 미납 같은 실수를
                막을 수 있었습니다.
              </p>
              <p className="review-author">박O영 · 준비관리 이용</p>
            </div>
            <div className="review-card">
              <div className="review-stars">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2.5l3 6.5 7 .8-5.2 4.8 1.4 7-6.2-3.6-6.2 3.6 1.4-7L2 9.8l7-.8z" />
                </svg>
              </div>
              <p className="review-text">
                답례품 고르는 것도 일이었는데 종류가 많고 후기도 자세해서 고민
                없이 골랐어요. 하객분들 반응도 좋았고 주문부터 배송까지
                빨랐습니다.
              </p>
              <p className="review-author">이O희 · 답례품 쇼핑몰 이용</p>
            </div>
          </div>
        </div>
      </section>

      <section className="cta-section">
        <p className="cta-title">지금, 결혼 준비를 시작해보세요</p>
        <p className="cta-sub">가입은 3초, 준비는 훨씬 가벼워집니다</p>
        <p className="cta-benefit">지금 가입하면 AI 업체 추천 1회 무료</p>
        <a
          href="#"
          className="btn btn-brand"
          style={{ height: "48px", padding: "0 36px", fontSize: "14px" }}
        >
          무료로 시작하기
        </a>
      </section>

      <footer>
        <div className="footer-inner">
          <div className="footer-top">
            <div className="footer-logo">
              <p>웨딩올인원</p>
              <p>wedding all in one</p>
            </div>
            <div className="footer-links">
>>>>>>> 70c73be166854e72c3573de7861da3374a964347
              <a href="#">회사소개</a>
              <a href="#">이용약관</a>
              <a href="#">개인정보처리방침</a>
            </div>
          </div>
<<<<<<< HEAD
          <div className="mb-6 text-xs leading-6 text-[#a8a6a0]">
            <p>(주)웨딩올인원 서울특별시 강남구 테헤란로 123 올인원빌딩 5층</p>
            <p>사업자등록번호 261-81-00000 · 대표전화 1588-0000 · 팩스 02-000-0000</p>
          </div>
          <p className="text-xs text-[#c9c6ba]">© 2026 웨딩올인원. All rights reserved.</p>
        </div>
      </footer>
    </div>
=======

          <div className="footer-info">
            <p>
              (주)웨딩올인원&nbsp; 서울특별시 강남구 테헤란로 123 올인원빌딩 5층
            </p>
            <p>
              5F Allinone Bldg, 123 Teheran-ro, Gangnam, Seoul, Republic of
              Korea&nbsp; 사업자등록번호 261-81-00000&nbsp; 대표전화
              1588-0000&nbsp; 팩스 02-000-0000
            </p>
          </div>

          <div className="footer-sns">
            <a href="#" className="sns-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M4 8h13v6a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z" />
                <path d="M17 9h2a2.5 2.5 0 0 1 0 5h-2" />
              </svg>
            </a>
            <a href="#" className="sns-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <rect x="3" y="3" width="18" height="18" rx="5" />
                <circle cx="12" cy="12" r="3.5" />
                <circle cx="17.2" cy="6.8" r="0.6" fill="currentColor" />
              </svg>
            </a>
            <a href="#" className="sns-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <rect x="2.5" y="6" width="19" height="12" rx="3" />
                <path
                  d="M10.5 9.5v5l4.5-2.5z"
                  fill="currentColor"
                  stroke="none"
                />
              </svg>
            </a>
            <a href="#" className="sns-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M4 5h16v11H9l-4 4z" />
                <path d="M8 9h8M8 12h5" />
              </svg>
            </a>
          </div>

          <p className="footer-copy">© 2026 웨딩올인원. All rights reserved.</p>
        </div>
      </footer>

      <style>{`

  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: -apple-system, "Pretendard", "Apple SD Gothic Neo", "Malgun Gothic", sans-serif;
    color: #3D3D3A;
    background: #ffffff;
    font-size: 14px;
    line-height: 1.5;
  }
  a { text-decoration: none; color: inherit; }
  .serif { font-family: Georgia, "Noto Serif KR", serif; }

  /* ===== 공통 유틸 ===== */
  .container {
    max-width: 1140px;
    margin: 0 auto;
    padding: 0 24px;
  }
  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 44px;
    padding: 0 28px;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    border: none;
  }
  .btn-outline {
    border: 1px solid #D9D7CD;
    background: #fff;
    color: #3D3D3A;
  }
  .btn-outline:hover { background: #F7F6F3; }
  .btn-brand {
    background: #D4537E;
    color: #fff;
  }
  .btn-brand:hover { background: #B8436A; }

  .eyebrow {
    font-size: 12px;
    letter-spacing: 0.15em;
    color: #993556;
    margin-bottom: 12px;
  }

  /* ===== 히어로 ===== */
  .hero {
    position: relative;
    width: 100%;
    height: 100vh;
    min-height: 680px;
    overflow: hidden;
  }
  .hero-bg {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    width: 100%;
    height: 100%;
  }
  .hero-content {
    position: relative;
    z-index: 10;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    padding: 0 24px;
  }
  .hero-eyebrow {
    font-size: 13px;
    letter-spacing: 0.2em;
    color: rgba(255,255,255,0.8);
    margin-bottom: 20px;
  }
  .hero-title {
    font-size: 52px;
    color: #fff;
    line-height: 1.25;
    margin-bottom: 24px;
  }
  .hero-sub {
    font-size: 14px;
    color: rgba(255,255,255,0.75);
    line-height: 1.7;
    max-width: 440px;
    font-style: italic;
  }
  .scroll-indicator {
    position: absolute;
    bottom: 40px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 10;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  .scroll-indicator span {
    font-size: 11px;
    color: rgba(255,255,255,0.6);
    letter-spacing: 0.05em;
  }
  .scroll-line {
    width: 1px;
    height: 32px;
    background: rgba(255,255,255,0.4);
  }

  /* ===== About ===== */
  .about {
    padding: 96px 24px;
    text-align: center;
    max-width: 720px;
    margin: 0 auto;
  }
  .about-text {
    font-size: 22px;
    line-height: 1.6;
    color: #3D3D3A;
    margin-bottom: 32px;
  }
  .about-buttons {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }

  /* ===== 기능 소개 섹션 (01/02/03) ===== */
  .feature-section {
    padding: 40px 0;
  }
  .feature-section.alt {
    background: #F7F6F3;
  }
  .feature-grid {
    max-width: 1140px;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: center;
    min-height: 420px;
    gap: 24px;
  }
  .feature-grid.reverse .feature-text { order: 2; }
  .feature-grid.reverse .feature-image { order: 1; }
  .feature-text { padding: 0 16px; }
  .feature-title {
    font-size: 26px;
    font-weight: 500;
    line-height: 1.4;
    margin-bottom: 16px;
  }
  .feature-desc {
    font-size: 14px;
    color: #73726C;
    line-height: 1.7;
    margin-bottom: 24px;
  }
  .feature-link {
    font-size: 13px;
    color: #993556;
    border-bottom: 1px solid #993556;
    padding-bottom: 2px;
  }
  .feature-image {
    aspect-ratio: 4 / 3;
    border-radius: 16px;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .feature-image svg { width: 64px; height: 64px; }

  /* ===== 신뢰 섹션 ===== */
  .trust-section {
    padding: 80px 24px;
  }
  .trust-inner {
    max-width: 1140px;
    margin: 0 auto;
  }
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24px;
    text-align: center;
    margin-bottom: 64px;
  }
  .stat-num {
    font-size: 36px;
    font-weight: 600;
    color: #4B1528;
    margin-bottom: 4px;
  }
  .stat-num span { font-size: 20px; }
  .stat-label {
    font-size: 13px;
    color: #73726C;
  }
  .reviews-heading {
    text-align: center;
    margin-bottom: 48px;
  }
  .reviews-heading .eyebrow { text-align: center; margin-bottom: 12px; }
  .reviews-heading p {
    font-size: 22px;
    font-weight: 500;
  }
  .review-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
  .review-card {
    background: #fff;
    border: 1px solid #E5E3DC;
    border-radius: 16px;
    padding: 24px;
  }
  .review-stars {
    display: flex;
    gap: 2px;
    margin-bottom: 12px;
  }
  .review-stars svg { width: 14px; height: 14px; color: #EF9F27; }
  .review-text {
    font-size: 13px;
    color: #3D3D3A;
    line-height: 1.7;
    margin-bottom: 16px;
  }
  .review-author {
    font-size: 12px;
    color: #A8A6A0;
  }

  /* ===== CTA ===== */
  .cta-section {
    background: #FBEAF0;
    padding: 80px 24px;
    text-align: center;
  }
  .cta-title {
    font-size: 22px;
    font-weight: 500;
    color: #4B1528;
    margin-bottom: 12px;
  }
  .cta-sub {
    font-size: 14px;
    color: #993556;
    margin-bottom: 4px;
  }
  .cta-benefit {
    font-size: 12px;
    color: #993556;
    margin-bottom: 32px;
  }

  /* ===== 푸터 ===== */
  footer {
    background: #fff;
    border-top: 1px solid #E5E3DC;
    padding: 48px 32px 32px;
  }
  .footer-inner {
    max-width: 1100px;
    margin: 0 auto;
  }
  .footer-top {
    display: flex;
    align-items: flex-start;
    gap: 48px;
    margin-bottom: 28px;
  }
  .footer-logo p:first-child {
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .footer-logo p:last-child {
    font-size: 11px;
    color: #A8A6A0;
    font-style: italic;
    margin-top: -2px;
  }
  .footer-links {
    display: flex;
    align-items: center;
    gap: 28px;
    font-size: 13px;
    color: #5F5E5A;
    margin-top: 6px;
  }
  .footer-links a:hover { color: #3D3D3A; }
  .footer-info {
    font-size: 12px;
    color: #A8A6A0;
    line-height: 1.7;
    margin-bottom: 24px;
  }
  .footer-sns {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;
  }
  .sns-icon {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: #3D3D3A;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .sns-icon:hover { background: #D4537E; }
  .sns-icon svg { width: 16px; height: 16px; }
  .footer-copy {
    font-size: 11px;
    color: #C9C6BA;
  }

      `}</style>
    </>
>>>>>>> 70c73be166854e72c3573de7861da3374a964347
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
