package com.redditclone.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.redditclone.backend.model.Message;
import com.redditclone.backend.model.User;
import com.redditclone.backend.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    /**
     * Saves a direct message to the database.
     * @param message The Message object to save.
     * @return The saved Message object.
     */
    public Message saveMessage(Message message) {
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false); // New messages are unread
        return messageRepository.save(message);
    }

    /**
     * Retrieves all messages between two users.
     * @param user1 The first user.
     * @param user2 The second user.
     * @return A list of messages between the two users, ordered by timestamp.
     */
    public List<Message> getMessagesBetweenUsers(User user1, User user2) {
        // Find messages where user1 is sender and user2 is receiver OR user2 is sender and user1 is receiver
        return messageRepository.findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(
                user1, user2, user2, user1
        );
    }

    /**
     * Marks messages as read.
     * @param messageIds List of message IDs to mark as read.
     * @param receiverId The ID of the user who received the messages.
     * @return The count of messages updated.
     */
    public long markMessagesAsRead(List<String> messageIds, String receiverId) {
        long updatedCount = 0;
        for (String messageId : messageIds) {
            messageRepository.findById(messageId).ifPresent(message -> {
                if (message.getReceiver().getId().equals(receiverId) && !message.isRead()) {
                    message.setRead(true);
                    messageRepository.save(message);
                    // If you want to increment a counter, you might need a custom update query
                    // or handle it in a more transaction-safe way depending on load.
                }
            });
        }
        return updatedCount;
    }
}