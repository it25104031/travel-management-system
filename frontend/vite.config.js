import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The Spring Boot backend runs on http://localhost:8081.
// Requests to /api and /payment-slips are forwarded to it, so the browser
// sees one origin and the login session cookie just works (no CORS needed).
const backend = {
  '/api': { target: 'http://localhost:8081', changeOrigin: true },
  '/payment-slips': { target: 'http://localhost:8081', changeOrigin: true },
};

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: backend },
  preview: { port: 4173, proxy: backend },
});
