import type { Config } from "tailwindcss";
import animate from "tailwindcss-animate";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    container: { center: true, padding: { DEFAULT: "1.5rem", lg: "3rem" }, screens: { "2xl": "1200px" } },
    extend: {
      colors: {
        canvas: "#FAFAFA",
        surface: "#FFFFFF",
        "surface-2": "#F4F4F5",
        "surface-3": "#E4E4E7",
        ink: "#09090B",
        "ink-2": "#52525B",
        "ink-3": "#A1A1AA",
        line: "#E4E4E7",
        primary: { DEFAULT: "#18181B", foreground: "#FAFAFA" },
        accent: { DEFAULT: "#2563EB", soft: "#EFF6FF", border: "#BFDBFE" },
        get: { DEFAULT: "#16A34A", soft: "#F0FDF4", border: "#BBF7D0" },
        post: { DEFAULT: "#2563EB", soft: "#EFF6FF", border: "#BFDBFE" },
        put: { DEFAULT: "#D97706", soft: "#FFFBEB", border: "#FDE68A" },
        del: { DEFAULT: "#DC2626", soft: "#FEF2F2", border: "#FECACA" },
      },
      fontFamily: {
        sans: ["'Inter Variable'", "Inter", "system-ui", "sans-serif"],
        mono: ["'JetBrains Mono Variable'", "'JetBrains Mono'", "ui-monospace", "monospace"],
      },
      borderRadius: { card: "10px", ctl: "6px", tag: "4px" },
      boxShadow: {
        card: "0 1px 2px 0 rgba(0,0,0,0.03)",
        float: "0 10px 30px -10px rgba(0,0,0,0.06), 0 1px 3px 0 rgba(0,0,0,0.04)",
      },
    },
  },
  plugins: [animate],
};

export default config;

