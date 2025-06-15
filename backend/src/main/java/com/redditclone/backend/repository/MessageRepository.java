package com.redditclone.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.redditclone.backend.model.Message;
import com.redditclone.backend.model.User;

public interface MessageRepository extends MongoRepository<Message, String> {
    // Find all messages between two users (ordered by timestamp)
    List<Message> findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(User sender1, User receiver1, User sender2, User receiver2);

    // Find all messages sent by a user
    List<Message> findBySender(User sender);

    // Find all unread messages for a receiver
    List<Message> findByReceiverAndReadFalse(User receiver);
}