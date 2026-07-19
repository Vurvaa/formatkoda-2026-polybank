import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const basePath = process.env.VITE_BASE_PATH || '/';
console.log('[vite.config.ts] VITE_BASE_PATH from process.env:', JSON.stringify(process.env.VITE_BASE_PATH));
console.log('[vite.config.ts] resolved basePath:', JSON.stringify(basePath));

export default defineConfig({
  base: basePath,
  define: {
    'import.meta.env.VITE_BASE_PATH': JSON.stringify(basePath),
  },
  plugins: [react()],
  server: {
    port: 5173
  }
});

