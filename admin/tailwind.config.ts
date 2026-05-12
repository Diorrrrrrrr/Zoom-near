import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: [
          "Pretendard",
          "Pretendard Variable",
          "-apple-system",
          "BlinkMacSystemFont",
          "system-ui",
          "Noto Sans KR",
          "sans-serif",
        ],
      },
      colors: {
        brand: {
          50: "#f0f4ff",
          500: "#4f6ef7",
          900: "#1a2a6c",
        },
        orange: {
          50: "#fff7ed",
          100: "#ffedd5",
          500: "#f97316",
          600: "#ea580c",
          700: "#c2410c",
          800: "#9a3412",
        },
        // ZOOM NEAR 일반 유저 브랜드 팔레트 — 깊은 포레스트 그린, 절제된 프리미엄 톤.
        forest: {
          50: "#f2f7f4",
          100: "#dceae0",
          200: "#b8d2c0",
          300: "#88b395",
          400: "#5a926f",
          500: "#3e7656",
          600: "#2c5a41",
          700: "#1f4332",
          800: "#143126",
          900: "#0c1f18",
        },
      },
      boxShadow: {
        "soft-sm": "0 1px 2px rgba(15, 49, 38, 0.06)",
        "soft-md": "0 6px 18px -8px rgba(15, 49, 38, 0.18)",
      },
      letterSpacing: {
        "tight-1": "-0.01em",
        "tight-2": "-0.02em",
      },
    },
  },
  plugins: [],
};

export default config;
