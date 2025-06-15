<script>
    import { authStore } from '$lib/stores/authStore';
    import { messages } from '$lib/api';
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { connectWebSocket, disconnectWebSocket, messageStore } from '$lib/stores/messageStore';

    let conversations = []; // This would typically be a list of users you've chatted with
    let loading = true;
    let error = '';

    // Redirect if not authenticated
    authStore.subscribe(auth => {
        if (!auth.isAuthenticated) {
            goto('/login');
        }
    });

    // This is a placeholder for fetching conversations.
    // In a real app, you'd have a backend endpoint to get a list of users
    // you have direct message conversations with.
    async function fetchConversations() {
        loading = true;
        error = '';
        try {
            // Example: Fetching a list of all users to simulate starting a conversation
            // In production, this would be a specific API endpoint for user's conversations
            const response = await messages.getConversation("dummyUserId"); // Replace with actual API call
            // For now, let's just create a dummy list of users
            conversations = [
                { id: 'user123', username: 'Alice' },
                { id: 'user456', username: 'Bob' },
                { id: 'user789', username: 'Charlie' },
            ];
        } catch (err) {
            console.error('Failed to fetch conversations:', err);
            error = 'Failed to load conversations.';
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        fetchConversations();
        // Connect to WebSocket when message page mounts (or based on some user action)
        connectWebSocket();
        return () => {
            disconnectWebSocket(); // Disconnect on unmount
        };
    });
</script>

<div class="max-w-2xl mx-auto bg-white dark:bg-gray-800 p-8 rounded-lg shadow-lg transition-colors duration-300">
    <h1 class="text-3xl font-bold text-center mb-6 text-gray-900 dark:text-gray-100">Direct Messages</h1>

    {#if loading}
        <p class="text-center text-gray-600 dark:text-gray-400">Loading conversations...</p>
    {:else if error}
        <p class="text-red-500 text-center">{error}</p>
    {:else if conversations.length === 0}
        <p class="text-gray-600 dark:text-gray-400 text-center">No conversations yet.</p>
    {:else}
        <h2 class="text-xl font-semibold mb-4 text-gray-900 dark:text-gray-100">Your Conversations</h2>
        <ul class="space-y-4">
            {#each conversations as conversation (conversation.id)}
                <li class="bg-gray-50 dark:bg-gray-700 p-4 rounded-md shadow-sm flex justify-between items-center">
                    <a href="/messages/{conversation.id}" class="text-blue-600 dark:text-blue-400 hover:underline text-lg font-medium">
                        {conversation.username}
                    </a>
                    </li>
            {/each}
        </ul>
    {/if}

    <div class="mt-8 text-center text-gray-600 dark:text-gray-400">
        {#if $messageStore.isConnected}
            <p class="text-green-500">WebSocket Connected.</p>
        {:else}
            <p class="text-red-500">WebSocket Disconnected. {$messageStore.error}</p>
        {/if}
    </div>
</div>
