<script>
    import { onMount, onDestroy } from 'svelte';
    import { page } from '$app/stores';
    import { authStore } from '$lib/stores/authStore';
    import { connectWebSocket, disconnectWebSocket, sendMessage, messageStore } from '$lib/stores/messageStore';
    import { messages as apiMessages } from '$lib/api';
    import { goto } from '$app/navigation';
    import Spinner from '$lib/components/Spinner.svelte';

    let recipientId = $page.params.userId;
    let recipientUsername = 'Loading...'; // Will fetch from backend based on ID
    let currentMessage = '';
    let chatMessages = [];
    let loading = true;
    let error = '';
    let chatContainer; // Bind to the chat messages div for auto-scrolling

    // Redirect if not authenticated
    authStore.subscribe(auth => {
        if (!auth.isAuthenticated) {
            goto('/login');
        }
    });

    // Function to fetch message history
    async function fetchMessageHistory() {
        loading = true;
        error = '';
        try {
            // In a real app, you'd have an API to get user details by ID
            // For simplicity, let's assume we can set recipientUsername directly or from a dummy list.
            // Replace this with a call to your backend user API if needed.
            const recipientResponse = await fetch(`http://localhost:8080/api/users/${recipientId}`); // Dummy call, replace
            if (recipientResponse.ok) {
                const recipient = await recipientResponse.json();
                recipientUsername = recipient.username;
            } else {
                recipientUsername = 'Unknown User';
                console.warn(`Could not fetch username for ID: ${recipientId}`);
            }


            // Fetch actual message history from backend
            const response = await apiMessages.getConversation(recipientId); // This API needs to be implemented on backend
            chatMessages = response.data; // Assuming backend returns list of messages
        } catch (err) {
            console.error('Failed to fetch message history:', err);
            error = 'Failed to load message history.';
        } finally {
            loading = false;
        }
    }

    // Function to send message via WebSocket
    function handleSendMessage() {
        if (currentMessage.trim() && $messageStore.isConnected) {
            sendMessage(recipientId, currentMessage);
            currentMessage = ''; // Clear input
        } else if (!$messageStore.isConnected) {
            alert('Not connected to chat. Please refresh or try again.');
        }
    }

    // Auto-scroll to bottom of chat
    function scrollToBottom() {
        if (chatContainer) {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    }

    // Subscribe to messages from the store and auto-scroll
    messageStore.subscribe(store => {
        if (store.messages.length > chatMessages.length) { // Only update if new messages arrived
            // Filter messages to show only those relevant to current chat
            const currentUserId = get(authStore).user?.id;
            chatMessages = store.messages.filter(msg =>
                (msg.sender?.id === currentUserId && msg.receiver?.id === recipientId) ||
                (msg.sender?.id === recipientId && msg.receiver?.id === currentUserId)
            );
            setTimeout(scrollToBottom, 50); // Scroll after DOM update
        }
    });


    onMount(() => {
        fetchMessageHistory();
        connectWebSocket(); // Ensure WebSocket is connected for this chat session

        // Set active chat ID in the store (if you want to highlight active chats)
        messageStore.update(s => ({ ...s, activeChatId: recipientId }));

        return () => {
            // Cleanup: Clear active chat ID when leaving this page
            messageStore.update(s => ({ ...s, activeChatId: null }));
            // Disconnect WebSocket if this is the only place it's used or on logout
            // (Depends on overall WebSocket strategy)
            disconnectWebSocket();
        };
    });
</script>

<div class="max-w-xl mx-auto bg-white dark:bg-gray-800 p-6 rounded-lg shadow-lg transition-colors duration-300 flex flex-col h-[calc(100vh-200px)]">
    <h1 class="text-2xl font-bold mb-4 text-gray-900 dark:text-gray-100">Chat with {recipientUsername}</h1>

    {#if loading}
        <div class="flex-grow flex items-center justify-center">
            <Spinner />
        </div>
    {:else if error}
        <div class="flex-grow flex items-center justify-center text-red-500">
            <p>{error}</p>
        </div>
    {:else}
        <div bind:this={chatContainer} class="flex-grow overflow-y-auto p-4 space-y-4 border rounded-md border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700 mb-4">
            {#each chatMessages as msg (msg.id)}
                <div class="flex {msg.sender.id === $authStore.user?.id ? 'justify-end' : 'justify-start'}">
                    <div class="{msg.sender.id === $authStore.user?.id ? 'bg-blue-500 text-white' : 'bg-gray-200 dark:bg-gray-600 text-gray-900 dark:text-gray-100'}
                                rounded-lg p-3 max-w-[80%] break-words">
                        <p class="font-semibold text-sm mb-1">{msg.sender?.username || 'Unknown'}</p>
                        <p>{msg.content}</p>
                        <span class="text-xs opacity-75 mt-1 block text-right">{new Date(msg.timestamp).toLocaleTimeString()}</span>
                    </div>
                </div>
            {/each}
        </div>

        <div class="flex space-x-2">
            <input
                type="text"
                bind:value={currentMessage}
                on:keydown={(e) => e.key === 'Enter' && handleSendMessage()}
                placeholder="Type your message..."
                class="flex-grow shadow appearance-none border rounded py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
            />
            <button
                on:click={handleSendMessage}
                class="bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition-colors duration-300"
                disabled={!currentMessage.trim() || !$messageStore.isConnected}
            >
                Send
            </button>
        </div>
    {/if}
</div>
