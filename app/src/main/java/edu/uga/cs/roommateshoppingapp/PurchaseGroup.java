package edu.uga.cs.roommateshoppingapp;

import java.util.ArrayList;
import java.util.List;

public class PurchaseGroup {
    private String id;
    private List<ShoppingItem> items;
    private double totalPrice;
    private String purchasedBy;
    private long timestamp;

    // Required empty constructor for Firebase
    public PurchaseGroup() {
        this.items = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public PurchaseGroup(List<ShoppingItem> items, double totalPrice, String purchasedBy) {
        this.items = items;
        this.totalPrice = totalPrice;
        this.purchasedBy = purchasedBy;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ShoppingItem> getItems() {
        return items;
    }

    public void setItems(List<ShoppingItem> items) {
        this.items = items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getPurchasedBy() {
        return purchasedBy;
    }

    public void setPurchasedBy(String purchasedBy) {
        this.purchasedBy = purchasedBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}