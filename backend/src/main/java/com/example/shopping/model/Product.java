package com.example.shopping.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Product {
    private String id = UUID.randomUUID().toString();
    private String name;
    private double price;
    private boolean bought = false;
    private boolean approved;
    private String addedByAvatar;
    private Set<String> voters = new HashSet<>(); 

    public Product() {}

    public Product(String name, double price, String addedByAvatar) {
        this.name = name;
        this.price = price;
        this.addedByAvatar = addedByAvatar;
        this.approved = price <= 500; 
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public boolean isBought() { return bought; }
    public void setBought(boolean bought) { this.bought = bought; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getAddedByAvatar() { return addedByAvatar; }
    public Set<String> getVoters() { return voters; }
    
    public void addVote(String userId) {
        voters.add(userId);
        if (voters.size() >= 3) {
            this.approved = true; 
        }
    }
}