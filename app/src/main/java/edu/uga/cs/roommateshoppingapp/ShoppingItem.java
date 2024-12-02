package edu.uga.cs.roommateshoppingapp;

import java.util.Objects;

public class ShoppingItem {
    private String id = "";
    private String itemName = "";
    private boolean isPurchased = false;
    private double price = 0.0;
    private String purchasedBy = "";
    private long timestamp = 0;
    private boolean inBasket = false;

    public boolean isInBasket() {
        return inBasket;
    }

    public void setInBasket(boolean inBasket) {
        this.inBasket = inBasket;
    }
    // Required empty constructor for Firebase
    public ShoppingItem() {
        // Default constructor with timestamp
        this.timestamp = System.currentTimeMillis();
    }

    public ShoppingItem(String itemName) {
        this.itemName = itemName;
        this.isPurchased = false;
        this.price = 0.0;
        this.purchasedBy = "";
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getItemName() {
        return itemName != null ? itemName : "";
    }

    public void setItemName(String itemName) {
        this.itemName = itemName != null ? itemName : "";
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPurchasedBy() {
        return purchasedBy != null ? purchasedBy : "";
    }

    public void setPurchasedBy(String purchasedBy) {
        this.purchasedBy = purchasedBy != null ? purchasedBy : "";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingItem that = (ShoppingItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ShoppingItem{" +
                "id='" + id + '\'' +
                ", itemName='" + itemName + '\'' +
                ", isPurchased=" + isPurchased +
                '}';
    }
}