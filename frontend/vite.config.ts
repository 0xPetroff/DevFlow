import { fileURLToPath, URL } from 'node:url';

import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

const devApiProxy = process.env.VITE_DEV_API_PROXY ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    strictPort: true,
    // The default VITE_API_BASE_URL is the relative /api, which nginx serves in the container
    // and this proxy serves in development. Both deployments then use one identical build.
    proxy: {
      '/api': { target: devApiProxy, changeOrigin: true },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    // The framework changes far less often than the app does, and nginx caches hashed assets
    // indefinitely, so keeping them apart means a release only invalidates what it changed.
    //
    // The charting and drag-and-drop libraries are kept out of the shared vendor chunk on
    // purpose. Each is reachable only from a lazily imported route, so its own chunk is fetched
    // when that route opens; folded into vendor they would be downloaded to render a login form.
    // The lists name the packages exclusively theirs: anything the app itself also imports
    // (clsx, use-sync-external-store) must stay in vendor or the chunk becomes eager again.
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            { name: 'react', test: /node_modules\/(react|react-dom|scheduler|react-router)\// },
            // Claimed before the lazy groups below. Groups are matched in order, and a library
            // the app imports directly would otherwise be pulled into whichever lazy chunk
            // reached it first, dragging that whole chunk into the eager graph. clsx did exactly
            // that: recharts depends on it, so the entire charting bundle became a modulepreload.
            {
              name: 'vendor',
              test: /node_modules\/(clsx|tailwind-merge|lucide-react|axios|@tanstack|react-hook-form|@hookform|zod|use-sync-external-store)\//,
            },
            { name: 'charts', test: /node_modules\/(recharts|victory-vendor|d3-[^/]+)\// },
            { name: 'dnd', test: /node_modules\/@dnd-kit\// },
            { name: 'vendor', test: /node_modules\// },
          ],
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    restoreMocks: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/types/**', 'src/main.tsx'],
    },
  },
});
