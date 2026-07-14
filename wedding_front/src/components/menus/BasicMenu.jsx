import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import { getMyManagedCompany } from "../../api/companyApi";

const BasicMenu = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const location = useLocation();
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  const isAdmin = loginState.roleNames?.some((r) =>
    ["ADMIN", "ROLE_ADMIN"].includes(r),
  );
  const isLoggedIn = !!loginState.email;
  const companyListPath = isAdmin ? "/admin/companies/list" : "/companies/list";

  const [isManager, setIsManager] = useState(false);

  useEffect(() => {
    if (!isLoggedIn || isAdmin) {
      setIsManager(false);
      return;
    }

    let cancelled = false;

    getMyManagedCompany()
      .then((data) => {
        if (!cancelled) {
          setIsManager(Boolean(data?.isManager));
        }
      })
      .catch(() => {
        if (!cancelled) {
          setIsManager(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [isLoggedIn, isAdmin]);

  const myPageLink = isManager ? "/manager/inquiries" : "/mypage";
  const myPageLabel = isManager ? "업체페이지" : "마이페이지";

  useEffect(() => {
    // .hero 섹션이 없는 페이지(다른 페이지)에선 즉시 scrolled 처리
    const heroEl = document.querySelector(".hero");
    if (!heroEl) {
      setScrolled(true);
      return;
    }
    setScrolled(false);
    const onScroll = () => setScrolled(window.scrollY > 40);
    window.addEventListener("scroll", onScroll, { passive: true });
    onScroll();
    return () => window.removeEventListener("scroll", onScroll);
  }, [location.pathname]);

  useEffect(() => {
    if (!menuOpen) return;
    const handler = (e) => {
      const menuEl = document.getElementById("mobileMenuPanel");
      const btnEl = document.getElementById("hamburgerBtn");
      if (
        menuEl &&
        !menuEl.contains(e.target) &&
        btnEl &&
        !btnEl.contains(e.target)
      ) {
        setMenuOpen(false);
      }
    };
    document.addEventListener("click", handler);
    return () => document.removeEventListener("click", handler);
  }, [menuOpen]);

  useEffect(() => {
    document.body.style.overflow = menuOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [menuOpen]);

  const closeMobileMenu = () => setMenuOpen(false);

  const isActive = (prefix) =>
    prefix === "/"
      ? location.pathname === "/"
      : location.pathname.startsWith(prefix);

  return (
    <>
      {/* 모바일 메뉴 패널 */}
      <div
        id="mobileMenuPanel"
        className={`mobile-menu${menuOpen ? " open" : ""}`}
      >
        <ul>
          <li>
            <Link to="/" onClick={closeMobileMenu}>
              홈
            </Link>
          </li>
          <li>
            <a href="#" onClick={closeMobileMenu}>
              AI 웨딩플랜
            </a>
          </li>
          <li>
            <Link to={companyListPath} onClick={closeMobileMenu}>
              홀 / 스드메
            </Link>
          </li>
          <li>
            <Link to="/prep/hub" onClick={closeMobileMenu}>
              준비관리
            </Link>
          </li>
          <li>
            <Link to="/product/" onClick={closeMobileMenu}>
              답례품
            </Link>
          </li>
          <li>
            <Link to="/board/list" onClick={closeMobileMenu}>
              커뮤니티
            </Link>
          </li>
        </ul>
      </div>

      {/* 네비 */}
      <nav id="mainNav" className={scrolled ? "scrolled" : ""}>
        <Link to="/" className="logo">
          <button
            id="hamburgerBtn"
            className="hamburger-btn"
            aria-label="메뉴 열기"
            onClick={(e) => {
              e.preventDefault();
              setMenuOpen((v) => !v);
            }}
          >
            {menuOpen ? (
              <svg
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="4" y1="4" x2="20" y2="20" />
                <line x1="20" y1="4" x2="4" y2="20" />
              </svg>
            ) : (
              <svg
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
            )}
          </button>
          <span className="logo-heart">🤍</span>
          웨딩올인원
        </Link>

        <ul className="navlinks">
          <li>
            <Link to="/" className={isActive("/") ? "active" : ""}>
              홈
            </Link>
          </li>
          <li>
            <a href="#">AI 웨딩플랜</a>
          </li>
          <li>
            <Link
              to={companyListPath}
              className={
                isActive("/companies") || isActive("/admin/companies")
                  ? "active"
                  : ""
              }
            >
              홀 / 스드메
            </Link>
          </li>
          <li>
            <Link to="/prep/hub" className={isActive("/prep") ? "active" : ""}>
              준비관리
            </Link>
          </li>
          <li>
            <Link
              to="/product/"
              className={isActive("/product") ? "active" : ""}
            >
              답례품
            </Link>
          </li>
          <li>
            <Link
              to="/board/list"
              className={isActive("/board") ? "active" : ""}
            >
              커뮤니티
            </Link>
          </li>
        </ul>

        {!isLoggedIn ? (
          <div className="nav-right">
            <Link to="/cart" className="nav-cart" aria-label="장바구니">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
            </Link>
            <Link to="/auth/join" className="nav-signup">
              회원가입
            </Link>
            <Link to="/auth/login" className="nav-btn">
              로그인
            </Link>
          </div>
        ) : isAdmin ? (
          <div className="nav-right">
            <Link to="/cart" className="nav-cart" aria-label="장바구니">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
            </Link>
            <Link to="/admin" className="nav-mypage">
              관리자페이지
            </Link>
            <Link to="/auth/logout" className="nav-btn">
              로그아웃
            </Link>
          </div>
        ) : (
          <div className="nav-right">
            <Link to="/cart" className="nav-cart" aria-label="장바구니">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
              <span className="cart-badge">0</span>
            </Link>
            <Link to={myPageLink} className="nav-mypage">
              {myPageLabel}
            </Link>
            <Link to="/auth/logout" className="nav-btn">
              로그아웃
            </Link>
          </div>
        )}
      </nav>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Gowun+Batang:wght@400;700&family=Gaegu:wght@400;700&family=Noto+Sans+KR:wght@300;400;500&display=swap');

        * { margin:0; padding:0; box-sizing:border-box; }
        body {
          font-family: 'Noto Sans KR', -apple-system, sans-serif;
          background: #FBF7F7;
          color: #3A362F;
          line-height: 1.6;
          overflow-x: hidden;
        }
        a { text-decoration: none; color: inherit; }

        /* ===== NAV — #mainNav 으로 한정 (다른 nav 태그에 영향 안 줌) ===== */
        #mainNav {
          position: fixed;
          top: 0; left: 0; right: 0;
          z-index: 300;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 20px 52px;
          transition: background 0.3s ease, box-shadow 0.3s ease;
        }
        #mainNav.scrolled {
          background: rgba(255,247,247,0.96);
          backdrop-filter: blur(12px);
          box-shadow: 0 1px 0 rgba(245,203,203,0.3);
        }
        .logo {
          font-family: 'Gowun Batang', serif;
          font-size: 23px;
          color: #C06080;
          display: flex;
          align-items: center;
          gap: 7px;
          text-decoration: none;
          flex-shrink: 0;
        }
        .logo-heart { display: inline; }

        /* 햄버거 버튼 — 데스크톱에서 숨김 */
        .hamburger-btn {
          display: none;
          background: none;
          border: none;
          cursor: pointer;
          padding: 4px;
          color: #C06080;
          align-items: center;
          justify-content: center;
          border-radius: 6px;
          transition: background 0.2s;
        }
        .hamburger-btn:hover { background: rgba(245,203,203,0.2); }

        /* 데스크톱 네비 링크 — flex 중앙 정렬 (absolute 제거) */
        #mainNav .navlinks {
          display: flex;
          gap: 28px;
          font-size: 17.5px;
          list-style: none;
          flex: 1;
          justify-content: center;
          min-width: 0;
          overflow: hidden;
        }
        #mainNav .navlinks a {
          text-decoration: none;
          color: #7A7364;
          transition: color 0.2s;
          position: relative;
          padding-bottom: 3px;
          white-space: nowrap;
          font-weight: 400;
        }
        #mainNav .navlinks a::after {
          content: '';
          position: absolute;
          left: 0; bottom: 0;
          width: 0; height: 1px;
          background: #F5CBCB;
          transition: width 0.2s;
        }
        #mainNav .navlinks a:hover { color: #C06080; }
        #mainNav .navlinks a:hover::after { width: 100%; }
        #mainNav .navlinks a.active { color: #C06080; }
        #mainNav .navlinks a.active::after { width: 100%; }

        /* 우측 버튼 */
        .nav-right {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;
        }
        .nav-signup {
          font-size: 13px;
          color: #3A362F;
          text-decoration: none;
          padding: 8px 18px;
          border-radius: 100px;
          background: #fff;
          border: none;
          font-weight: 400;
          white-space: nowrap;
          display: inline-flex;
          align-items: center;
          cursor: pointer;
          transition: background 0.2s, box-shadow 0.2s;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .nav-signup:hover { background: #F5F5F5; box-shadow: 0 3px 12px rgba(0,0,0,0.15); }
        .nav-btn {
          background: #3A362F;
          color: #FBF7F7;
          padding: 8px 18px;
          border-radius: 100px;
          font-size: 13px;
          font-weight: 400;
          text-decoration: none;
          border: none;
          cursor: pointer;
          font-family: inherit;
          transition: background 0.2s;
          white-space: nowrap;
          display: inline-flex;
          align-items: center;
        }
        .nav-btn:hover { background: #5A5650; }
        .nav-mypage {
          font-size: 13px;
          color: #3A362F;
          text-decoration: none;
          padding: 8px 18px;
          border-radius: 100px;
          background: #fff;
          border: none;
          font-weight: 400;
          white-space: nowrap;
          display: inline-flex;
          align-items: center;
          cursor: pointer;
          transition: background 0.2s, box-shadow 0.2s;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .nav-mypage:hover { background: #F5F5F5; box-shadow: 0 3px 12px rgba(0,0,0,0.15); }
        .nav-cart {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 36px; height: 36px;
          color: #3A362F;
          text-decoration: none;
          border-radius: 50%;
          transition: background 0.2s, color 0.2s;
          position: relative;
        }
        .nav-cart:hover { background: rgba(58,54,47,0.08); color: #3A362F; }
        .cart-badge {
          position: absolute;
          top: 2px; right: 2px;
          width: 14px; height: 14px;
          border-radius: 50%;
          background: #C87070;
          color: #fff;
          font-size: 9px;
          font-weight: 700;
          display: flex;
          align-items: center;
          justify-content: center;
          line-height: 1;
        }

        /* ===== 모바일 메뉴 패널 ===== */
        .mobile-menu {
          display: none;
          position: fixed;
          top: 0; left: 0; right: 0;
          z-index: 299;
          background: rgba(255,247,247,0.98);
          backdrop-filter: blur(14px);
          padding: 88px 28px 36px;
          box-shadow: 0 8px 32px rgba(245,203,203,0.3);
          border-bottom: 1px solid #F0DEDE;
        }
        .mobile-menu.open { display: block; }
        .mobile-menu ul {
          list-style: none;
          display: flex;
          flex-direction: column;
          gap: 4px;
          margin-bottom: 24px;
        }
        .mobile-menu ul a {
          display: block;
          font-family: 'Noto Sans KR', sans-serif;
          font-size: 15px;
          color: #3A362F;
          text-decoration: none;
          padding: 11px 4px;
          border-bottom: 1px solid #F0DEDE;
          transition: color 0.2s, padding-left 0.2s;
          letter-spacing: 0.02em;
          font-weight: 400;
        }
        .mobile-menu ul a:hover { color: #C06080; padding-left: 8px; }

        /* ===== 반응형 ===== */
        @media (max-width: 1100px) {
          #mainNav { padding: 18px 32px; }
          #mainNav .navlinks { gap: 20px; font-size: 16px; }
        }
        @media (max-width: 960px) {
          #mainNav .navlinks { gap: 12px; font-size: 14.5px; }
          .nav-signup { padding: 7px 12px; font-size: 12px; }
          .nav-btn { padding: 7px 14px; font-size: 12px; }
        }
        @media (max-width: 820px) {
          /* 이 구간에서 navlinks 숨기고 햄버거로 전환 */
          #mainNav .navlinks { display: none !important; }
          .hamburger-btn { display: flex; }
          .logo-heart { display: none; }
          #mainNav { padding: 16px 24px; }
        }
        @media (max-width: 768px) {
          #mainNav { padding: 16px 20px; }
        }
      `}</style>
    </>
  );
};

export default BasicMenu;
