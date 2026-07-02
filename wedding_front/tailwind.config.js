/** @type{import('tailwindcss').Config}*/
export default {
  content: ["./index.html", "./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ["Fraunces", "serif"],
        body: ["Plus Jakarta Sans", "sans-serif"],
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
      },
      backgroundImage: {
        "rose-gradient":
          "linear-gradient(135deg, #FF7EB3 0%, #FF5B9E 45%, #C44569 100%)",
      },
    },
  },
  plugins: [],
};
