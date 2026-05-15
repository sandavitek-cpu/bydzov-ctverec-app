/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: {
          DEFAULT: '#F4F0E8',
          alt: '#ECE5D8',
        },
        surface: {
          DEFAULT: '#FBF8F2',
          2: '#F6F1E8',
          strong: '#E7DED0',
        },
        border: {
          DEFAULT: '#D3C7B6',
          strong: '#B8AA96',
        },
        text: {
          DEFAULT: '#2A241C',
          muted: '#5C5246',
          soft: '#857865',
          inverse: '#F8F4EC',
        },
        primary: {
          DEFAULT: '#09097B',
          hover: '#070763',
          active: '#05054B',
        },
        olive: {
          DEFAULT: '#6F7750',
        },
        petrol: {
          DEFAULT: '#2F5D62',
        },
        gold: {
          DEFAULT: '#B88A3B',
        },
        success: '#4E6B3C',
        warning: '#A56A1F',
        error: '#9E2F2F',
        info: '#3F667A',
      },
      fontFamily: {
        display: ['"Bebas Neue"', '"Oswald"', 'sans-serif'],
        body: ['"Inter"', '"Source Sans 3"', 'sans-serif'],
        accent: ['"Merriweather"', 'serif'],
      },
      borderRadius: {
        sm: '6px',
        md: '10px',
        lg: '14px',
        xl: '18px',
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
