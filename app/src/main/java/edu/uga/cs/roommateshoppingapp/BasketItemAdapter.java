package edu.uga.cs.roommateshoppingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BasketItemAdapter extends RecyclerView.Adapter<BasketItemAdapter.ViewHolder> {
    private List<ShoppingItem> basketItems;
    private final OnBasketItemActionListener listener;

    public interface OnBasketItemActionListener {
        void onRemoveFromBasket(ShoppingItem item);
    }

    public BasketItemAdapter(List<ShoppingItem> basketItems, OnBasketItemActionListener listener) {
        this.basketItems = new ArrayList<>(basketItems);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_basket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = basketItems.get(position);
        holder.itemName.setText(item.getItemName());

        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                // First notify the listener
                listener.onRemoveFromBasket(item);
                // Then update our local list
                int pos = basketItems.indexOf(item);
                if (pos != -1) {
                    basketItems.remove(pos);
                    notifyItemRemoved(pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return basketItems.size();
    }

    public void updateItems(List<ShoppingItem> newItems) {
        basketItems = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView itemName;
        final ImageButton removeButton;

        ViewHolder(View view) {
            super(view);
            itemName = view.findViewById(R.id.basketItemName);
            removeButton = view.findViewById(R.id.removeFromBasketButton);
        }
    }
}