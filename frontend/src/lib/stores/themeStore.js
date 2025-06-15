import { writable } from 'svelte/store';

function getInitialTheme() {
    if (typeof window === 'undefined') {
        return 'light'; // Default for SSR
    }
    const storedTheme = localStorage.getItem('theme');
    if (storedTheme) {
        return storedTheme;
    }
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        return 'dark';
    }
    return 'light';
}

const theme = writable(getInitialTheme());

// ✅ Immediately apply theme class to <html> on load
if (typeof window !== 'undefined') {
    const initialTheme = getInitialTheme();
    document.documentElement.classList.toggle('dark', initialTheme === 'dark');
}

theme.subscribe(value => {
    if (typeof window !== 'undefined') {
        localStorage.setItem('theme', value);
        document.documentElement.classList.toggle('dark', value === 'dark');
    }
});

export const themeStore = theme;

/*export function toggleTheme() {
    theme.update(currentTheme => {
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        return newTheme;
    });
}
export function setTheme(newTheme) {
    theme.set(newTheme);
}
export function isDarkMode() {
    return $themeStore === 'dark';
}
export function isLightMode() {
    return $themeStore === 'light';
}
export function getCurrentTheme() {
    return $themeStore;
}
export function getThemeClass() {
    return $themeStore === 'dark' ? 'dark' : 'light';
}
export function getThemeIcon() {
    return $themeStore === 'dark' ? 'moon' : 'sun';
}
export function getThemeContrast() {
    return $themeStore === 'dark' ? 'contrast-dark' : 'contrast-light';
}
export function getThemeContrastClass() {
    return $themeStore === 'dark' ? 'contrast-dark' : 'contrast-light';
}
export function getThemeContrastIcon() {
    return $themeStore === 'dark' ? 'contrast-moon' : 'contrast-sun';
}*/