/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        exchange: {
          bg: '#0B0E11',
          panel: '#181A20',
          border: '#2B3139',
          text: '#EAECEF',
          muted: '#848E9C',
          green: '#0ECB81',
          red: '#F6465D',
          hover: '#2B3139',
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
