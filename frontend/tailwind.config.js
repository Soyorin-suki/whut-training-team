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
        'bg-secondary': '#F6F8FA',
        'text-primary': '#24292F',
        'text-secondary': '#57606A',
        'success': '#2DA44E',
        'warning': '#D29922',
        'error': '#CF222E',
        'border': '#d0d7de',
        'hover': '#e8eaed',
        'active': '#d0d7de',
      },
      borderRadius: {
        'ui': '8px',
      },
      maxWidth: {
        'page': '1400px',
      },
      spacing: {
        'page': '24px',
      },
      fontSize: {
        'nav': '14px',
      },
    },
  },
  plugins: [],
}
