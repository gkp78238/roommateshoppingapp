package edu.uga.cs.roommateshoppingapp;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

public class ShoppingItemAdapter extends RecyclerView.Adapter<ShoppingItemAdapter.ViewHolder> {
    private List<ShoppingItem> items;
    private final DatabaseReference databaseRef;
    private final FirebaseAuth mAuth;
    private boolean isSelectionMode = false;
    private final Set<ShoppingItem> selectedItems = new HashSet<>();
    private final OnSelectionModeChangeListener selectionModeListener;

    public interface OnSelectionModeChangeListener {
        void onSelectionModeChanged(boolean isInSelectionMode, Set<ShoppingItem> selectedItems);
    }

    public ShoppingItemAdapter(DatabaseReference databaseRef, FirebaseAuth mAuth, OnSelectionModeChangeListener listener) {
        this.items = new ArrayList<>();
        this.databaseRef = databaseRef;
        this.mAuth = mAuth;
        this.selectionModeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);
        holder.itemName.setText(item.getItemName());
        holder.checkbox.setVisibility(isSelectionMode && !item.isPurchased() ? View.VISIBLE : View.GONE);
        holder.checkbox.setChecked(selectedItems.contains(item));

        if (item.isPurchased()) {
            holder.price.setText(String.format(Locale.US, "$%.2f", item.getPrice()));
            holder.purchasedBy.setText(String.format("Bought by: %s", item.getPurchasedBy()));
            holder.purchasedBy.setVisibility(View.VISIBLE);
            holder.price.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        } else {
            holder.purchasedBy.setVisibility(View.GONE);
            holder.price.setVisibility(View.GONE);

            // Long press for selection mode
            holder.itemView.setOnLongClickListener(v -> {
                if (!isSelectionMode && !item.isPurchased()) {
                    startSelectionMode();
                    toggleSelection(item);
                    return true;
                }
                return false;
            });

            // Click handling
            holder.itemView.setOnClickListener(v -> {
                if (isSelectionMode) {
                    toggleSelection(item);
                } else {
                    showItemMenu(v, item);
                }
            });

            // Add checkbox click listener
            holder.checkbox.setOnClickListener(v -> {
                toggleSelection(item);
            });
        }
    }

    private void showItemMenu(View view, ShoppingItem item) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.inflate(R.menu.menu_main);

        popup.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_edit) {
                showEditDialog(view.getContext(), item);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteDialog(view.getContext(), item);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showEditDialog(android.content.Context context, ShoppingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_item, null);
        EditText editText = dialogView.findViewById(R.id.editItemName);
        editText.setText(item.getItemName());

        builder.setView(dialogView)
                .setTitle(R.string.edit_item)
                .setPositiveButton(R.string.save, (dialog, id) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        item.setItemName(newName);
                        databaseRef.child(item.getId()).setValue(item)
                                .addOnSuccessListener(aVoid -> Toast.makeText(context,
                                        R.string.item_updated_success, Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(context,
                                        R.string.error_update_item, Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteDialog(android.content.Context context, ShoppingItem item) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete_item)
                .setMessage(R.string.delete_item_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        databaseRef.child(item.getId()).removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context,
                                        R.string.item_deleted_success, Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(context,
                                        R.string.error_delete_item, Toast.LENGTH_SHORT).show()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void startSelectionMode() {
        isSelectionMode = true;
        notifyDataSetChanged();
        selectionModeListener.onSelectionModeChanged(true, selectedItems);
    }

    public void toggleSelection(ShoppingItem item) {
        if (!item.isPurchased()) {
            if (selectedItems.contains(item)) {
                selectedItems.remove(item);
            } else {
                selectedItems.add(item);
            }
            notifyDataSetChanged();

            if (selectionModeListener != null) {
                // Only notify about selection, don't add to basket
                Set<ShoppingItem> selectedItemsCopy = new HashSet<>(selectedItems);
                selectionModeListener.onSelectionModeChanged(isSelectionMode, selectedItemsCopy);
            }
        }
    }

    public void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
        selectionModeListener.onSelectionModeChanged(false, selectedItems);
    }

    public Set<ShoppingItem> getSelectedItems() {
        return new HashSet<>(selectedItems);
    }

    public void updateItems(List<ShoppingItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, price, purchasedBy;
        CheckBox checkbox;

        ViewHolder(View view) {
            super(view);
            itemName = view.findViewById(R.id.itemName);
            price = view.findViewById(R.id.price);
            purchasedBy = view.findViewById(R.id.purchasedBy);
            checkbox = view.findViewById(R.id.itemCheckbox);
        }
    }


    // Add this method to ShoppingItemAdapter class
    public void restoreSelectedItems(ArrayList<String> selectedItemIds) {
        selectedItems.clear();
        if (selectedItemIds != null && !selectedItemIds.isEmpty()) {
            for (ShoppingItem item : items) {
                if (selectedItemIds.contains(item.getId())) {
                    selectedItems.add(item);
                    isSelectionMode = true;
                }
            }
            if (!selectedItems.isEmpty()) {
                notifyDataSetChanged();
                if (selectionModeListener != null) {
                    selectionModeListener.onSelectionModeChanged(true, selectedItems);
                }
            }
        }
    }
}