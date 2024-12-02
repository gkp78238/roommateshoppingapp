package edu.uga.cs.roommateshoppingapp;

import java.util.HashSet;
import java.util.Set;

public class ShoppingBasket {
    private static ShoppingBasket instance;
    private final Set<ShoppingItem> basketItems;

    private ShoppingBasket() {
        basketItems = new HashSet<>();
    }

    public static synchronized ShoppingBasket getInstance() {
        if (instance == null) {
            instance = new ShoppingBasket();
        }
        return instance;
    }

    public void addToBasket(ShoppingItem item) {
        basketItems.add(item);
    }

    public void addAllToBasket(Set<ShoppingItem> items) {
        basketItems.clear();
        for (ShoppingItem item : items) {
            basketItems.add(item);
        }
    }

    public void removeFromBasket(ShoppingItem item) {
        basketItems.remove(item);
    }

    public void clearBasket() {
        basketItems.clear();
    }

    public Set<ShoppingItem> getBasketItems() {
        return new HashSet<>(basketItems);
    }

    public boolean hasItems() {
        return !basketItems.isEmpty();
    }

    public int getItemCount() {
        return basketItems.size();
    }
}