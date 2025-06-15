// src/lib/stores/themeStore.js
import { writable } from 'svelte/store';

// Function to get initial theme from localStorage or system preference
function getInitialTheme() {
    if (typeof window === 'undefined') {
        return 'light'; // Default for SSR
    }
    const storedTheme = localStorage.getItem('theme');
    if (storedTheme) {
        return storedTheme;
    }
    // Check system preference
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        return 'dark';
    }
    return 'light'; // Default fallback
}

const theme = writable(getInitialTheme());

// Update localStorage and document class when theme changes
theme.subscribe(value => {
    if (typeof window !== 'undefined') {
        localStorage.setItem('theme', value);
        if (value === 'dark') {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
    }
});

export const themeStore = theme;