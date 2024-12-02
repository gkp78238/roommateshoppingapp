package edu.uga.cs.roommateshoppingapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;


public class Settlement {
    private double totalCost;
    private Map<String, Double> spendingByRoommate;
    private List<Transaction> transactions;
    private Map<String, Double> differences;

    public static class Transaction {
        public String from;
        public String to;
        public double amount;

        public Transaction(String from, String to, double amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }
    }

    public Settlement(List<PurchaseGroup> purchasedItems) {
        this.spendingByRoommate = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.differences = new HashMap<>();
        calculateTotalSpending(purchasedItems);
        calculateDifferences();
        calculateSettlement();
    }

    private void calculateTotalSpending(List<PurchaseGroup> purchasedItems) {
        totalCost = 0;
        for (PurchaseGroup group : purchasedItems) {
            double price = group.getTotalPrice();
            String purchaser = group.getPurchasedBy();
            totalCost += price;
            spendingByRoommate.put(purchaser,
                    spendingByRoommate.getOrDefault(purchaser, 0.0) + price);
        }
    }

    private void calculateDifferences() {
        double averageSpending = totalCost / spendingByRoommate.size();

        for (Map.Entry<String, Double> entry : spendingByRoommate.entrySet()) {
            double difference = entry.getValue() - averageSpending;
            // Round to 2 decimal places
            difference = Math.round(difference * 100.0) / 100.0;
            differences.put(entry.getKey(), difference);
        }
    }

    private void calculateSettlement() {
        if (spendingByRoommate.isEmpty()) return;

        double averageSpending = totalCost / spendingByRoommate.size();

        // Create lists of who owes and who is owed
        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : differences.entrySet()) {
            if (entry.getValue() < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), -entry.getValue()));
            } else if (entry.getValue() > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }

        // Match debtors with creditors
        for (Map.Entry<String, Double> debtor : debtors) {
            double debtRemaining = debtor.getValue();

            for (Map.Entry<String, Double> creditor : creditors) {
                if (creditor.getValue() <= 0) continue;

                double transferAmount = Math.min(debtRemaining, creditor.getValue());
                if (transferAmount > 0.01) {  // Only create transaction if amount is significant
                    transferAmount = Math.round(transferAmount * 100.0) / 100.0; // Round to 2 decimals
                    transactions.add(new Transaction(
                            debtor.getKey(),
                            creditor.getKey(),
                            transferAmount
                    ));

                    debtRemaining -= transferAmount;
                    creditor.setValue(creditor.getValue() - transferAmount);
                }

                if (debtRemaining <= 0.01) break;
            }
        }
    }


    public double getTotalCost() {
        return Math.round(totalCost * 100.0) / 100.0;
    }

    public double getAverageCost() {
        return Math.round((totalCost / spendingByRoommate.size()) * 100.0) / 100.0;
    }

    public Map<String, Double> getSpendingByRoommate() {
        return spendingByRoommate;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Map<String, Double> getDifferences() {
        return differences;
    }
}