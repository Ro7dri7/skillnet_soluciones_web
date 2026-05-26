/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        'skillnet-dark': '#032b60',
        'skillnet-accent': '#145bff',
        'skillnet-sky': '#89ceff',
        'skillnet-cyan': '#39b8fd',
        'skillnet-surface': '#f8f9ff',
        'skillnet-muted': '#43474f',
      },
      boxShadow: {
        card: '0 4px 20px rgba(3, 43, 96, 0.08)',
      },
      keyframes: {
        cartDropdown: {
          '0%': { opacity: '0', transform: 'translateY(-8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        cartDropdown: 'cartDropdown 0.2s ease-out',
      },
    },
  },
  plugins: [],
};
