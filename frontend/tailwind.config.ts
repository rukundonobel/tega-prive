import type { Config } from 'tailwindcss'

// TEGA PRIVÉ brand palette
// Deep forest green #101C19 · White #FFFFFF · Champagne gold #C5A46D · Light grey #F5F6F4 · Dark text #202824
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        forest: { DEFAULT: '#101C19', light: '#1B2A24', deep: '#0B1311' },
        gold: { DEFAULT: '#C5A46D', light: '#D8BE93', dark: '#A88754' },
        mist: '#F5F6F4',
        ink: '#202824',
        line: '#E4E7E2',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
        display: ['Georgia', 'Cambria', 'Times New Roman', 'serif'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(16,28,25,0.04), 0 8px 24px rgba(16,28,25,0.06)',
        'card-hover': '0 2px 4px rgba(16,28,25,0.06), 0 16px 40px rgba(16,28,25,0.10)',
      },
      keyframes: {
        floaty: { '0%,100%': { transform: 'translateY(0)' }, '50%': { transform: 'translateY(-6px)' } },
        fadeIn: { '0%': { opacity: '0', transform: 'translateY(8px)' }, '100%': { opacity: '1', transform: 'translateY(0)' } },
      },
      animation: {
        floaty: 'floaty 6s ease-in-out infinite',
        fadeIn: 'fadeIn 0.5s ease-out both',
      },
    },
  },
  plugins: [],
} satisfies Config
