package com.example.bestmlawi.ui.orders;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ✅ Imports nécessaires
import com.example.bestmlawi.ui.orders.Order;
import android.util.Log;

public class Consultation_orders extends Activity {

    private ListView lstOrders;
    private TextView txtTitle, txtOpenFilters;
    private EditText edtSearch;

    private ArrayAdapter<String> adpOrders;
    private FirebaseFirestore db;
    private List<Order> orderList = new ArrayList<>();
    private List<String> orderStringList = new ArrayList<>();

    // Pour afficher les détails des plats
    private Map<String, MenuItem> menuItemsDetail = new HashMap<>();
    private ArrayAdapter<OrderLineItem> orderItemsAdapter;

    // 🔍 Filtres sélectionnés
    private String selectedStatus = "";           // ex: "Delivered"
    private String selectedSalesPoint = "";       // ex: "Available"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consultation_orders);

        initialiser();
        ecouteurs();
        remplir();
        ecouteursRecherche(); // Rechercher en temps réel
    }

    private void initialiser() {
        lstOrders = findViewById(R.id.lstOrders);
        txtTitle = findViewById(R.id.txtTitle);
        txtOpenFilters = findViewById(R.id.txtOpenFilters);
        edtSearch = findViewById(R.id.edtSearch);

        orderStringList = new ArrayList<>();
        adpOrders = new ArrayAdapter<>(this, 0, orderStringList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_order, parent, false);
                }

                String item = getItem(position);
                TextView txtOrderInfo = convertView.findViewById(R.id.txtOrderInfo);
                Button btnViewDetail = convertView.findViewById(R.id.btnViewDetail);
                Button btnCancel = convertView.findViewById(R.id.btnCancel);
                Button btnAccept = convertView.findViewById(R.id.btnAccept);

                txtOrderInfo.setText(item);

                final Order order;
                if (position < orderList.size()) {
                    order = orderList.get(position);
                } else {
                    order = null;
                }

                btnViewDetail.setOnClickListener(v -> {
                    if (order != null) showOrderDialog(order);
                });

                btnCancel.setOnClickListener(v -> {
                    if (order != null && !"Livré".equals(order.getStatus())) {
                        showDeleteConfirmationDialog(order);
                    }
                });

                btnAccept.setOnClickListener(v -> {
                    if (order != null) {
                        Intent intent = new Intent(Consultation_orders.this, AssignOrderActivity.class);
                        intent.putExtra("ORDER_ID", order.getId());
                        intent.putExtra("DOCUMENT_ID", order.getId());
                        startActivityForResult(intent, 1001);
                    }
                });

                return convertView;
            }
        };
        lstOrders.setAdapter(adpOrders);
        db = FirebaseFirestore.getInstance();
    }

    private void ecouteurs() {
        txtOpenFilters.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilterOrdersActivity.class);
            startActivityForResult(intent, 1002);
        });
    }

    private void ecouteursRecherche() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrerOrders();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchMenuItemForOrderLineItem(OrderLineItem item) {
        if (item == null || item.getMenuItemId() == "" || item.getId() == null) {
            Log.w("Consultation_orders", "Invalid OrderLineItem: " + item);
            if (item != null && item.getId() != null) {
                menuItemsDetail.put(item.getId(), null);
            }
            return;
        }

        String menuItemIdStr = String.valueOf(item.getMenuItemId());

        db.collection("menu_items")
                .document(menuItemIdStr)
                .get()
                .addOnCompleteListener(task -> {
                    MenuItem menuItem = null;
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.exists()) {
                            try {
                                Map<String, Object> data = doc.getData();
                                if (data != null) {
                                    String name = safeString(data.get("name"), "Inconnu");
                                    double price = safeDouble(data.get("price"));
                                    String imageUrl = safeString(data.get("imageUrl"), "");
                                    String category = safeString(data.get("category"), "");
                                    double rating = safeDouble(data.get("rating"));
                                    String description = safeString(data.get("description"), "");
                                    String ingredients = safeString(data.get("ingredients"), "");

                                    menuItem = new MenuItem(
                                            doc.getId(),
                                            name,
                                            price,
                                            imageUrl,
                                            category,
                                            rating,
                                            description,
                                            ingredients
                                    );
                                }
                            } catch (Exception e) {
                                Log.e("Consultation_orders", "Erreur création MenuItem ID: " + menuItemIdStr, e);
                            }
                        } else {
                            Log.w("Consultation_orders", "Plat non trouvé: " + menuItemIdStr);
                        }
                    } else {
                        Log.e("Consultation_orders", "Erreur Firestore pour plat: " + menuItemIdStr, task.getException());
                    }

                    menuItemsDetail.put(item.getId(), menuItem);

                    runOnUiThread(() -> {
                        if (orderItemsAdapter != null) {
                            orderItemsAdapter.notifyDataSetChanged();
                        }
                    });
                });
    }

    private String safeString(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }

    private double safeDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private void showOrderDialog(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.order_dialog, null);
        builder.setView(dialogView);

        TextView txtOrderId = dialogView.findViewById(R.id.txtDialogOrderId);
        TextView txtCustomerName = dialogView.findViewById(R.id.txtDialogCustomerName);
        TextView txtTotalAmount = dialogView.findViewById(R.id.txtDialogTotalAmount);
        TextView txtStatus = dialogView.findViewById(R.id.txtDialogStatus);
        TextView txtTimestamp = dialogView.findViewById(R.id.txtDialogTimestamp);
        Button btnMarkReady = dialogView.findViewById(R.id.btnMarkReady);
        Button btnMarkDelivered = dialogView.findViewById(R.id.btnMarkDelivered);
        Button btnDelete = dialogView.findViewById(R.id.btnDeleteOrder);
        ListView lstOrderItems = dialogView.findViewById(R.id.lstOrderItems);

        txtOrderId.setText("Commande #" + order.getId());
        txtCustomerName.setText(order.getAddress());
        txtTotalAmount.setText("N/A");
        txtStatus.setText(order.getStatus());

        if (order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);
            txtTimestamp.setText(sdf.format(order.getOrderDate()));
        } else {
            txtTimestamp.setText("Date inconnue");
        }

        loadOrderItems(order.getId(), lstOrderItems);

        boolean isDelivered = "Livré".equals(order.getStatus()) || "Delivered".equals(order.getStatus());
        boolean isReady = "Prêt".equals(order.getStatus()) || "Ready".equals(order.getStatus());

        if (isDelivered) {
            btnMarkReady.setEnabled(false);
            btnMarkDelivered.setEnabled(false);
            btnDelete.setEnabled(false);
        } else if (isReady) {
            btnMarkReady.setEnabled(false);
            btnMarkDelivered.setVisibility(View.VISIBLE);
            btnMarkDelivered.setEnabled(true);
        } else {
            btnMarkReady.setVisibility(View.VISIBLE);
            btnMarkDelivered.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnMarkReady.setOnClickListener(v -> {
            dialog.dismiss();
            updateOrderStatus(order, "Prêt");
        });

        btnMarkDelivered.setOnClickListener(v -> {
            dialog.dismiss();
            updateOrderStatus(order, "Livré");
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteConfirmationDialog(order);
        });
    }

    private void loadOrderItems(String orderId, ListView listView) {
        Log.d("Consultation_orders", "Chargement des articles pour orderId = " + orderId);

        List<OrderLineItem> items = new ArrayList<>();

        db.collection("order_line_items")
                .whereEqualTo("orderId", orderId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            OrderLineItem item = doc.toObject(OrderLineItem.class);
                            item.setId(doc.getId());
                            items.add(item);
                            fetchMenuItemForOrderLineItem(item);
                        }

                        orderItemsAdapter = new ArrayAdapter<>(this, R.layout.item_order_item, items) {
                            @NonNull
                            @Override
                            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                                if (convertView == null) {
                                    convertView = LayoutInflater.from(getContext())
                                            .inflate(R.layout.item_order_item, parent, false);
                                }

                                OrderLineItem item = getItem(position);
                                TextView txtItemName = convertView.findViewById(R.id.txtItemName);
                                TextView txtItemDescription = convertView.findViewById(R.id.txtItemDescription);
                                TextView txtItemQuantity = convertView.findViewById(R.id.txtItemQuantity);
                                TextView txtItemPrice = convertView.findViewById(R.id.txtItemPrice);

                                txtItemQuantity.setText("Qte : " + item.getQuantity());

                                MenuItem menuItem = menuItemsDetail.get(item.getId());
                                if (menuItem != null) {
                                    txtItemName.setText(menuItem.getName());
                                    txtItemDescription.setText(menuItem.getDescription());
                                    txtItemPrice.setText(String.format(Locale.FRANCE, "%.2f €", menuItem.getPrice()));
                                } else {
                                    txtItemName.setText("Chargement...");
                                }

                                return convertView;
                            }
                        };

                        listView.setAdapter(orderItemsAdapter);
                    } else {
                        Toast.makeText(this, "Erreur chargement: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showDeleteConfirmationDialog(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View deleteView = inflater.inflate(R.layout.suppression, null);
        builder.setView(deleteView);

        TextView txtDeleteTitle = deleteView.findViewById(R.id.txtDeleteTitle);
        TextView txtDeleteMessage = deleteView.findViewById(R.id.txtDeleteMessage);
        Button btnCancel = deleteView.findViewById(R.id.btnCancelDelete);
        Button btnConfirm = deleteView.findViewById(R.id.btnConfirmDelete);

        if (txtDeleteTitle != null) txtDeleteTitle.setText("Supprimer la commande");
        if (txtDeleteMessage != null) {
            txtDeleteMessage.setText("Êtes-vous sûr de vouloir supprimer la commande #" +
                    order.getId() + " ?\n\nCette action est irréversible !");
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            deleteOrder(order);
        });
    }

    private void updateOrderStatus(Order order, String newStatus) {
        db.collection("orders").document(order.getId())
                .update("status", newStatus)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Statut mis à jour", Toast.LENGTH_SHORT).show();
                        remplir();
                    } else {
                        Toast.makeText(this, "Erreur: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteOrder(Order order) {
        db.collection("orders").document(order.getId())
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Commande supprimée", Toast.LENGTH_SHORT).show();
                        remplir();
                    } else {
                        Toast.makeText(this, "Erreur: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void remplir() {
        db.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        orderList.clear();
                        orderStringList.clear();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Order order = doc.toObject(Order.class);
                            order.setId(doc.getId());
                            orderList.add(order);
                        }

                        filtrerOrders(); // Appliquer filtres actuels
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Erreur Firebase: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Méthode de filtrage identique à filtrerEmployees()
     */
    private void filtrerOrders() {
        String searchText = edtSearch.getText().toString().toLowerCase().trim();

        orderStringList.clear();

        for (Order order : orderList) {
            // 🔍 Filtre par recherche (ID, adresse, statut)
            boolean matchesSearch = searchText.isEmpty() ||
                    order.getId().toLowerCase().contains(searchText) ||
                    (order.getAddress() != null && order.getAddress().toLowerCase().contains(searchText)) ||
                    (order.getStatus() != null && order.getStatus().toLowerCase().contains(searchText));

            // 🏷️ Filtre par statut
            boolean matchesStatus = selectedStatus.isEmpty() ||
                    (order.getStatus() != null && order.getStatus().equals(selectedStatus));

            // 📍 Filtre par point de vente
            boolean matchesSalesPoint = selectedSalesPoint.isEmpty() ||
                    (order.getSales_point_id() != null &&
                            order.getSales_point_id().contains(selectedSalesPoint));

            if (matchesSearch && matchesStatus && matchesSalesPoint) {
                String displayText = "Cmd #" + order.getId() +
                        " - " + order.getAddress() +
                        " (" + order.getStatus() + ")";
                if (order.getOrderDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.FRANCE);
                    displayText += " • " + sdf.format(order.getOrderDate());
                }
                orderStringList.add(displayText);
            }
        }

        adpOrders.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            String message = data.getStringExtra("MESSAGE");
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            remplir();
        }

        else if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            // Mettre à jour les filtres sélectionnés
            selectedStatus = data.getStringExtra("FILTER_STATUS") != null ?
                    data.getStringExtra("FILTER_STATUS") : "";

            selectedSalesPoint = data.getStringExtra("FILTER_SALES_POINT_FILTER") != null ?
                    data.getStringExtra("FILTER_SALES_POINT_FILTER") : "";

            // Ne pas utiliser min/max price ici (pas de totalAmount dans Order)
            filtrerOrders(); // 🔥 Appliquer immédiatement les filtres
        }
    }
}