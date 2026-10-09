import animate from "tailwindcss-animate"

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{js,jsx,css}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Public Sans", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["Bricolage Grotesque", "Public Sans", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      fontSize: {
        display: ["var(--text-display)", { lineHeight: "var(--leading-display)", letterSpacing: "var(--tracking-display)" }],
        title: ["var(--text-title)", { lineHeight: "var(--leading-title)", letterSpacing: "var(--tracking-title)" }],
        heading: ["var(--text-heading)", { lineHeight: "var(--leading-heading)", letterSpacing: "var(--tracking-heading)" }],
      },
      lineHeight: {
        reading: "var(--leading-reading)",
      },
      // `shadow` is the resting card shadow, `shadow-raised` the floating one. No `card` key: `shadow-card` already
      // means "shadow colour: card".
      boxShadow: {
        DEFAULT: "var(--shadow-card)",
        raised: "var(--shadow-raised)",
      },
      colors: {
        surface: {
          raised: "hsl(var(--surface-raised))",
        },
        paper: {
          DEFAULT: "hsl(var(--paper))",
          foreground: "hsl(var(--paper-foreground))",
          line: "hsl(var(--paper-line))",
          mark: "hsl(var(--paper-mark))",
        },
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        success: {
          DEFAULT: "hsl(var(--success))",
          foreground: "hsl(var(--success-foreground))",
        },
        warning: {
          DEFAULT: "hsl(var(--warning))",
          foreground: "hsl(var(--warning-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
    },
  },
  plugins: [animate],
};
