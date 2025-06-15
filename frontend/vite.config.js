import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [tailwindcss(), sveltekit()],
	server: {
        proxy: {
            '/api': { // Any request starting with /api
                target: 'http://localhost:8080', // Proxy it to your backend's address
                changeOrigin: true, // Needed for virtual hosted sites
                rewrite: (path) => path.replace(/^\/api/, '/api'), // Rewrite the path (optional, depends on your backend)
                // If your backend endpoint is /auth/signup (without /api prefix):
                // rewrite: (path) => path.replace(/^\/api/, '')
            }
        }
    }
});
