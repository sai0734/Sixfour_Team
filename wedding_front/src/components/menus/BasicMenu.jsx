import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import useCustomLogin from "../../hooks/useCustomLogin";

// 사이트 전체에서 공용으로 쓰는 상단바. 홈(히어로 있는 페이지)에서는 스크롤에 따라
// 투명 -> 흰 배경으로 전환되고, 히어로가 없는 다른 페이지(커뮤니티/준비관리 등)에서는
// 처음부터 흰 배경(scrolled) 상태로 고정됩니다.
const BasicMenu = () => {
  const { loginState } = useCustomLogin();
  const location = useLocation();
  const [scrolled, setScrolled] = useState(true);

  useEffect(() => {
    const heroEl = document.querySelector(".hero");

    if (!heroEl) {
      setScrolled(true);
      return;
    }

    setScrolled(false);

    const onScroll = () => {
      const threshold = heroEl.offsetHeight - 80;
      setScrolled(window.scrollY > threshold);
    };

    window.addEventListener("scroll", onScroll, { passive: true });
    onScroll();

    return () => window.removeEventListener("scroll", onScroll);
  }, [location.pathname]);

  const isActive = (prefix) =>
    prefix === "/"
      ? location.pathname === "/"
      : location.pathname.startsWith(prefix);

  return (
    <>
      <header className={scrolled ? "scrolled" : ""}>
        <div className="header-inner">
          <Link to="/" className="logo">
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
              <path d="M19.5 12.572 12 20l-7.5-7.428a5 5 0 1 1 7.5-6.566 5 5 0 1 1 7.5 6.566Z" />
            </svg>
            <span>웨딩올인원</span>
          </Link>
          <nav className="nav">
            <Link to="/" className={isActive("/") ? "active" : ""}>
              홈
            </Link>
            <a href="#">AI 웨딩플랜</a>
            <Link
              to="/product/"
              className={isActive("/product") ? "active" : ""}
            >
              답례품 쇼핑몰
            </Link>
            <Link
              to="/board/list"
              className={isActive("/board") ? "active" : ""}
            >
              커뮤니티
            </Link>
            <a href="#">준비관리</a>
          </nav>
          <div className="header-right">
            {!loginState.email ? (
              <Link to="/auth/login" className="login">
                로그인
              </Link>
            ) : (
              <Link to="/auth/logout" className="login">
                로그아웃
              </Link>
            )}
            <Link to="/auth/login" className="btn-header">
              시작하기
            </Link>
          </div>
        </div>
      </header>

      <style>{`
/* ===== 헤더 ===== */
  header {
    position: fixed;
    top: 0; left: 0; right: 0;
    z-index: 100;
    transition: background-color 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
    background: transparent;
    border-bottom: 1px solid transparent;
  }
  header.scrolled {
    background: #ffffff;
    border-bottom: 1px solid #E5E3DC;
    box-shadow: 0 1px 0 rgba(0,0,0,0.02);
  }
  .header-inner {
    max-width: 1280px;
    margin: 0 auto;
    padding: 0 32px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
  }
  header.scrolled .header-inner { height: 68px; }
  .logo {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    transition: color 0.25s ease;
  }
  header.scrolled .logo { color: #3D3D3A; }
  .nav {
    display: flex;
    align-items: center;
    gap: 48px;
    font-size: 13px;
    font-weight: 500;
    letter-spacing: 0.02em;
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }
  .nav a {
    position: relative;
    color: rgba(255,255,255,0.9);
    transition: color 0.25s ease;
    padding-bottom: 4px;
  }
  .nav a::after {
    content: '';
    position: absolute;
    left: 0; bottom: 0;
    width: 0;
    height: 1px;
    background: currentColor;
    transition: width 0.2s ease;
  }
  .nav a:hover::after { width: 100%; }
  .nav a:hover { color: #fff; }
  .nav a.active { color: #fff; }
  .nav a.active::after { width: 100%; }
  header.scrolled .nav a { color: #5F5E5A; }
  header.scrolled .nav a:hover { color: #3D3D3A; }
  header.scrolled .nav a.active { color: #D4537E; }
  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;
    font-size: 13px;
  }
  .header-right a.login { color: rgba(255,255,255,0.9); transition: color 0.25s ease; }
  .header-right a.login:hover { color: #fff; }
  header.scrolled .header-right a.login { color: #5F5E5A; }
  header.scrolled .header-right a.login:hover { color: #3D3D3A; }
  .btn-header {
    height: 36px;
    padding: 0 20px;
    border-radius: 999px;
    background: #fff;
    color: #4B1528;
    display: flex;
    align-items: center;
    font-weight: 500;
    transition: background-color 0.25s ease, color 0.25s ease;
  }
  header.scrolled .btn-header {
    background: #D4537E;
    color: #fff;
  }

  
      `}</style>
    </>
  );
};

export default BasicMenu;
