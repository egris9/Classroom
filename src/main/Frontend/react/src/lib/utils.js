import { clsx } from "clsx";
import { extendTailwindMerge } from "tailwind-merge";

// The utilities tailwind.config.js adds. Without this, tailwind-merge reads `text-title` as a text colour and
// `shadow-raised` as a shadow colour, and drops or keeps the wrong class when one meets a shadcn component's own.
const twMerge = extendTailwindMerge({
  extend: {
    classGroups: {
      "font-size": [{ text: ["display", "title", "heading"] }],
      leading: [{ leading: ["reading"] }],
      shadow: [{ shadow: ["raised"] }],
    },
  },
});

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}
