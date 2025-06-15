<script>
    import '../app.css'; // Your global Tailwind CSS file
    import Header from '$lib/components/Header.svelte';
    import Footer from '$lib/components/Footer.svelte';
    import { themeStore } from '$lib/stores/themeStore';

    // This reactive declaration will ensure the theme class is applied
    // whenever themeStore changes. The logic is already in themeStore.js
    // to handle the DOM manipulation.
    // For SSR, you might need to handle this in `+layout.server.js` or `+layout.js`
    // to set the class on the initial render.
</script>

<div class="min-h-screen bg-gray-200 text-gray-900 dark:bg-gray-900 dark:text-gray-100 transition-colors duration-300">
    <Header />
    <main class="container mx-auto p-4 max-w-7xl">
        <slot /> </main>
    <Footer />
</div>