package com.example.shopping.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
public class Event {
    @Id
    private String id = UUID.randomUUID().toString();
    
    private LocalDateTime createdAt = LocalDateTime.now(); // Для сортировки
    private String timestamp;
    private String action; 
    private String productName;
    private String userAvatar;

    public Event() {}

    public Event(String action, String productName, String userAvatar) {
        this.action = action;
        this.productName = productName;
        this.userAvatar = userAvatar;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public String getProductName() { return productName; }
    public String getUserAvatar() { return userAvatar; }
}