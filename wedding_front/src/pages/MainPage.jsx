import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useSelector } from "react-redux";
import BasicMenu from "../components/menus/BasicMenu";
import { getMainHighlights } from "../api/homeApi";
import { getOne as getCompanyOne, getCompanyImageUrl } from "../api/companyApi";
import {
  API_SERVER_HOST,
  getListByMember as getMyReservations,
} from "../api/reservationApi";
import { getByMember } from "../api/weddingplanApi";

// 업체 방문 리스트 - 카테고리명 텍스트 대신 아이콘으로 표시
const CATEGORY_ICON = {
  STUDIO: "📸",
  HALL: "🏛️",
  DRESS: "👗🧥",
  MAKEUP: "💄",
};

const slides = [
  {
    eyebrow: "01 — AI WEDDING PLAN",
    title: "예산과 날짜만 알면\nAI가 웨딩플랜을 짜드려요",
    desc: "예산, 날짜, 하객수 원하는 스타일 알려주시면\n일정과 예산 배분까지 자동으로 설계해드립니다",
    cta: "AI 웨딩플랜 만들어보기 →",
    linkTo: "/aiplan",
  },
  {
    eyebrow: "02 — HALL · STUDIO · DRESS · MAKEUP",
    title:
      "웨딩홀부터 스튜디오,\n  드레스, 메이크업까지\n한눈에 비교하고 예약하세요",
    desc: "지역, 예산, 스타일로 필터링해서\n마음에 드는 업체를 바로 찾고 예약할 수 있어요",
    cta: "업체 둘러보기 →",
    linkTo: "/companies/list",
  },
  {
    eyebrow: "03 — PREPARATION",
    title: "D-day까지 해야 할 일을\n하나씩 정리해드려요",
    desc: "예식까지 남은 시간을 기준으로\n체크리스트와 예산, 납부 일정을 자동으로 관리합니다",
    cta: "준비관리 둘러보기 →",
    linkTo: "/prep/hub",
  },
  {
    eyebrow: "04 — GIFT SHOP",
    title: "하객들에게 전하는\n마음, 답례품 쇼핑몰",
    desc: "캔들, 디퓨저, 수건 세트까지\n취향대로 고르고 바로 주문할 수 있어요",
    cta: "답례품 구경하기 →",
    linkTo: "/product/",
  },
  {
    eyebrow: "05 — COMMUNITY",
    title: "같은 고민, 같은 설렘\n먼저 겪은 사람들과 나눠요",
    desc: "예식 후기부터 업체 정보, 소소한 고민까지\n자유롭게 묻고 답하며 함께 준비해요",
    cta: "커뮤니티 구경하기 →",
    linkTo: "/board/list",
  },
];

const calcDday = (dateStr) => {
  if (!dateStr) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(dateStr);
  const diff = Math.ceil((target - today) / 86400000);
  if (diff > 0) return `D-${diff}`;
  if (diff === 0) return "D-DAY";
  return `D+${Math.abs(diff)}`;
};

// "26.07.30" 형태로 짧게 표기 (예약 방문일 리스트용)
const formatShortDate = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const yy = String(d.getFullYear()).slice(-2);
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yy}.${mm}.${dd}`;
};

// "2026년 8월 15일" 형태로 표기 (D-day 카드의 예식일용)
// new Date(dateStr)로 파싱하면 타임존에 따라 하루 밀릴 수 있어서 문자열을 직접 분해한다.
const formatWeddingDate = (dateStr) => {
  if (!dateStr) return "";
  const [y, m, d] = dateStr.split("-");
  return `${y}년 ${Number(m)}월 ${Number(d)}일`;
};

const MainPage = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const isLoggedIn = !!loginState.email;
  const isAdmin = loginState.roleNames?.some((r) =>
    ["ADMIN", "ROLE_ADMIN"].includes(r),
  );
  const companyListPath = isAdmin ? "/admin/companies/list" : "/companies/list";

  const [activeSlide, setActiveSlide] = useState(0);
  const current = slides[activeSlide];

  const nickname =
    loginState.nickname || loginState.email?.split("@")[0] || "회원";

  const [weddingPlan, setWeddingPlan] = useState(null);
  const dday = calcDday(weddingPlan?.weddingDate) ?? "D-???";

  const [highlights, setHighlights] = useState({
    hallCompany: null,
    stylingCompany: null,
    topProduct: null,
  });
  const [upcomingVisits, setUpcomingVisits] = useState([]);
  const [budgetSpent, setBudgetSpent] = useState(0);

  const totalBudget = weddingPlan?.totalBudget || 0;
  const budgetRemaining = totalBudget - budgetSpent;
  const budgetPercent =
    totalBudget > 0
      ? Math.min(100, Math.round((budgetSpent / totalBudget) * 100))
      : 0;

  useEffect(() => {
    const timer = setInterval(() => {
      setActiveSlide((prev) => (prev + 1) % slides.length);
    }, 5000);
    return () => clearInterval(timer);
  }, []);

  // 로그인 상태에서만 쓰이는 위젯(D-day 카드, 방문 카드, 예산 카드)이라
  // 비로그인이면 아예 호출하지 않는다.
  useEffect(() => {
    if (!isLoggedIn) return;

    let cancelled = false;

    // D-day 카드 - 마이페이지 "플랜" 탭과 같은 소스(예식 플랜)에서 예식일/신랑신부 이름을 가져온다.
    getByMember(loginState.email)
      .then((data) => {
        if (!cancelled) setWeddingPlan(data || null);
      })
      .catch(() => {
        // 아직 등록한 플랜이 없으면 "이름 등록하기" 안내 유지
      });

    // 취향 카드 - 취소 안 된 예약 중 방문일이 남은 것 최대 3개를 D-day 임박순으로 보여준다.
    // 예산 카드 - 같은 예약 목록에서 결제완료(PAID) 건의 금액을 합산해 사용 금액으로 쓴다.
    getMyReservations(loginState.email)
      .then(async (data) => {
        if (cancelled) return;

        const spent = (data || [])
          .filter((r) => r.payStatus === "PAID")
          .reduce((sum, r) => sum + (r.amount || 0), 0);
        setBudgetSpent(spent);

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const upcoming = (data || [])
          .filter((r) => r.status !== "취소" && r.weddingDate)
          .filter((r) => new Date(r.weddingDate) >= today)
          .sort((a, b) => new Date(a.weddingDate) - new Date(b.weddingDate))
          .slice(0, 3);

        const uniqueCmnos = [
          ...new Set(upcoming.map((r) => r.cmno).filter(Boolean)),
        ];
        const results = await Promise.allSettled(
          uniqueCmnos.map((cmno) => getCompanyOne(cmno)),
        );
        const companyByCmno = {};
        results.forEach((res, i) => {
          if (res.status === "fulfilled") {
            companyByCmno[uniqueCmnos[i]] = res.value;
          }
        });

        if (!cancelled) {
          setUpcomingVisits(
            upcoming.map((r) => ({
              reservationId: r.reservationId,
              name: companyByCmno[r.cmno]?.name || `업체 #${r.cmno}`,
              category: companyByCmno[r.cmno]?.category,
              dateLabel: `${formatShortDate(r.weddingDate)}(${calcDday(r.weddingDate)})`,
            })),
          );
        }
      })
      .catch(() => {
        // 조회 실패 시 빈 리스트 유지
      });

    return () => {
      cancelled = true;
    };
  }, [isLoggedIn, loginState.email]);

  // 비로그인 폴라로이드(웨딩홀/스드메 매출 1위, 답례품 구매 1위)는 로그인 여부와 무관하게 공개 데이터라
  // 로그인 상태에서도 미리 받아둘 필요는 없어서 비로그인일 때만 호출한다.
  useEffect(() => {
    if (isLoggedIn) return;

    let cancelled = false;

    getMainHighlights()
      .then((data) => {
        if (!cancelled) setHighlights(data);
      })
      .catch((err) => console.error(err));

    return () => {
      cancelled = true;
    };
  }, [isLoggedIn]);

  const goSlide = (idx) => setActiveSlide(idx);

  return (
    <>
      <BasicMenu />

      {/* ===== HERO ===== */}
      <div className="hero-wrapper">
        <section className="hero">
          {/* 좌: 텍스트 슬라이드 */}
          <div className="hero-copy">
            <div className="hero-slide-content" key={activeSlide}>
              <div className="hero-eyebrow">{current.eyebrow}</div>
              <h1 style={{ whiteSpace: "pre-line" }}>{current.title}</h1>
              <p style={{ whiteSpace: "pre-line" }}>{current.desc}</p>
              <Link to={current.linkTo} className="hero-cta-link">
                {current.cta}
              </Link>
            </div>
            <div className="hero-dots">
              {slides.map((_, i) => (
                <button
                  key={i}
                  className={`hero-dot${activeSlide === i ? " active" : ""}`}
                  onClick={() => goSlide(i)}
                  aria-label={`${i + 1}번 슬라이드`}
                />
              ))}
            </div>
          </div>

          {/* 우: 비로그인 → 폴라로이드 / 로그인 → 회원 위젯 */}
          {!isLoggedIn ? (
            <div className="polaroid-stack">
              <div className="polaroid p1">
                <div className="photo">
                  {highlights.hallCompany?.imageUrl ? (
                    <img
                      src={getCompanyImageUrl(highlights.hallCompany.imageUrl)}
                      alt={highlights.hallCompany.name}
                    />
                  ) : (
                    <div className="photo-placeholder">
                      <span>💍</span>
                      <span className="ph-label">Wedding Hall</span>
                    </div>
                  )}
                </div>
                <div className="cap">
                  {highlights.hallCompany
                    ? `웨딩홀 인기 1위 · ${highlights.hallCompany.name}`
                    : "웨딩홀 탐색 중 💍"}
                </div>
              </div>
              <div className="polaroid p2">
                <div className="photo">
                  {highlights.stylingCompany?.imageUrl ? (
                    <img
                      src={getCompanyImageUrl(
                        highlights.stylingCompany.imageUrl,
                      )}
                      alt={highlights.stylingCompany.name}
                    />
                  ) : (
                    <div className="photo-placeholder">
                      <span>👗</span>
                      <span className="ph-label">Dress &amp; Studio</span>
                    </div>
                  )}
                </div>
                <div className="cap">
                  {highlights.stylingCompany
                    ? `스드메 인기 1위 · ${highlights.stylingCompany.name}`
                    : "스드메 고르는 중 👗"}
                </div>
              </div>
              <div className="polaroid p3">
                <div className="photo">
                  {highlights.topProduct?.imageUrl ? (
                    <img
                      src={`${API_SERVER_HOST}/api/product/view/${highlights.topProduct.imageUrl}`}
                      alt={highlights.topProduct.name}
                    />
                  ) : (
                    <div className="photo-placeholder">
                      <span>🎁</span>
                      <span className="ph-label">Gift Shop</span>
                    </div>
                  )}
                </div>
                <div className="cap">
                  {highlights.topProduct
                    ? `답례품 구매 1위 · ${highlights.topProduct.name}`
                    : "답례품 구경 중 🎁"}
                </div>
              </div>
            </div>
          ) : (
            <div className="member-widget">
              {/* D-day 노트 — 가장 뒤 (z-index 1) - 클릭 시 마이페이지 플랜 탭으로 */}
              <Link to="/mypage?tab=plan" className="w-card wc-dday">
                <div className="dday-pin" />
                <div className="dday-num">{dday}</div>
                <div className="dday-note">{nickname} 님, 안녕하세요. </div>
                <hr className="dday-divider" />
                {weddingPlan?.groomName && weddingPlan?.brideName ? (
                  <>
                    <div className="dday-couple">
                      {weddingPlan.brideName}
                      <span className="dday-heart">💍</span>
                      {weddingPlan.groomName}
                    </div>
                    {weddingPlan?.weddingDate && (
                      <div className="dday-date">
                        <span className="dday-date-deco">✦</span>
                        {formatWeddingDate(weddingPlan.weddingDate)}
                        <span className="dday-date-deco">✦</span>
                      </div>
                    )}
                  </>
                ) : (
                  <span className="dday-couple-link">내 플랜 등록하기 →</span>
                )}
              </Link>
              {/* 예약 방문 D-day 리스트 — 중간 (z-index 2) - 클릭 시 마이페이지 결제 내역 탭으로 */}
              <Link to="/mypage?tab=payment" className="w-card wc-taste">
                <div className="cap visit-title">업체 방문 일정 📅</div>
                {upcomingVisits.length > 0 ? (
                  <ul className="visit-list">
                    {upcomingVisits.map((v) => (
                      <li key={v.reservationId} className="visit-row">
                        <span className="visit-name">
                          {v.name}
                          {CATEGORY_ICON[v.category] && (
                            <span className="visit-category-icon">
                              {CATEGORY_ICON[v.category]}
                            </span>
                          )}
                        </span>
                        <span className="visit-dday">{v.dateLabel}</span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="visit-empty">예정된 업체 방문이 없어요</p>
                )}
              </Link>
              {/* 예산 사용 현황 — 가장 앞 (z-index 3) - 클릭 시 예산관리 페이지로 */}
              <Link to="/budget/list" className="w-card wc-ai">
                <div className="ai-label">
                  <div className="ai-dot" />
                  예산 사용 현황
                </div>
                {totalBudget > 0 ? (
                  <div className="budget-summary">
                    <div className="budget-bar-track">
                      <div
                        className="budget-bar-fill"
                        style={{ width: `${budgetPercent}%` }}
                      />
                    </div>
                    <div className="budget-bar-caption">
                      <span className="budget-percent">{budgetPercent}% 사용</span>
                      <span className="budget-total">
                        총예산 {totalBudget.toLocaleString()}원
                      </span>
                    </div>
                    <div className="budget-row">
                      <span className="budget-row-label">사용 금액</span>
                      <span className="budget-row-value">
                        {budgetSpent.toLocaleString()}원
                      </span>
                    </div>
                    <div className="budget-row">
                      <span className="budget-row-label">
                        {budgetRemaining < 0 ? "예산 초과" : "남은 예산"}
                      </span>
                      <span
                        className={`budget-row-value${budgetRemaining < 0 ? " over" : ""}`}
                      >
                        {Math.abs(budgetRemaining).toLocaleString()}원
                      </span>
                    </div>
                  </div>
                ) : (
                  <p className="ai-empty">
                    아직 예산을 설정하지 않았어요.
                  </p>
                )}
              </Link>
            </div>
          )}
        </section>
      </div>

      {/* ===== FEATURES ===== */}
      <section className="features">
        <div className="section-head">
          <span className="tape">우리가 도와줄게요</span>
          <h2>준비, 이렇게 함께해요</h2>
        </div>
        <div className="feat-row">
          <Link
            to="/aiplan"
            className="feat-card"
            aria-label="AI 웨딩플랜 페이지로 이동"
          >
            <div className="feat-icon">✨</div>
            <h3>AI 웨딩플랜</h3>
            <p>
              예산과 날짜만 알면 AI가 일정과 예산 배분까지 자동으로
              설계해드려요. 웨딩홀부터 스드메까지 딱 맞는 업체까지 추천해줍니다
            </p>
          </Link>
          <Link
            to="/prep/hub"
            className="feat-card"
            aria-label="준비관리 페이지로 이동"
          >
            <div className="feat-icon">📋</div>
            <h3>준비관리</h3>
            <p>
              D-day까지 해야 할 일을 체크리스트와 예산, 납부 일정으로 자동
              정리해드려요. 중요한 날짜를 절대 놓치지 않도록 챙겨드립니다
            </p>
          </Link>
          <Link
            to="/product/"
            className="feat-card"
            aria-label="답례품 쇼핑몰로 이동"
          >
            <div className="feat-icon">🎁</div>
            <h3>답례품 쇼핑몰</h3>
            <p>
              캔들, 디퓨저, 수건 세트까지 취향대로 골라 바로 주문하세요.
              하객분들이 기억하는 특별한 답례품을 함께 골라드립니다
            </p>
          </Link>
        </div>
      </section>

      {/* ===== TRUST ===== */}
      <section className="trust">
        <span className="tape">먼저 다녀간 커플들</span>
        <h2>우리처럼, 함께 준비했어요</h2>
        <div className="trust-stats">
          <div>
            <div className="stat-num">
              12,400<span style={{ fontSize: "22px" }}>+</span>
            </div>
            <div className="stat-label">함께한 커플</div>
          </div>
          <div>
            <div className="stat-num">
              3,800<span style={{ fontSize: "22px" }}>+</span>
            </div>
            <div className="stat-label">실제 예약·계약 성사</div>
          </div>
          <div>
            <div className="stat-num">
              4.8<span style={{ fontSize: "22px" }}>/5</span>
            </div>
            <div className="stat-label">이용자 평균 만족도</div>
          </div>
        </div>
      </section>

      {/* ===== REVIEWS ===== */}
      <section className="reviews">
        <div className="section-head">
          <span className="tape">실제 후기</span>
          <h2>먼저 준비한 분들의 이야기</h2>
        </div>
        <div className="review-grid">
          <div className="review-card">
            <div className="review-stars">
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
            </div>
            <p className="review-text">
              예산이랑 결혼식 날짜만 입력했는데 일정이랑 예산 배분까지 한 번에
              짜주고, 딱 맞는 웨딩홀까지 추천해줬어요. 막막했던 시작이 훨씬
              가벼워졌습니다.
            </p>
            <div className="review-footer">
              <div className="review-avatar">🌸</div>
              <div className="review-author">김O진 님</div>
              <span className="review-tag">웨딩홀 예약</span>
            </div>
          </div>
          <div className="review-card">
            <div className="review-stars">
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
            </div>
            <p className="review-text">
              체크리스트 덕분에 뭘 놓치고 있는지 한눈에 보여서 정말 든든했어요.
              D-day 기준으로 알림까지 와서 계약금 미납 같은 실수를 막을 수
              있었어요.
            </p>
            <div className="review-footer">
              <div className="review-avatar">🌿</div>
              <div className="review-author">박O영 님</div>
              <span className="review-tag">준비관리 이용</span>
            </div>
          </div>
          <div className="review-card">
            <div className="review-stars">
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
              <span className="star">★</span>
            </div>
            <p className="review-text">
              답례품 고르는 것도 일이었는데 종류가 많고 후기도 자세해서 고민
              없이 골랐어요. 하객분들 반응도 너무 좋았고 배송까지 빨랐습니다.
            </p>
            <div className="review-footer">
              <div className="review-avatar">🕊️</div>
              <div className="review-author">이O희 님</div>
              <span className="review-tag">답례품 쇼핑몰</span>
            </div>
          </div>
        </div>
      </section>

      {/* ===== FOOTER ===== */}
      <footer>
        <div className="footer-inner">
          <div className="footer-top">
            <div>
              <div className="footer-logo">🤍 웨딩올인원</div>
              <p className="footer-desc">
                결혼 준비의 모든 순간을
                <br />
                함께하겠습니다.
                <br />
                <br />
                wedding all in one
              </p>
            </div>
            <div className="footer-nav-cols">
              <div className="footer-nav-group">
                <h4>서비스</h4>
                <ul>
                  <li>
                    <Link to={companyListPath}>업체 둘러보기</Link>
                  </li>
                  <li>
                    <a href="#">AI 웨딩플랜</a>
                  </li>
                  <li>
                    <Link to="/prep/hub">준비관리</Link>
                  </li>
                  <li>
                    <Link to="/product/">답례품 쇼핑몰</Link>
                  </li>
                </ul>
              </div>
              <div className="footer-nav-group">
                <h4>커뮤니티</h4>
                <ul>
                  <li>
                    <Link to="/board/free">자유게시판</Link>
                  </li>
                  <li>
                    <Link to="/board/review">웨딩 후기</Link>
                  </li>
                </ul>
              </div>
              <div className="footer-nav-group">
                <h4>회사</h4>
                <ul>
                  <li>
                    <a href="#">회사소개</a>
                  </li>
                  <li>
                    <a href="#">이용약관</a>
                  </li>
                  <li>
                    <a href="#">개인정보처리방침</a>
                  </li>
                  <li>
                    <a href="#">고객센터</a>
                  </li>
                </ul>
              </div>
            </div>
            <div className="footer-sns">
              <p>우리를 팔로우해요</p>
              <div className="sns-row">
                <a href="#" className="sns-btn" aria-label="인스타그램">
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <rect x="2" y="2" width="20" height="20" rx="5" />
                    <circle cx="12" cy="12" r="4" />
                    <circle cx="17.5" cy="6.5" r="0.8" fill="currentColor" />
                  </svg>
                </a>
                <a href="#" className="sns-btn" aria-label="유튜브">
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <rect x="2" y="5" width="20" height="14" rx="3" />
                    <path
                      d="M10 9l5 3-5 3V9z"
                      fill="currentColor"
                      stroke="none"
                    />
                  </svg>
                </a>
                <a href="#" className="sns-btn" aria-label="카카오">
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M12 3C6.5 3 2 6.7 2 11.3c0 2.9 1.8 5.5 4.6 7L5 21l4.4-2.3c.8.2 1.7.3 2.6.3 5.5 0 10-3.7 10-8.3S17.5 3 12 3z" />
                  </svg>
                </a>
                <a href="#" className="sns-btn" aria-label="블로그">
                  <svg
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M4 7h13v8a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V7z" />
                    <path d="M17 8h2a2 2 0 0 1 0 4h-2" />
                  </svg>
                </a>
              </div>
            </div>
          </div>
          <div className="footer-bottom">
            <div className="footer-info">
              <span>(주)웨딩올인원</span>
              <span>대표 홍길동</span>
              <span>사업자등록번호 261-81-00000</span>
              <br />
              <span>서울특별시 강남구 테헤란로 123 올인원빌딩 5층</span>
              <span>대표전화 1588-0000</span>
              <span>이메일 hello@weddingooi.com</span>
            </div>
            <div className="footer-copy">
              © 2026 웨딩올인원. All rights reserved.
            </div>
          </div>
        </div>
      </footer>

      <style>{`
        /* ===== HERO ===== */
        .hero-wrapper {
          background-image: url('/hero-bg.jpg');
          background-size: cover;
          background-position: center 55%;
          background-repeat: no-repeat;
          position: relative;
          overflow: hidden;
        }
        /* 사진 위 핑크-화이트 오버레이 */
        .hero-wrapper::before {
          content: '';
          position: absolute;
          inset: 0;
          background:
            linear-gradient(
              135deg,
              rgba(251,239,239,0.82) 0%,
              rgba(255,247,247,0.70) 35%,
              rgba(237,224,245,0.60) 70%,
              rgba(255,255,255,0.55) 100%
            );
          backdrop-filter: blur(2px);
          pointer-events: none;
        }
        .hero {
          padding: 130px 60px 110px;
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 60px;
          align-items: center;
          max-width: 1340px;
          margin: 0 auto;
          min-height: 100vh;
          position: relative;
          z-index: 1;
        }
        @keyframes heroFadeUp {
          from { opacity: 0; transform: translateY(16px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        .hero-slide-content { animation: heroFadeUp 0.6s ease forwards; }
        .hero-eyebrow {
          display: inline-block;
          font-family: 'Gaegu', cursive;
          font-size: 13px;
          color: #8A8070;
          letter-spacing: 0.12em;
          margin-bottom: 20px;
        }
        .hero-copy h1 {
          font-family: 'Gowun Batang', serif;
          font-size: 46px;
          line-height: 1.45;
          color: #3A362F;
          margin-bottom: 22px;
        }
        .hero-copy p {
          font-size: 15px;
          color: #7A7364;
          margin-bottom: 36px;
          line-height: 1.85;
        }
        .hero-cta-link {
          display: inline-flex;
          align-items: center;
          gap: 8px;
          background: rgba(255,255,255,0.82);
          color: #C06080;
          padding: 13px 28px;
          border-radius: 100px;
          text-decoration: none;
          font-size: 14px;
          font-weight: 400;
          border: 1.5px solid rgba(245,203,203,0.7);
          backdrop-filter: blur(6px);
          transition: background 0.2s, border-color 0.2s, transform 0.2s, box-shadow 0.2s;
          box-shadow: 0 4px 16px rgba(197,179,211,0.25);
        }
        .hero-cta-link:hover {
          background: rgba(255,255,255,0.96);
          border-color: #C5B3D3;
          transform: translateY(-2px);
          box-shadow: 0 8px 24px rgba(197,179,211,0.35);
        }
        .hero-dots {
          display: flex;
          gap: 8px;
          margin-top: 36px;
          align-items: center;
        }
        .hero-dot {
          height: 7px;
          border-radius: 100px;
          border: none;
          cursor: pointer;
          transition: width 0.3s ease, background 0.3s ease;
          background: rgba(192,96,128,0.28);
          width: 7px;
          padding: 0;
        }
        .hero-dot:hover { background: rgba(192,96,128,0.5); }
        .hero-dot.active { width: 24px; background: #C06080; box-shadow: 0 2px 6px rgba(192,96,128,0.4); }

        /* ===== 폴라로이드 스택 (1.3x) ===== */
        .polaroid-stack {
          position: relative;
          height: 703px;
          width: 100%;
        }
        .polaroid {
          position: absolute;
          width: 384px;
          background: #FFFDF9;
          padding: 20px 20px 70px;
          box-shadow: 0 24px 52px -10px rgba(58,54,47,0.28);
          border-radius: 2px;
          cursor: pointer;
          transition: transform 0.38s cubic-bezier(0.34,1.56,0.64,1), box-shadow 0.38s ease;
        }
        .polaroid:hover { box-shadow: 0 40px 80px -8px rgba(58,54,47,0.36); z-index: 10 !important; }
        .polaroid::before {
          content: '';
          position: absolute;
          top: -12px; left: 50%;
          transform: translateX(-50%);
          width: 62px; height: 22px;
          background: rgba(197,179,211,0.65);
          border-radius: 2px;
        }
        .polaroid .photo {
          height: 296px;
          border-radius: 1px;
          overflow: hidden;
          position: relative;
        }
        .polaroid .photo img { width: 100%; height: 100%; object-fit: cover; filter: saturate(0.85) brightness(0.97); }
        .polaroid .photo::after {
          content: '';
          position: absolute;
          inset: 0;
          background: linear-gradient(to bottom, rgba(255,240,220,0.18), rgba(200,180,160,0.08));
          mix-blend-mode: multiply;
          pointer-events: none;
        }
        .polaroid .cap {
          font-family: 'Gaegu', cursive;
          font-size: 19px;
          text-align: center;
          margin-top: 16px;
          color: #4A4638;
          letter-spacing: 0.02em;
        }
        .p1 { top: 12px; left: 0px;   transform: rotate(-7deg);  z-index: 3; }
        .p1:hover { transform: rotate(-3deg) translateY(-22px) scale(1.04); }
        .p1 .photo { background: linear-gradient(145deg,#FBEFEF 0%,#FFE2E2 40%,#F5CBCB 70%,#E8A8A8 100%); }
        .p2 { top: 90px; left: 240px; transform: rotate(5deg);   z-index: 2; }
        .p2:hover { transform: rotate(1deg) translateY(-22px) scale(1.04); }
        .p2 .photo { background: linear-gradient(145deg,#EDE0F5 0%,#DDD3E8 40%,#C5B3D3 70%,#A890C0 100%); }
        .p3 { top: 290px; left: 90px; transform: rotate(-2.5deg); z-index: 1; }
        .p3:hover { transform: rotate(0.5deg) translateY(-22px) scale(1.04); }
        .p3 .photo { background: linear-gradient(145deg,#FFE2E2 0%,#F5CBCB 40%,#EDB8B8 70%,#E0A0A8 100%); }
        .photo-placeholder {
          position: absolute;
          inset: 0;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 10px;
        }
        .photo-placeholder span:first-child { font-size: 40px; }
        .ph-label {
          font-family: 'Gowun Batang', serif;
          font-size: 12px;
          color: rgba(58,54,47,0.45);
          letter-spacing: 0.14em;
          text-transform: uppercase;
        }

        /* ===== 핑크 보드 패널 ===== */
        .member-widget {
          position: relative;
          height: 580px;
          width: 100%;
        }
        /* 연핑크 반투명 보드 */
        .member-widget::before {
          content: '';
          position: absolute;
          inset: -34px -62px;
          border-radius: 24px;
          background: linear-gradient(
            145deg,
            rgba(255,239,239,0.40) 0%,
            rgba(255,226,226,0.40) 50%,
            rgba(255,239,239,0.40) 100%
          );
          backdrop-filter: blur(10px);
          -webkit-backdrop-filter: blur(10px);
          box-shadow:
            0 20px 60px rgba(100,60,80,0.14),
            0 8px 24px rgba(100,60,80,0.09),
            inset 0 1px 0 rgba(255,255,255,0.7),
            inset 0 -1px 0 rgba(245,203,203,0.2);
          border: 1px solid rgba(255,255,255,0.55);
          z-index: 0;
        }

        .w-card {
          position: absolute;
          display: block;
          border-radius: 16px;
          background: #FFFDF9;
          box-shadow: 0 6px 20px -4px rgba(150,120,180,0.22), 0 2px 6px rgba(150,120,180,0.1);
          transition: transform 0.3s ease, box-shadow 0.3s ease;
          z-index: 1;
          color: inherit;
          text-decoration: none;
          cursor: pointer;
        }
        .w-card:hover { transform: translateY(-8px); box-shadow: 0 18px 44px -6px rgba(150,120,180,0.3); z-index: 10 !important; }

        /* D-day 카드 — z-index 1 (가장 뒤) */
        .wc-dday {
          top: 0; left: 0;
          width: 334px;
          padding: 38px 31px 31px;
          transform: rotate(-5deg);
          z-index: 1;
          background: #FFFEF8;
          box-shadow: 0 6px 20px -4px rgba(80,45,10,0.28), 2px 2px 0 rgba(0,0,0,0.06);
        }
        .wc-dday:hover { transform: rotate(-2deg) translateY(-10px); }
        /* D-day 테이프 */
        .wc-dday::before {
          content: '';
          position: absolute;
          top: -13px; left: 50%;
          transform: translateX(-50%) rotate(-1deg);
          width: 72px; height: 22px;
          background: rgba(255,226,226,0.85);
          border-radius: 2px;
          box-shadow: 0 1px 3px rgba(80,45,10,0.15);
        }
        .dday-pin { width: 18px; height: 18px; background: #C87070; border-radius: 50%; margin: 0 auto 18px; box-shadow: 0 2px 8px rgba(200,112,112,0.5); }
        .dday-num { font-family: 'Gowun Batang', serif; font-size: 48px; color: #3A362F; font-weight: 700; line-height: 1; margin-bottom: 14px; }
        .dday-note { font-family: 'Gaegu', cursive; font-size: 18px; color: #6A6458; line-height: 1.7; }
        .dday-divider { border: none; border-top: 1px dashed #E0D8CC; margin: 19px 0; }
        .dday-couple { font-family: 'Gowun Batang', serif; font-size: 17px; color: #5A5448; text-align: center; letter-spacing: 0.02em; }
        .dday-couple .dday-heart { color: #C87070; margin: 0 8px; font-family: initial; }
        .dday-date { margin-top: 8px; font-family: 'Gowun Batang', serif; font-size: 12.5px; color: #B08A72; text-align: center; letter-spacing: 0.1em; }
        .dday-date-deco { color: #E8A8A8; font-size: 9px; margin: 0 7px; vertical-align: middle; }
        .dday-couple-link { display: inline-block; font-size: 14px; color: #C06080; text-decoration: none; border-bottom: 1px dashed #E8A8A8; transition: color 0.2s, border-color 0.2s; }
        .dday-couple-link:hover { color: #A34860; border-color: #C06080; }

        /* 취향 폴라로이드 — z-index 2 (중간) */
        .wc-taste {
          top: 160px; right: 0;
          width: 312px;
          min-height: 246px;
          padding: 17px 17px 62px;
          transform: rotate(4deg);
          z-index: 2;
          background: #FFFDF9;
          box-shadow: 0 6px 20px -4px rgba(80,45,10,0.28), 2px 2px 0 rgba(0,0,0,0.06);
          display: flex;
          flex-direction: column;
          justify-content: center;
        }
        .wc-taste:hover { transform: rotate(1deg) translateY(-10px); }
        .wc-taste::before { content: ''; position: absolute; top: -12px; left: 50%; transform: translateX(-50%); width: 55px; height: 19px; background: rgba(197,179,211,0.8); border-radius: 2px; box-shadow: 0 1px 3px rgba(80,45,10,0.15); }
        .wc-taste .visit-title { font-family: 'Gaegu', cursive; font-size: 17px; text-align: center; color: #5A5448; margin-bottom: 18px; }
        .visit-list { display: flex; flex-direction: column; gap: 12px; }
        .visit-row { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; padding-bottom: 12px; border-bottom: 1px dashed #E0D8CC; }
        .visit-row:last-child { border-bottom: none; padding-bottom: 0; }
        .visit-name { flex: 1; min-width: 0; font-size: 14px; color: #4A4638; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .visit-category-icon { margin-left: 5px; font-size: 13px; }
        .visit-dday { flex-shrink: 0; font-family: 'Gowun Batang', serif; font-size: 13px; font-weight: 700; color: #C06080; white-space: nowrap; }
        .visit-empty { font-size: 13px; color: #A8A090; text-align: center; line-height: 1.6; }

        /* AI 매칭 카드 — z-index 3 (가장 앞) */
        .wc-ai {
          top: 340px; left: 20px;
          width: 432px;
          padding: 34px 29px 26px;
          transform: rotate(-1.5deg);
          z-index: 3;
          background: #FFFDF9;
          box-shadow: 0 6px 20px -4px rgba(80,45,10,0.28), 2px 2px 0 rgba(0,0,0,0.06);
        }
        .wc-ai:hover { transform: rotate(0deg) translateY(-8px); }
        /* AI 카드 테이프 */
        .wc-ai::before {
          content: '';
          position: absolute;
          top: -13px; left: 32px;
          transform: rotate(1.5deg);
          width: 68px; height: 22px;
          background: rgba(245,203,203,0.85);
          border-radius: 2px;
          box-shadow: 0 1px 3px rgba(80,45,10,0.15);
        }
        .ai-label { display: flex; align-items: center; gap: 8px; font-size: 15px; color: #7A7364; margin-bottom: 22px; font-weight: 500; }
        .ai-dot { width: 9px; height: 9px; border-radius: 50%; background: #F5CBCB; animation: pulse 1.8s ease-in-out infinite; flex-shrink: 0; }
        @keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.5;transform:scale(0.8)} }
        .ai-empty { font-size: 13px; color: #A8A090; line-height: 1.6; }
        .budget-summary { display: flex; flex-direction: column; gap: 10px; }
        .budget-bar-track { height: 12px; background: #F7EDED; border-radius: 100px; overflow: hidden; }
        .budget-bar-fill { height: 100%; border-radius: 100px; background: linear-gradient(to right,#F5CBCB,#C06080); transition: width 1.2s ease; }
        .budget-bar-caption { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
        .budget-percent { font-family: 'Gowun Batang', serif; font-size: 13px; font-weight: 700; color: #C06080; }
        .budget-total { font-size: 12px; color: #A8A090; }
        .budget-row { display: flex; align-items: center; justify-content: space-between; padding-top: 6px; border-top: 1px dashed #E0D8CC; }
        .budget-row-label { font-size: 13px; color: #A8A090; }
        .budget-row-value { font-size: 14px; font-weight: 600; color: #4A4638; }
        .budget-row-value.over { color: #C0405F; }

        /* ===== FEATURES ===== */
        .features { padding: 80px 60px 100px; max-width: 1180px; margin: 0 auto; }
        .section-head { margin-bottom: 52px; }
        .section-head .tape { display: inline-block; background: #FFE2E2; color: #C06080; font-family: 'Gaegu', cursive; font-size: 13px; padding: 4px 14px; transform: rotate(-2deg); margin-bottom: 14px; }
        .section-head h2 { font-family: 'Gowun Batang', serif; font-size: 30px; color: #3A362F; }
        .feat-row { display: grid; grid-template-columns: repeat(3,1fr); gap: 24px; }
        .feat-card { display: block; background: #fff; color: inherit; text-decoration: none; cursor: pointer; border-radius: 18px; padding: 36px 28px; box-shadow: 0 8px 24px -12px rgba(245,203,203,0.4); transition: transform 0.25s ease, box-shadow 0.25s ease; position: relative; overflow: hidden; }
        .feat-card::before { content: ''; position: absolute; bottom: 0; right: 0; width: 80px; height: 80px; border-radius: 50%; opacity: 0.18; transition: transform 0.3s ease; }
        .feat-card:nth-child(1)::before { background: #FFE2E2; transform: translate(20px,20px); }
        .feat-card:nth-child(2)::before { background: #C5B3D3; transform: translate(20px,20px); }
        .feat-card:nth-child(3)::before { background: #F5CBCB; transform: translate(20px,20px); }
        .feat-card:hover { transform: translateY(-6px); box-shadow: 0 18px 40px -12px rgba(245,203,203,0.5); }
        .feat-card:hover::before { transform: translate(10px,10px) scale(1.3); }
        .feat-icon { width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; margin-bottom: 20px; }
        .feat-card:nth-child(1) .feat-icon { background: #FBEFEF; }
        .feat-card:nth-child(2) .feat-icon { background: #EDE0F5; }
        .feat-card:nth-child(3) .feat-icon { background: #FFE2E2; }
        .feat-card h3 { font-family: 'Gowun Batang', serif; font-size: 19px; margin-bottom: 12px; }
        .feat-card p { font-size: 13.5px; color: #7A7364; line-height: 1.7; }

        /* ===== TRUST ===== */
        .trust { background: linear-gradient(135deg, #FBEFEF 0%, #FFE2E2 50%, #EDE0F5 100%); padding: 80px 60px; text-align: center; }
        .trust .tape { display: inline-block; background: #fff; color: #C5B3D3; font-family: 'Gaegu', cursive; font-size: 13px; padding: 4px 14px; transform: rotate(1deg); margin-bottom: 16px; }
        .trust h2 { font-family: 'Gowun Batang', serif; font-size: 28px; margin-bottom: 48px; }
        .trust-stats { display: flex; justify-content: center; gap: 72px; }
        .stat-num { font-family: 'Gowun Batang', serif; font-size: 38px; color: #C06080; }
        .stat-label { font-size: 13px; color: #8A8373; margin-top: 8px; }

        /* ===== REVIEWS ===== */
        .reviews { padding: 100px 60px; max-width: 1200px; margin: 0 auto; }
        .reviews .section-head .tape { background: #FFE2E2; color: #C06080; }
        .review-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 24px; }
        .review-card { background: #fff; border-radius: 2px; padding: 28px 24px 24px; box-shadow: 0 8px 24px -10px rgba(245,203,203,0.45); position: relative; transition: transform 0.25s ease, box-shadow 0.25s ease; }
        .review-card:hover { transform: translateY(-5px) rotate(0.3deg); box-shadow: 0 20px 40px -10px rgba(245,203,203,0.6); }
        .review-card::before { content: ''; position: absolute; top: -9px; left: 28px; width: 44px; height: 16px; border-radius: 2px; }
        .review-card:nth-child(1)::before { background: rgba(255,226,226,0.85); transform: rotate(-2deg); }
        .review-card:nth-child(2)::before { background: rgba(197,179,211,0.75); transform: rotate(1.5deg); }
        .review-card:nth-child(3)::before { background: rgba(245,203,203,0.8); transform: rotate(-1deg); }
        .review-stars { display: flex; gap: 3px; margin-bottom: 14px; }
        .star { color: #C9A96A; font-size: 16px; }
        .review-text { font-size: 13.5px; color: #4A4638; line-height: 1.8; margin-bottom: 18px; font-style: italic; position: relative; padding-left: 14px; }
        .review-text::before { content: '"'; position: absolute; left: 0; top: -2px; font-family: 'Gowun Batang', serif; font-size: 28px; color: #F5CBCB; line-height: 1; }
        .review-footer { display: flex; align-items: center; gap: 10px; padding-top: 14px; border-top: 1px dashed #F0DEDE; }
        .review-avatar { width: 34px; height: 34px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
        .review-card:nth-child(1) .review-avatar { background: #FBEFEF; }
        .review-card:nth-child(2) .review-avatar { background: #EDE0F5; }
        .review-card:nth-child(3) .review-avatar { background: #FFE2E2; }
        .review-author { font-family: 'Gaegu', cursive; font-size: 14px; color: #C5B3D3; }
        .review-tag { margin-left: auto; font-size: 11px; color: #A8A090; background: #FBEFEF; padding: 2px 8px; border-radius: 100px; }

        /* ===== FOOTER ===== */
        footer { background: #2E2B25; color: #A8A090; padding: 56px 60px 36px; }
        .footer-inner { max-width: 1180px; margin: 0 auto; }
        .footer-top { display: grid; grid-template-columns: 240px 1fr auto; gap: 48px; align-items: flex-start; padding-bottom: 36px; border-bottom: 1px solid rgba(255,255,255,0.06); margin-bottom: 28px; }
        .footer-logo { font-family: 'Gowun Batang', serif; font-size: 20px; color: #EDE8DF; margin-bottom: 10px; }
        .footer-desc { font-size: 12.5px; color: #7A7668; line-height: 1.7; }
        .footer-nav-group h4 { font-size: 11px; letter-spacing: 0.12em; color: #6B6860; text-transform: uppercase; margin-bottom: 14px; }
        .footer-nav-group ul { list-style: none; display: flex; flex-direction: column; gap: 10px; }
        .footer-nav-group a { font-size: 13px; color: #8A8278; text-decoration: none; transition: color 0.2s; }
        .footer-nav-group a:hover { color: #EDE8DF; }
        .footer-nav-cols { display: flex; gap: 48px; }
        .footer-sns { display: flex; flex-direction: column; gap: 12px; align-items: flex-end; }
        .footer-sns p { font-family: 'Gaegu', cursive; font-size: 14px; color: #F5CBCB; margin-bottom: 4px; }
        .sns-row { display: flex; gap: 10px; }
        .sns-btn { width: 36px; height: 36px; border-radius: 50%; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.08); display: flex; align-items: center; justify-content: center; color: #7A7668; text-decoration: none; transition: background 0.2s, color 0.2s; }
        .sns-btn:hover { background: #C5B3D3; color: #fff; }
        .footer-bottom { display: flex; justify-content: space-between; align-items: flex-end; gap: 24px; flex-wrap: wrap; }
        .footer-info { font-size: 12px; color: #5C5A54; line-height: 1.8; }
        .footer-info span { margin-right: 12px; }
        .footer-copy { font-size: 11.5px; color: #4A4840; white-space: nowrap; }

        /* ===== RESPONSIVE ===== */

        /* ── 1100px: 좁은 데스크톱 ── */
        @media (max-width: 1100px) {
          .hero { padding: 120px 32px 80px; gap: 40px; }
          .hero-copy h1 { font-size: 38px; }
          .polaroid { width: 320px; }
          .polaroid .photo { height: 245px; }
          .polaroid-stack { height: 590px; }
          .member-widget { height: 530px; }
          .p2 { left: 200px; }
          .p3 { top: 260px; left: 75px; }
          .features, .reviews { padding: 80px 32px; }
          .trust { padding: 60px 32px; }
          footer { padding: 48px 32px 28px; }
          .footer-top { grid-template-columns: 200px 1fr; }
          .footer-sns { display: none; }
        }

        /* ── 820px: 햄버거 + 히어로 1컬럼 동시 전환 ── */
        @media (max-width: 820px) {
          .hero {
            grid-template-columns: 1fr;
            padding: 88px 24px 64px;
            text-align: center;
            min-height: auto;
            gap: 52px;
          }
          .hero-copy h1 { font-size: 32px; }
          .hero-copy p  { font-size: 14px; }
          .hero-dots { justify-content: center; }

          /* 폴라로이드 스택 */
          .polaroid-stack {
            height: 420px;
            width: 360px;
            margin: 0 auto;
          }
          .polaroid { width: 215px; padding: 13px 13px 48px; }
          .polaroid .photo { height: 165px; }
          .polaroid .cap { font-size: 14px; }
          .p1 { top: 10px; left: 0px; }
          .p2 { top: 65px; left: 145px; }
          .p3 { top: 185px; left: 65px; }

          /* 회원 위젯 */
          .member-widget {
            height: 510px;
            width: 400px;
            margin: 0 auto;
          }
          .member-widget::before { inset: -18px -28px; border-radius: 16px; }
          .wc-dday  { width: 218px; top: 0; left: 0; }
          .wc-dday .dday-num  { font-size: 36px; }
          .wc-dday .dday-note { font-size: 13px; }
          .wc-taste { width: 208px; top: 130px; right: 0; min-height: 162px; }
          .wc-ai    { width: 340px; left: 10px; top: 278px; }

          /* 기타 섹션 */
          .feat-row { grid-template-columns: 1fr; }
          .trust-stats { gap: 36px; flex-wrap: wrap; justify-content: center; }
          .review-grid { grid-template-columns: 1fr; }
          .features, .reviews { padding: 60px 20px; }
          .trust { padding: 60px 20px; }
          footer { padding: 40px 20px 24px; }
          .footer-top { grid-template-columns: 1fr; gap: 32px; }
          .footer-nav-cols { flex-wrap: wrap; gap: 28px; }
          .footer-bottom { flex-direction: column; align-items: flex-start; gap: 10px; }
        }

        /* ── 480px: 작은 모바일 ── */
        @media (max-width: 480px) {
          .hero { padding: 80px 16px 52px; }
          .hero-copy h1 { font-size: 27px; }
          .polaroid-stack { width: 300px; height: 360px; }
          .polaroid { width: 178px; padding: 11px 11px 40px; }
          .polaroid .photo { height: 138px; }
          .p2 { left: 120px; }
          .p3 { top: 158px; left: 54px; }
          .member-widget { width: 340px; height: 480px; }
          .member-widget::before { inset: -14px -20px; border-radius: 14px; }
          .wc-dday  { width: 188px; }
          .wc-taste { width: 178px; min-height: 138px; }
          .wc-ai    { width: 290px; top: 260px; left: 8px; }
        }
      `}</style>
    </>
  );
};

export default MainPage;
