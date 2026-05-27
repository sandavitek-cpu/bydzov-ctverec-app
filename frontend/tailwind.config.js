/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: {
          DEFAULT: 'rgb(var(--bg-rgb) / <alpha-value>)',
          alt: 'rgb(var(--bg-alt-rgb) / <alpha-value>)',
        },
        surface: {
          DEFAULT: 'rgb(var(--surface-rgb) / <alpha-value>)',
          2: 'rgb(var(--surface-2-rgb) / <alpha-value>)',
          strong: 'rgb(var(--surface-strong-rgb) / <alpha-value>)',
        },
        border: {
          DEFAULT: 'rgb(var(--border-rgb) / <alpha-value>)',
          strong: 'rgb(var(--border-strong-rgb) / <alpha-value>)',
        },
        text: {
          DEFAULT: 'rgb(var(--text-rgb) / <alpha-value>)',
          muted: 'rgb(var(--text-muted-rgb) / <alpha-value>)',
          soft: 'rgb(var(--text-soft-rgb) / <alpha-value>)',
          inverse: 'rgb(var(--text-inverse-rgb) / <alpha-value>)',
        },
        primary: {
          DEFAULT: 'rgb(var(--primary-rgb) / <alpha-value>)',
          hover: 'rgb(var(--primary-hover-rgb) / <alpha-value>)',
          active: 'rgb(var(--primary-active-rgb) / <alpha-value>)',
        },
        red: {
          DEFAULT: 'rgb(var(--red-rgb) / <alpha-value>)',
          hover: 'rgb(var(--red-hover-rgb) / <alpha-value>)',
          light: 'rgb(var(--red-light-rgb) / <alpha-value>)',
        },
        success: 'rgb(var(--success-rgb) / <alpha-value>)',
        warning: 'rgb(var(--warning-rgb) / <alpha-value>)',
        error: 'rgb(var(--error-rgb) / <alpha-value>)',
        info: 'rgb(var(--info-rgb) / <alpha-value>)',
      },
      fontFamily: {
        display: ['"Bebas Neue"', 'sans-serif'],
        body: ['"Inter"', '"Source Sans 3"', 'sans-serif'],
        accent: ['"Merriweather"', 'serif'],
      },
      borderRadius: {
        sm: '4px',
        md: '6px',
        lg: '8px',
        xl: '12px',
        pill: '999px',
      },
      boxShadow: {
        sm: '0 2px 6px rgba(42,36,28,0.06)',
        md: '0 8px 20px rgba(42,36,28,0.10)',
        lg: '0 16px 40px rgba(42,36,28,0.14)',
      },
      spacing: {
        18: '72px',
        22: '88px',
      },
      maxWidth: {
        content: '1200px',
        wide: '1280px',
        form: '720px',
      },
    },
  },
  plugins: [],
}
