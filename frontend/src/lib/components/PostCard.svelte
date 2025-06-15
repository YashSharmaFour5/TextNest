<script>
    import { authStore } from '$lib/stores/authStore';
    import { posts } from '$lib/api';
    import { createEventDispatcher, getContext } from 'svelte';
    import { goto } from '$app/navigation';

    export let post;

    const dispatch = createEventDispatcher();
    const filterByTag = getContext('filterByTag');

    async function handleToggleLike(event) {
        event.stopPropagation();
        if (!$authStore.isAuthenticated) {
            goto('/login');
            return;
        }
        try {
            const response = await posts.toggleLike(post.id);
            dispatch('postUpdated', response.data);
        } catch (err) {
            console.error('Failed to toggle like:', err.response?.data || err);
            alert(err.response?.data?.message || 'Failed to like/unlike post.');
        }
    }

    function handleFilterByTagClick(tag, event) {
        event.stopPropagation();
        if (filterByTag) {
            filterByTag(tag);
        } else {
            console.warn('filterByTag context not available.');
        }
    }

    let mediaPreviewUrl = null;
    let mediaType = null;

    $: {
        if (post.mediaUrls && post.mediaUrls.length > 0) {
            const firstUrl = post.mediaUrls[0];

            if (firstUrl.match(/\.(jpeg|jpg|gif|png|webp)$/i)) {
                mediaPreviewUrl = firstUrl;
                mediaType = 'image';
            } else if (firstUrl.match(/\.(mp4|webm|ogg)$/i)) {
                mediaPreviewUrl = firstUrl;
                mediaType = 'video';
            } else if (firstUrl.includes("youtube.com/embed/") || firstUrl.includes("youtu.be/")) {
                const videoIdMatch = firstUrl.match(/(?:youtube\.com\/(?:[^\/]+\/.+\/|\/(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([a-zA-Z0-9_-]{11})/);
                if (videoIdMatch && videoIdMatch[1]) {
                    mediaPreviewUrl = `http://www.youtube.com/watch?v=9IFnKazfiWM{videoIdMatch[1]}/hqdefault.jpg`;
                    mediaType = 'youtube';
                } else {
                    mediaPreviewUrl = null;
                    mediaType = null;
                }
            }
             else {
                mediaPreviewUrl = null;
                mediaType = 'link';
            }
        } else {
            mediaPreviewUrl = null;
            mediaType = null;
        }
    }

    function navigateToPost(event) {
        if (event.target.closest('button')) {
            return;
        }
        goto(`/posts/${post.id}`);
    }
</script>

<div
    class="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md transition-colors duration-300 block no-underline text-inherit hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer"
    on:click={navigateToPost}
    role="button"
    tabindex="0"
    on:keydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { navigateToPost(e); } }}
>
    <h2 class="text-xl font-bold mb-2 text-gray-900 dark:text-gray-100">{post.title}</h2>
    {#if post.isNsfw && (!$authStore.user || !$authStore.user.isAdult)}
        <div class="bg-red-100 dark:bg-red-900 border border-red-400 text-red-700 dark:text-red-200 px-4 py-3 rounded relative mb-4">
            <span class="block sm:inline">⚠️ This content is 18+. Log in as an adult to view.</span>
        </div>
    {:else}
        {#if post.content}
            <p class="text-gray-700 dark:text-gray-300 mb-4 line-clamp-3">{post.content}</p>
        {/if}

        {#if mediaPreviewUrl}
            <div class="mb-4 max-h-48 overflow-hidden rounded-md relative group">
                {#if mediaType === 'image' || mediaType === 'youtube'}
                    <img src={mediaPreviewUrl} alt="Post media preview" class="w-full h-full object-cover" />
                    {#if mediaType === 'youtube'}
                        <div class="absolute inset-0 flex items-center justify-center bg-black bg-opacity-40 text-white opacity-0 group-hover:opacity-100 transition-opacity">
                            <svg class="w-16 h-16" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                        </div>
                    {/if}
                {:else if mediaType === 'video'}
                    <video src={mediaPreviewUrl} class="w-full h-full object-cover" preload="metadata">
                        <track kind="captions" label="No captions" />
                    </video>
                    <div class="absolute inset-0 flex items-center justify-center bg-black bg-opacity-40 text-white opacity-0 group-hover:opacity-100 transition-opacity">
                        <svg class="w-16 h-16" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                    </div>
                {/if}
                 {#if post.mediaUrls.length > 1}
                    <div class="absolute top-2 right-2 bg-black bg-opacity-50 text-white text-xs py-1 px-2 rounded">
                        1/{post.mediaUrls.length}
                    </div>
                {/if}
            </div>
        {:else if mediaType === 'link'}
            <div class="mb-4 px-4 py-2 bg-blue-50 dark:bg-blue-900 text-blue-700 dark:text-blue-300 rounded-md flex items-center">
                <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M12.586 4.586a2 2 0 112.828 2.828l-3 3a2 2 0 01-2.828 0 1 1 0 00-1.414 1.414 4 4 0 005.656 0l3-3a4 4 0 00-5.656-5.656l-1.5 1.5a1 1 0 101.414 1.414l1.5-1.5zm-5 5a2 2 0 012.828 0l3 3a2 2 0 11-2.828 2.828l-3-3a2 2 0 010-2.828 1 1 0 00-1.414-1.414 4 4 0 000 5.656l3 3a4 4 0 005.656 0 .999.999 0 00-1.414-1.414 2 2 0 01-2.828 0l-3-3a2 2 0 010-2.828z" clip-rule="evenodd"></path></svg>
                <span>Attached Document/Link</span>
            </div>
        {/if}
        <div class="flex items-center justify-between text-gray-600 dark:text-gray-400 text-sm">
            <span>Posted by <span class="font-semibold">{post.author?.username || 'Unknown'}</span></span>
            <span>{new Date(post.createdAt).toLocaleDateString('en-GB')}</span>
        </div>
        <div class="mt-4 flex items-center space-x-4">
            <button
                on:click|stopPropagation={handleToggleLike}
                class="flex items-center space-x-1 px-3 py-1 rounded-full
                {post.likedBy?.includes($authStore.user?.id) ? 'bg-blue-200 text-blue-700 dark:bg-blue-700 dark:text-blue-200' : 'bg-gray-200 text-gray-700 dark:bg-gray-700 dark:text-gray-300'}
                hover:bg-blue-300 dark:hover:bg-blue-600 transition-colors"
            >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd"></path></svg>
                <span>{post.likeCount} Likes</span>
            </button>
            <button
                type="button"
                class="text-blue-500 hover:underline bg-transparent border-none p-0 m-0 cursor-pointer"
                on:click|stopPropagation={() => goto(`/posts/${post.id}`)}
            >View Comments</button>
        </div>
        {#if post.tags && post.tags.length > 0}
            <div class="mt-2 text-sm text-gray-500 dark:text-gray-400">
                Tags:
                {#each post.tags as tag}
                    <button on:click|stopPropagation={() => handleFilterByTagClick(tag)} class="inline-block bg-gray-200 dark:bg-gray-700 rounded-full px-3 py-1 text-xs font-semibold text-gray-700 dark:text-gray-300 mr-2 mb-2 hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors">#{tag}</button>
                {/each}
            </div>
        {/if}
    {/if}
</div>