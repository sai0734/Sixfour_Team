/** @type{import('tailwindcss').Config}*/
export default {
  content: ["./index.html", "./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ["Fraunces", "serif"],
        body: ["Plus Jakarta Sans", "sans-serif"],
        serif: ["Georgia", "Noto Serif KR", "serif"],
        // 메인 화면 컨셉(스크랩북 무드) 전용 - 로그인/가입 등 auth 화면에서 사용
        serifkr: ["Gowun Batang", "serif"],
        handwrite: ["Gaegu", "cursive"],
      },
      colors: {
        // ── 팔레트 기반 색상 ──────────────────────────────
        // #FBEFEF / #FFE2E2 / #F5CBCB / #C5B3D3
        blush: {
          50: "#FBEFEF", // 가장 연한 핑크 (배경, 섹션 배경)
          100: "#FFE2E2", // 연한 핑크 (카드 배경, hover)
          200: "#F5CBCB", // 중간 핑크 (버튼, 포인트)
          300: "#EDB8B8", // 살짝 더 진한 핑크 (border, badge)
        },
        lavender: {
          DEFAULT: "#C5B3D3", // 라벤더/퍼플 (포인트 컬러)
          light: "#DDD3E8", // 연한 라벤더 (배경, hover)
          dark: "#9E88B8", // 진한 라벤더 (텍스트, 버튼)
        },

        // ── 브랜드 메인 색상 (팔레트 기반으로 업데이트) ──
        brand: {
          DEFAULT: "#F5CBCB", // 메인 핑크
          dark: "#E8A8A8", // 버튼 hover, 강조
          deep: "#C06080", // 진한 강조 (제목, 링크)
          accent: "#C5B3D3", // 라벤더 포인트
          light: "#FBEFEF", // 가장 연한 배경
        },

        // ── 텍스트 색상 (기존 유지) ────────────────────────
        ink: {
          DEFAULT: "#3D3D3A",
          soft: "#5F5E5A",
          muted: "#73726C",
          faint: "#A8A6A0",
        },

        // ── 선/구분선 ──────────────────────────────────────
        line: {
          DEFAULT: "#F0DEDE", // 핑크 톤 구분선
          soft: "#F7EDED", // 연한 구분선
        },

        // ── 배경 ───────────────────────────────────────────
        cream: "#FBF7F7", // 핑크 톤 크림 배경
        surface: "#FFF4F4", // 섹션 배경
      },

      // ── 그라데이션 ─────────────────────────────────────
      backgroundImage: {
        // 메인 버튼 그라데이션
        "brand-gradient":
          "linear-gradient(135deg, #FFE2E2 0%, #F5CBCB 50%, #E8A8A8 100%)",
        // 라벤더 포인트 그라데이션
        "lavender-gradient":
          "linear-gradient(135deg, #DDD3E8 0%, #C5B3D3 100%)",
        // 핑크-라벤더 혼합 (히어로, 섹션 배경용)
        "blush-lavender":
          "linear-gradient(135deg, #FBEFEF 0%, #FFE2E2 40%, #E8D8F0 80%, #C5B3D3 100%)",
        // 기존 유지
        "rose-gradient":
          "linear-gradient(135deg, #FFE2E2 0%, #F5CBCB 45%, #E8A8A8 100%)",
      },
    },
  },
  plugins: [],
};
