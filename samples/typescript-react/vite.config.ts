import { createRequire } from "node:module";

import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import { defineConfig } from "vite";

const require = createRequire(import.meta.url);
const tsVersion: string = require("typescript/package.json").version;

export default defineConfig({
  plugins: [
    tanstackRouter({ target: "react", autoCodeSplitting: true }),
    react(),
    tailwindcss(),
  ],
  define: {
    __TS_VERSION__: JSON.stringify(tsVersion),
  },
  server: {
    port: 5173,
  },
});
