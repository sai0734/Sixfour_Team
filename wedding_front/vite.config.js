import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  cacheDir: ".vite-cache",
  define: {
    global: "globalThis",
  },
  plugins: [
    react({
      include: "**/*.{jsx,js}",
    }),
  ],
  define: {
    global: "globalThis",
  },
  server: {
    port: 3000,
    open: false,
  },
});
