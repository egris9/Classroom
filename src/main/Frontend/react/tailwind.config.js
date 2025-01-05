import withMT from "@material-tailwind/react/utils/withMT"
/** @type {import('tailwindcss').Config} */
export default withMT({
  content: ["./index.html", "./src/**/*.{jsx,css}","./node_modules/@material-tailwind/react/components/**/*.{js,jsx}",
    "./node_modules/@material-tailwind/react/theme/components/**/*.{js,jsx}",],
  theme: {
    extend: {},
  },
  plugins: [import('@tailwindcss/forms'),],
});
