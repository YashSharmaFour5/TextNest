package com.redditclone.backend.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    @DBRef
    private User sender;
    @DBRef
    private User receiver;
    private String content;
    private LocalDateTime timestamp;
    private boolean read; // Has the receiver read this message?

    public Message() {
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }
}