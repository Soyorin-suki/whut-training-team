/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'bg-primary': '#FFFFFF',
        'bg-secondary': '#F4F4F0',
        'text-primary': '#111111',
        'text-secondary': '#6F6F69',
        'success': '#365A3A',
        'warning': '#D29922',
        'error': '#CF222E',
        'border': '#D9D9D3',
        'hover': '#EAEAE5',
        'active': '#D2D2CC',
      },
      borderRadius: {
        'ui': '14px',
      },
      maxWidth: {
        'page': '1540px',
      },
      spacing: {
        'page': '32px',
      },
      fontSize: {
        'nav': '14px',
      },
    },
  },
  plugins: [],
}
