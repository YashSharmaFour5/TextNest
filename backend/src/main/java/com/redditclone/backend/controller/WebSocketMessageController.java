package com.redditclone.backend.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.redditclone.backend.model.Message;
import com.redditclone.backend.model.User;
import com.redditclone.backend.service.MessageService;
import com.redditclone.backend.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final SimpMessagingTemplate messagingTemplate; // Used to send messages to specific users
    private final MessageService messageService;
    private final UserService userService;

    /**
     * Handles incoming direct messages from a client.
     * Messages sent to "/app/chat.sendMessage" will be handled here.
     * @param chatMessage The Message object containing sender, receiver, and content.
     * @param headerAccessor Accessor for WebSocket headers, useful for getting user info.
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Retrieve sender from WebSocket session (or from JWT if passed in header and processed by filter)
        // For simplicity, let's assume senderId is part of the chatMessage payload or derived from authentication.
        // In a real app, you'd get the authenticated user's ID here.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderUsername = authentication.getName(); // Get username from Spring Security context

        Optional<User> senderOpt = userService.findByUsername(senderUsername);
        Optional<User> receiverOpt = userService.findById(chatMessage.getReceiver().getId()); // Assume receiver ID is set in payload

        if (senderOpt.isPresent() && receiverOpt.isPresent()) {
            User sender = senderOpt.get();
            User receiver = receiverOpt.get();

            chatMessage.setSender(sender);
            chatMessage.setReceiver(receiver);
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setRead(false); // Mark as unread initially

            // Save the message to the database
            Message savedMessage = messageService.saveMessage(chatMessage);

            // Send the message to the receiver's private queue
            // The destination format is `/user/{receiverId}/queue/messages`
            messagingTemplate.convertAndSendToUser(
                    receiver.getId(), // This should be the recipient's unique identifier for routing
                    "/queue/messages", // The private queue for messages
                    savedMessage // The message object to send
            );

            // Optionally, send the message back to the sender to update their chat window
            // This is good for immediate UI feedback without another API call
            messagingTemplate.convertAndSendToUser(
                    sender.getId(),
                    "/queue/messages",
                    savedMessage
            );

        } else {
            // Handle error: sender or receiver not found
            System.err.println("Error: Sender or Receiver not found for message.");
        }
    }
}