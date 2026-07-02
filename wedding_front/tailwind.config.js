/** @type{import('tailwindcss').Config}*/
export default {
  content: ["./index.html", "./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: "#D4537E",
          dark: "#B8436A",
          deep: "#4B1528",
          accent: "#993556",
          light: "#FBEAF0",
        },
        ink: {
          DEFAULT: "#3D3D3A",
          soft: "#5F5E5A",
          muted: "#73726C",
          faint: "#A8A6A0",
        },
        line: {
          DEFAULT: "#E5E3DC",
          soft: "#D9D7CD",
        },
        cream: "#F7F6F3",
        surface: "#F1EFE8",
      },
      fontFamily: {
        serif: ["Georgia", "Noto Serif KR", "serif"],
      },
    },
  },
  plugins: [],
};
