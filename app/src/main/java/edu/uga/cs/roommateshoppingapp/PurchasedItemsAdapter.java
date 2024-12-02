package edu.uga.cs.roommateshoppingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchasedItemsAdapter extends RecyclerView.Adapter<PurchasedItemsAdapter.ViewHolder> {
    private final List<PurchaseGroup> purchaseGroups;
    private final SimpleDateFormat dateFormat;
    private final OnPurchaseItemActionListener listener;

    public interface OnPurchaseItemActionListener {
        void onRemoveFromPurchase(PurchaseGroup group, ShoppingItem item);
        void onUpdatePrice(PurchaseGroup group);
    }

    public PurchasedItemsAdapter(List<PurchaseGroup> purchaseGroups, OnPurchaseItemActionListener listener) {
        this.purchaseGroups = purchaseGroups;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchase_group, parent, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PurchaseGroup group = purchaseGroups.get(position);
        Context context = holder.itemView.getContext();

        holder.purchaseDate.setText(dateFormat.format(new Date(group.getTimestamp())));
        holder.purchasedBy.setText(String.format(context.getString(R.string.purchased_by_format),
                group.getPurchasedBy()));
        holder.totalPrice.setText(String.format(Locale.US, context.getString(R.string.price_format),
                group.getTotalPrice()));

        // Add click listener to price for editing
        holder.totalPrice.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdatePrice(group);
            }
        });








        // Clear previous items
        holder.itemsContainer.removeAllViews();

        // Add each item with a remove button
        for (ShoppingItem item : group.getItems()) {
            View itemView = LayoutInflater.from(context)
                    .inflate(R.layout.item_purchased_items, holder.itemsContainer, false);
            TextView itemName = itemView.findViewById(R.id.purchasedItemName);
            ImageButton removeButton = itemView.findViewById(R.id.removeFromPurchaseButton);

            itemName.setText(item.getItemName());

            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveFromPurchase(group, item);
                }
            });

            holder.itemsContainer.addView(itemView);
        }
    }

    @Override
    public int getItemCount() {
        return purchaseGroups.size();
    }







    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView purchaseDate;
        final TextView purchasedBy;
        final TextView totalPrice;
        final ViewGroup itemsContainer;

        ViewHolder(View view) {
            super(view);
            purchaseDate = view.findViewById(R.id.purchaseDate);
            purchasedBy = view.findViewById(R.id.purchasedBy);
            totalPrice = view.findViewById(R.id.totalPrice);
            itemsContainer = view.findViewById(R.id.itemsContainer);
        }
    }

    public void updatePurchases(List<PurchaseGroup> newPurchases) {
        purchaseGroups.clear();
        purchaseGroups.addAll(newPurchases);
        notifyDataSetChanged();
    }
}