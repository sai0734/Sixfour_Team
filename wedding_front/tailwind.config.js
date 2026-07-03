/** @type{import('tailwindcss').Config}*/
export default {
  content: ["./index.html", "./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
<<<<<<< HEAD
      fontFamily: {
        display: ["Fraunces", "serif"],
        body: ["Plus Jakarta Sans", "sans-serif"],
        serif: ["Georgia", "Noto Serif KR", "serif"],
      },
      colors: {
        blush: {
          50: "#FFF5F7",
          100: "#FFE4EC",
        },
        rose: {
          400: "#FF7EB3",
          500: "#FF5B9E",
          600: "#E14A87",
          700: "#C44569",
        },
        plum: {
          900: "#2D1B24",
          500: "#8B6B78",
        },
=======
      colors: {
>>>>>>> b653b5515216255cfb32ac5885525e600d705d8d
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
<<<<<<< HEAD
      backgroundImage: {
        "rose-gradient":
          "linear-gradient(135deg, #FF7EB3 0%, #FF5B9E 45%, #C44569 100%)",
=======
      fontFamily: {
        serif: ["Georgia", "Noto Serif KR", "serif"],
>>>>>>> b653b5515216255cfb32ac5885525e600d705d8d
      },
    },
  },
  plugins: [],
};
