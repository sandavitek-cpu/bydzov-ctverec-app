/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: {
          DEFAULT: '#FFFFFF',
          alt: '#F3F4F6',
        },
        surface: {
          DEFAULT: '#FFFFFF',
          2: '#F9FAFB',
          strong: '#E5E7EB',
        },
        border: {
          DEFAULT: '#D1D5DB',
          strong: '#9CA3AF',
        },
        text: {
          DEFAULT: '#111827',
          muted: '#4B5563',
          soft: '#6B7280',
          inverse: '#FFFFFF',
        },
        primary: {
          DEFAULT: '#09097B',
          hover: '#070763',
          active: '#05054B',
        },
        red: {
          DEFAULT: '#DC2626',
          hover: '#B91C1C',
          light: '#FEE2E2',
        },
        success: '#16A34A',
        warning: '#D97706',
        error: '#DC2626',
        info: '#2563EB',
      },
      fontFamily: {
        display: ['"Bebas Neue"', '"Oswald"', 'sans-serif'],
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
