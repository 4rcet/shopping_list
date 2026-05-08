package com.example.shopping.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Event {
    private String id = UUID.randomUUID().toString();
    private String timestamp;
    private String action; // e.g., "Добавлено", "Удалено", "Куплено"
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
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public String getProductName() { return productName; }
    public String getUserAvatar() { return userAvatar; }
}