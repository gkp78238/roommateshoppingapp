package edu.uga.cs.roommateshoppingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;
import java.util.stream.Collectors;

import android.text.TextWatcher;

public class HomeActivity extends AppCompatActivity implements ShoppingItemAdapter.OnSelectionModeChangeListener {
    private Button logoutButton;
    private Button settleButton;
    private Button toggleViewButton;
    private Button viewBasketButton;
    private FloatingActionButton addItemButton;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private RecyclerView recyclerView;
    private ShoppingItemAdapter adapter;
    private List<ShoppingItem> shoppingItems;
    private boolean showingPurchased = false;
    private ShoppingBasket basket;
    private AlertDialog basketDialog;
    private BasketItemAdapter basketAdapter;
    private TextView viewTitle;


    // State preservation keys
    private static final String KEY_SHOWING_PURCHASED = "showing_purchased";
    private static final String KEY_SELECTED_ITEMS = "selected_items";
    private static final String KEY_BASKET_DIALOG_SHOWING = "basket_dialog_showing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("shopping_items");
        basket = ShoppingBasket.getInstance();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        if (savedInstanceState != null) {
            showingPurchased = savedInstanceState.getBoolean(KEY_SHOWING_PURCHASED, false);
            if (showingPurchased) {
                toggleViewButton.setText(R.string.view_shopping_list);
                settleButton.setText(R.string.settle_costs);
                addItemButton.setVisibility(View.GONE);
                viewBasketButton.setVisibility(View.GONE);
                loadPurchasedItems();
            } else {
                loadShoppingItems();
            }

            // Restore selected items if any
            ArrayList<String> selectedItemIds = savedInstanceState.getStringArrayList(KEY_SELECTED_ITEMS);
            if (selectedItemIds != null && !selectedItemIds.isEmpty()) {
                adapter.restoreSelectedItems(selectedItemIds);
            }

            // Restore basket dialog if it was showing
            if (savedInstanceState.getBoolean(KEY_BASKET_DIALOG_SHOWING, false)) {
                showBasketDialog();
            }
        } else {
            loadShoppingItems();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SHOWING_PURCHASED, showingPurchased);

        // Save selected items
        if (adapter != null && !adapter.getSelectedItems().isEmpty()) {
            ArrayList<String> selectedItemIds = new ArrayList<>();
            for (ShoppingItem item : adapter.getSelectedItems()) {
                selectedItemIds.add(item.getId());
            }
            outState.putStringArrayList(KEY_SELECTED_ITEMS, selectedItemIds);
        }

        // Save basket dialog state
        outState.putBoolean(KEY_BASKET_DIALOG_SHOWING, basketDialog != null && basketDialog.isShowing());
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (recyclerView != null) {
            recyclerView.requestLayout();
        }

        if (basketDialog != null && basketDialog.isShowing()) {
            basketDialog.dismiss();
            showBasketDialog();
        }
    }


    private void initializeViews() {
        viewTitle = findViewById(R.id.viewTitle);
        logoutButton = findViewById(R.id.logoutButton);
        settleButton = findViewById(R.id.settleButton);
        toggleViewButton = findViewById(R.id.toggleViewButton);
        viewBasketButton = findViewById(R.id.viewBasketButton);
        addItemButton = findViewById(R.id.addItemButton);
        recyclerView = findViewById(R.id.recyclerView);

        // Initially hide settle button since we start in shopping list view
        settleButton.setVisibility(View.GONE);

        updateBasketButtonVisibility();
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            shoppingItems = new ArrayList<>();
            adapter = new ShoppingItemAdapter(databaseRef, mAuth, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        } else {
            Toast.makeText(this, R.string.error_setup_view, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        });

        addItemButton.setOnClickListener(v -> showAddItemDialog());
        toggleViewButton.setOnClickListener(v -> toggleView());
        viewBasketButton.setOnClickListener(v -> showBasketDialog());

        settleButton.setOnClickListener(v -> {
            if (!showingPurchased && !adapter.getSelectedItems().isEmpty()) {
                // In shopping list with selections - add to basket
                Set<ShoppingItem> selectedItems = adapter.getSelectedItems();
                basket.clearBasket();
                basket.addAllToBasket(selectedItems);

                for (ShoppingItem item : selectedItems) {
                    item.setInBasket(true);
                    databaseRef.child(item.getId()).setValue(item);
                }

                updateBasketButtonVisibility();
                if (basketAdapter != null) {
                    basketAdapter.updateItems(new ArrayList<>(basket.getBasketItems()));
                }
                adapter.exitSelectionMode();
            } else if (showingPurchased) {
                // In purchased items view - show settlement
                showSettlementDialog();
            }
        });
    }

    private void toggleView() {
        showingPurchased = !showingPurchased;
        if (showingPurchased) {
            viewTitle.setText("Purchased Items");
            toggleViewButton.setText(R.string.view_shopping_list);
            settleButton.setText(R.string.settle_costs);
            settleButton.setVisibility(View.VISIBLE);  // Show settle button
            addItemButton.setVisibility(View.GONE);
            viewBasketButton.setVisibility(View.GONE);
            loadPurchasedItems();
        } else {
            viewTitle.setText("Shopping List");
            toggleViewButton.setText(R.string.view_purchased_items);
            settleButton.setVisibility(View.GONE);     // Hide settle button
            addItemButton.setVisibility(View.VISIBLE);
            updateBasketButtonVisibility();
            setupRecyclerView();
            loadShoppingItems();
        }
    }

    private void loadPurchasedItems() {
        DatabaseReference purchasesRef = FirebaseDatabase.getInstance().getReference("purchases");
        purchasesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<PurchaseGroup> purchaseGroups = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    PurchaseGroup group = snapshot.getValue(PurchaseGroup.class);
                    if (group != null) {
                        group.setId(snapshot.getKey());
                        purchaseGroups.add(group);
                    }
                }
                displayPurchasedItems(purchaseGroups);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load purchases", Toast.LENGTH_SHORT).show();
            }
        });
    }






    private void displayPurchasedItems(List<PurchaseGroup> purchaseGroups) {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        PurchasedItemsAdapter purchasedAdapter = new PurchasedItemsAdapter(purchaseGroups,
                new PurchasedItemsAdapter.OnPurchaseItemActionListener() {
                    @Override
                    public void onRemoveFromPurchase(PurchaseGroup group, ShoppingItem item) {
                        group.getItems().remove(item);

                        if (group.getItems().isEmpty()) {
                            DatabaseReference purchasesRef = FirebaseDatabase.getInstance()
                                    .getReference("purchases");
                            purchasesRef.child(group.getId()).removeValue();
                        } else {
                            DatabaseReference purchasesRef = FirebaseDatabase.getInstance()
                                    .getReference("purchases");
                            purchasesRef.child(group.getId()).setValue(group);
                        }

                        item.setPurchased(false);
                        item.setInBasket(false);
                        item.setPrice(0);
                        item.setPurchasedBy("");
                        String key = databaseRef.push().getKey();
                        if (key != null) {
                            item.setId(key);
                            databaseRef.child(key).setValue(item)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(HomeActivity.this,
                                                "Item returned to shopping list",
                                                Toast.LENGTH_SHORT).show();
                                        loadPurchasedItems();
                                    });
                        }
                    }

                    @Override
                    public void onUpdatePrice(PurchaseGroup group) {
                        showPriceUpdateDialog(group);
                    }
                });

        recyclerView.setAdapter(purchasedAdapter);
    }

    private void showPriceUpdateDialog(PurchaseGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_update_price, null);
        EditText priceInput = view.findViewById(R.id.priceInput);

        priceInput.setText(String.format(Locale.US, "%.2f", group.getTotalPrice()));

        builder.setView(view)
                .setTitle(R.string.update_purchase_price)
                .setPositiveButton(R.string.update, (dialog, id) -> {
                    String priceStr = priceInput.getText().toString();
                    if (!priceStr.isEmpty()) {
                        try {
                            double newPrice = Double.parseDouble(priceStr);
                            DatabaseReference purchasesRef = FirebaseDatabase.getInstance()
                                    .getReference("purchases");
                            group.setTotalPrice(newPrice);
                            purchasesRef.child(group.getId()).setValue(group)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, R.string.price_update_success,
                                                Toast.LENGTH_SHORT).show();
                                        loadPurchasedItems();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this,
                                            R.string.price_update_failed, Toast.LENGTH_SHORT).show());
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.invalid_price_format,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }







    private void updateList() {
        List<ShoppingItem> filteredItems = showingPurchased ?
                shoppingItems.stream()
                        .filter(ShoppingItem::isPurchased)
                        .collect(Collectors.toList()) :
                shoppingItems.stream()
                        .filter(item -> !item.isPurchased() && !item.isInBasket())
                        .collect(Collectors.toList());
        adapter.updateItems(filteredItems);
    }

    private void loadShoppingItems() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                shoppingItems.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ShoppingItem item = snapshot.getValue(ShoppingItem.class);
                    if (item != null) {
                        item.setId(snapshot.getKey());
                        shoppingItems.add(item);
                    }
                }
                updateList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HomeActivity.this, R.string.error_load_items, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null);
        EditText input = view.findViewById(R.id.editItemName);
        TextView errorText = view.findViewById(R.id.errorText);

        builder.setView(view)
                .setTitle(R.string.add_item)
                .setPositiveButton(R.string.add, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view1 -> {
                String itemName = input.getText().toString().trim();

                if (itemName.isEmpty()) {
                    errorText.setText(R.string.error_empty_item);
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                boolean isDuplicate = shoppingItems.stream()
                        .filter(item -> !item.isPurchased())
                        .anyMatch(item -> item.getItemName().equalsIgnoreCase(itemName));

                if (isDuplicate) {
                    errorText.setText(R.string.error_duplicate_item);
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                addShoppingItem(itemName);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void addShoppingItem(String itemName) {
        ShoppingItem newItem = new ShoppingItem(itemName);
        String key = databaseRef.push().getKey();
        if (key != null) {
            newItem.setId(key);
            databaseRef.child(key).setValue(newItem)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.item_added_success,
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, R.string.error_add_item,
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void showSettlementDialog() {
        DatabaseReference purchasesRef = FirebaseDatabase.getInstance().getReference("purchases");
        purchasesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(HomeActivity.this, R.string.no_purchased_items, Toast.LENGTH_SHORT).show();
                    return;
                }

                List<PurchaseGroup> purchaseGroups = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    PurchaseGroup group = snapshot.getValue(PurchaseGroup.class);
                    if (group != null) {
                        purchaseGroups.add(group);
                    }
                }

                Settlement settlement = new Settlement(purchaseGroups);
                showSettlementDetailsDialog(settlement, purchasesRef);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load purchases", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showSettlementDetailsDialog(Settlement settlement, DatabaseReference purchasesRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settlement, null);

        TextView totalCostText = view.findViewById(R.id.totalCostText);
        TextView avgCostText = view.findViewById(R.id.avgCostText);
        LinearLayout spendingBreakdownContainer = view.findViewById(R.id.spendingBreakdownContainer);
        TextView settlementSummaryText = view.findViewById(R.id.settlementSummaryText);

        totalCostText.setText(String.format(Locale.US, "Total Cost: $%.2f", settlement.getTotalCost()));
        avgCostText.setText(String.format(Locale.US, "Average Cost per Roommate: $%.2f",
                settlement.getAverageCost()));

        spendingBreakdownContainer.removeAllViews();
        for (Map.Entry<String, Double> entry : settlement.getSpendingByRoommate().entrySet()) {
            TextView roommateText = new TextView(this);
            roommateText.setText(String.format(Locale.US, "%s spent: $%.2f",
                    entry.getKey(), entry.getValue()));
            roommateText.setPadding(0, 4, 0, 4);
            spendingBreakdownContainer.addView(roommateText);
        }

        StringBuilder summaryText = new StringBuilder("\nWho Owes What:\n\n");
        Map<String, Double> differences = settlement.getDifferences();
        for (Map.Entry<String, Double> entry : differences.entrySet()) {
            String roommate = entry.getKey();
            double difference = entry.getValue();

            if (Math.abs(difference) > 0.01) {
                if (difference < 0) {
                    summaryText.append(String.format(Locale.US, "%s owes: $%.2f\n",
                            roommate, -difference));
                } else {
                    summaryText.append(String.format(Locale.US, "%s is owed: $%.2f\n",
                            roommate, difference));
                }
            }
        }

        List<Settlement.Transaction> transactions = settlement.getTransactions();
        if (!transactions.isEmpty()) {
            summaryText.append("\nPayment Plan:\n");
            for (Settlement.Transaction transaction : transactions) {
                summaryText.append(String.format(Locale.US, "%s should pay %s $%.2f\n",
                        transaction.from, transaction.to, transaction.amount));
            }
        }

        settlementSummaryText.setText(summaryText.toString());

        builder.setView(view)
                .setTitle("Cost Settlement")
                .setPositiveButton("Complete Settlement & Clear Items", (dialog, id) -> {
                    purchasesRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "All purchases cleared", Toast.LENGTH_SHORT).show();
                                loadPurchasedItems();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Failed to clear purchases", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }





    private void showBatchPurchaseDialog(Set<ShoppingItem> selectedItems) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_batch_purchase, null);

        TextView itemsList = view.findViewById(R.id.selectedItemsList);
        EditText subtotalInput = view.findViewById(R.id.subtotalInput);
        TextView taxAmount = view.findViewById(R.id.taxAmount);
        TextView totalWithTax = view.findViewById(R.id.totalWithTax);

        StringBuilder items = new StringBuilder();
        for (ShoppingItem item : selectedItems) {
            items.append("• ").append(item.getItemName()).append("\n");
        }
        itemsList.setText(items.toString());

        subtotalInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double subtotal = s.length() > 0 ? Double.parseDouble(s.toString()) : 0;
                    double tax = subtotal * 0.07; // 7% tax
                    double total = subtotal + tax;

                    taxAmount.setText(String.format(Locale.US, "Tax (7%%): $%.2f", tax));
                    totalWithTax.setText(String.format(Locale.US, "Total with tax: $%.2f", total));
                } catch (NumberFormatException e) {
                    taxAmount.setText(R.string.tax_zero);
                    totalWithTax.setText(R.string.total_zero);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        builder.setView(view)
                .setTitle(R.string.purchase_selected_items)
                .setPositiveButton(R.string.purchase, (dialog, id) -> {
                    String subtotalStr = subtotalInput.getText().toString();
                    if (!subtotalStr.isEmpty()) {
                        try {
                            double subtotal = Double.parseDouble(subtotalStr);
                            double tax = subtotal * 0.07;
                            double total = subtotal + tax;
                            batchPurchaseItems(selectedItems, total);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.invalid_price, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void batchPurchaseItems(Set<ShoppingItem> items, double totalPrice) {
        String currentUser = mAuth.getCurrentUser().getEmail();
        PurchaseGroup purchaseGroup = new PurchaseGroup(
                new ArrayList<>(items),
                totalPrice,
                currentUser
        );

        DatabaseReference purchasesRef = FirebaseDatabase.getInstance().getReference("purchases");
        String purchaseId = purchasesRef.push().getKey();
        if (purchaseId != null) {
            purchaseGroup.setId(purchaseId);
            purchasesRef.child(purchaseId).setValue(purchaseGroup)
                    .addOnSuccessListener(aVoid -> {
                        for (ShoppingItem item : items) {
                            databaseRef.child(item.getId()).removeValue();
                        }
                        basket.clearBasket();
                        updateBasketButtonVisibility();
                        Toast.makeText(this, R.string.items_purchased_success, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Failed to complete purchase", Toast.LENGTH_SHORT).show());
        }
    }





    private void updateBasketButtonVisibility() {
        if (basket.hasItems()) {
            viewBasketButton.setVisibility(View.VISIBLE);
            viewBasketButton.setText(String.format(getString(R.string.view_basket_format),
                    basket.getItemCount()));
        } else {
            viewBasketButton.setVisibility(View.GONE);
        }
    }

    private void showBasketDialog() {
        if (basketDialog != null) {
            basketDialog.dismiss();
            basketDialog = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_shopping_basket, null);
        RecyclerView basketRecyclerView = view.findViewById(R.id.basketRecyclerView);
        Set<ShoppingItem> basketItems = basket.getBasketItems();

        basketAdapter = new BasketItemAdapter(new ArrayList<>(basketItems),
                item -> {
                    basket.removeFromBasket(item);
                    item.setInBasket(false);
                    databaseRef.child(item.getId()).setValue(item);

                    if (basketAdapter != null) {
                        basketAdapter.updateItems(new ArrayList<>(basket.getBasketItems()));
                    }
                    updateBasketButtonVisibility();

                    if (!basket.hasItems() && basketDialog != null && basketDialog.isShowing()) {
                        basketDialog.dismiss();
                    }
                });

        basketRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        basketRecyclerView.setAdapter(basketAdapter);

        basketDialog = builder
                .setView(view)
                .setTitle(R.string.shopping_basket)
                .setPositiveButton(R.string.proceed_to_checkout, (dialogInterface, i) -> {
                    showBatchPurchaseDialog(basket.getBasketItems());
                })
                .setNegativeButton(R.string.continue_shopping, null)
                .create();

        basketDialog.setOnDismissListener(dialog -> {
            basketDialog = null;
            basketAdapter = null;
        });

        if (!isFinishing()) {
            basketDialog.show();
        }
    }

    @Override
    public void onSelectionModeChanged(boolean isInSelectionMode, Set<ShoppingItem> selectedItems) {
        if (isInSelectionMode && selectedItems != null) {
            // When items are selected, show the settle button with "Purchase Selected" text
            settleButton.setVisibility(View.VISIBLE);
            settleButton.setText(String.format(Locale.US, "Purchase Selected Items (%d)",
                    selectedItems.size()));
        } else {
            // When no selection, hide the settle button in shopping list view
            if (!showingPurchased) {
                settleButton.setVisibility(View.GONE);
            } else {
                settleButton.setText(R.string.settle_costs);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (basketDialog != null) {
            basketDialog.dismiss();
            basketDialog = null;
        }
        basketAdapter = null;
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && !adapter.getSelectedItems().isEmpty()) {
            adapter.exitSelectionMode();
        } else if (showingPurchased) {
            toggleView();
        } else {
            super.onBackPressed();
        }
    }
}











