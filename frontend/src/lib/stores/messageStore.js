// src/lib/stores/messageStore.js
import { writable, get } from 'svelte/store';
import { authStore } from './authStore';

// You will need to install these: npm install sockjs-client stompjs
import SockJS from 'sockjs-client/dist/sockjs';
import { Stomp } from '@stomp/stompjs';

export const messageStore = writable({
    isConnected: false,
    messages: [], // Array of message objects for the active chat
    conversations: new Map(), // Map to store message history for different users (key: userId, value: array of messages)
    activeChatId: null, // ID of the user currently chatting with
    error: null,
});

let stompClient = null;
let reconnectTimeout = null;
const MAX_RECONNECT_ATTEMPTS = 5;
let reconnectAttempts = 0;

// WebSocket URL from environment variable
const WS_BASE_URL = import.meta.env.PUBLIC_API_BASE_URL.replace('/api', '/ws');


export function connectWebSocket() {
    const { token } = get(authStore);
    if (!token) {
        messageStore.update(s => ({ ...s, error: "Authentication token not available. Cannot connect to WebSocket." }));
        return;
    }

    if (stompClient && stompClient.connected) {
        console.log("WebSocket already connected.");
        return;
    }

    const socket = new SockJS(WS_BASE_URL);
    stompClient = Stomp.over(socket);

    // Disable STOMP debug output if not needed
    stompClient.debug = (str) => {
        // console.log("STOMP:", str);
    };

    stompClient.connect(
        { 'Authorization': `Bearer ${token}` }, // Pass JWT token in headers
        (frame) => {
            console.log('Connected to WebSocket:', frame);
            messageStore.update(s => ({ ...s, isConnected: true, error: null }));
            reconnectAttempts = 0; // Reset reconnect attempts on successful connection

            // Subscribe to user-specific private message queue
            const userId = get(authStore).user?.id;
            if (userId) {
                stompClient.subscribe(`/user/${userId}/queue/messages`, (message) => {
                    const receivedMessage = JSON.parse(message.body);
                    console.log('Received message:', receivedMessage);
                    messageStore.update(s => {
                        const newConversations = new Map(s.conversations);
                        const senderId = receivedMessage.sender.id;
                        const receiverId = receivedMessage.receiver.id;

                        // Determine the other participant's ID for this message
                        const otherParticipantId = (senderId === userId) ? receiverId : senderId;

                        const conversation = newConversations.get(otherParticipantId) || [];
                        newConversations.set(otherParticipantId, [...conversation, receivedMessage]);

                        // Update the active chat messages if this message belongs to the active chat
                        const newActiveMessages = (s.activeChatId === otherParticipantId) ? [...s.messages, receivedMessage] : s.messages;

                        return {
                            ...s,
                            conversations: newConversations,
                            messages: newActiveMessages // Update only active chat messages
                        };
                    });
                });
            } else {
                console.error("User ID not found in authStore for WebSocket subscription.");
            }
        },
        (error) => {
            console.error('WebSocket connection error:', error);
            messageStore.update(s => ({ ...s, isConnected: false, error: 'WebSocket connection failed.' }));
            // Attempt to reconnect
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                reconnectAttempts++;
                console.log(`Attempting to reconnect in ${2 ** reconnectAttempts * 1000}ms... (Attempt ${reconnectAttempts})`);
                reconnectTimeout = setTimeout(connectWebSocket, 2 ** reconnectAttempts * 1000); // Exponential backoff
            } else {
                console.error("Max reconnect attempts reached. Please refresh.");
            }
        }
    );
}

export function disconnectWebSocket() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {
            console.log('Disconnected from WebSocket');
            messageStore.update(s => ({ ...s, isConnected: false, messages: [], conversations: new Map(), activeChatId: null }));
            if (reconnectTimeout) {
                clearTimeout(reconnectTimeout);
            }
        });
    }
}

export function sendMessage(receiverId, content) {
    if (stompClient && stompClient.connected) {
        const senderId = get(authStore).user?.id;
        if (!senderId) {
            console.error("Cannot send message: Sender ID not found.");
            return;
        }
        const message = {
            sender: { id: senderId }, // Backend expects sender object
            receiver: { id: receiverId }, // Backend expects receiver object
            content: content,
            timestamp: new Date().toISOString() // Add client-side timestamp for display
        };
        // Publish to the STOMP broker
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(message));

        // Optimistically update local store for immediate display
        messageStore.update(s => {
            const newConversations = new Map(s.conversations);
            const conversation = newConversations.get(receiverId) || [];
            newConversations.set(receiverId, [...conversation, message]);

            const newActiveMessages = (s.activeChatId === receiverId) ? [...s.messages, message] : s.messages;

            return {
                ...s,
                conversations: newConversations,
                messages: newActiveMessages
            };
        });

    } else {
        console.error("Cannot send message: WebSocket not connected.");
        alert("Chat service not available. Please try again later.");
    }
}

// Function to set active chat and load its messages into the store's 'messages' array
export function setActiveChat(userId) {
    messageStore.update(s => ({
        ...s,
        activeChatId: userId,
        messages: s.conversations.get(userId) || [] // Load messages for this user
    }));
}

// Global subscription to handle WebSocket connection/disconnection on auth state change
authStore.subscribe(($authStore) => {
    if ($authStore.isAuthenticated && !get(messageStore).isConnected) {
        // Connect WebSocket when authenticated and not already connected
        // It's often better to call connectWebSocket() when a user visits the messages page,
        // or a dedicated chat feature is opened, rather than on every auth state change.
        // For simplicity, we'll keep it here, but be aware of when you actually need the connection.
        // connectWebSocket();
    } else if (!$authStore.isAuthenticated && get(messageStore).isConnected) {
        // Disconnect WebSocket on logout
        disconnectWebSocket();
    }
});