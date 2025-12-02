package com.example.bestmlawi.ui.delivery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.bestmlawi.R;
import com.example.bestmlawi.adapters.DeliveryOrderAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActiveDeliveryFragment extends Fragment {

    private RecyclerView activeOrdersRecyclerView;
    private DeliveryOrderAdapter orderAdapter;
    private List<Order> activeOrderList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvEmptyState, tvActiveCount;
    private SwipeRefreshLayout swipeRefreshLayout;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_delivery, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        activeOrderList = new ArrayList<>();

        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        loadActiveDeliveries();

        return view;
    }

    private void initializeViews(View view) {
        activeOrdersRecyclerView = view.findViewById(R.id.active_orders_recycler_view);
        tvEmptyState = view.findViewById(R.id.tv_empty_state_active);
        tvActiveCount = view.findViewById(R.id.tv_active_count);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout_active);

        // CORRECTION : Ne pas cacher tvActiveCount au début
        // Juste initialiser le texte
        if (tvActiveCount != null) {
            tvActiveCount.setText("Chargement...");
        }
    }

    private void setupRecyclerView() {
        activeOrdersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new DeliveryOrderAdapter(activeOrderList, new DeliveryOrderAdapter.OnOrderClickListener() {
            @Override
            public void onOrderClick(Order order) {
                showOrderDetails(order);
            }

            @Override
            public void onAcceptOrder(Order order) {
                // Non utilisé dans ce fragment
            }

            @Override
            public void onStartDelivery(Order order) {
                startDelivery(order);
            }

            @Override
            public void onCompleteDelivery(Order order) {
                completeDelivery(order);
            }
        });
        activeOrdersRecyclerView.setAdapter(orderAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadActiveDeliveries();
        });
    }

    private void loadActiveDeliveries() {
        swipeRefreshLayout.setRefreshing(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showErrorState("Veuillez vous connecter");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String userId = currentUser.getUid();

        System.out.println("DEBUG - Chargement des livraisons actives pour: " + userId);

        // CORRECTION : Amélioration de la requête
        db.collection("orders")
                .whereEqualTo("deliverId", userId)
                .whereIn("status", Arrays.asList("assigned", "in_progress", "Prêt", "En cours"))
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        activeOrderList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // CORRECTION : Utiliser document.toObject() pour mapper automatiquement
                                Order order = document.toObject(Order.class);

                                // S'assurer que l'ID est défini
                                if (order != null) {
                                    order.setOrderId(document.getId());

                                    // S'assurer que les champs sont correctement mappés
                                    if (order.getCustomerName() == null || order.getCustomerName().isEmpty()) {
                                        String deliveryName = document.getString("deliveryName");
                                        order.setCustomerName(deliveryName != null ? deliveryName : "Client");
                                    }

                                    if (order.getDeliveryAddress() == null || order.getDeliveryAddress().isEmpty()) {
                                        String address = document.getString("address");
                                        order.setDeliveryAddress(address != null ? address : "Adresse inconnue");
                                    }

                                    // Debug
                                    System.out.println("DEBUG - Commande active: " +
                                            order.getOrderId() + " - " + order.getStatus() +
                                            " - " + order.getCustomerName());

                                    activeOrderList.add(order);
                                }
                            } catch (Exception e) {
                                System.out.println("Erreur parsing commande: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        // CORRECTION : Trier manuellement par date
                        java.util.Collections.sort(activeOrderList, (o1, o2) -> {
                            if (o1.getOrderDate() != null && o2.getOrderDate() != null) {
                                return o2.getOrderDate().compareTo(o1.getOrderDate());
                            }
                            return 0;
                        });

                        orderAdapter.notifyDataSetChanged();
                        updateUI();

                        System.out.println("DEBUG - Total commandes actives: " + activeOrderList.size());

                    } else {
                        String errorMsg = "Erreur de chargement";
                        if (task.getException() != null) {
                            errorMsg += ": " + task.getException().getMessage();
                            task.getException().printStackTrace();
                        }
                        showErrorState(errorMsg);
                    }
                });
    }

    private void updateUI() {
        int count = activeOrderList.size();

        // Mettre à jour le compteur
        if (tvActiveCount != null) {
            tvActiveCount.setText(count + " livraison(s) active(s)");
        }

        // Gérer l'état vide/rempli
        if (count == 0) {
            tvEmptyState.setText("Aucune livraison active\n\nLes commandes en cours apparaîtront ici.");
            tvEmptyState.setVisibility(View.VISIBLE);
            activeOrdersRecyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            activeOrdersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorState(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        activeOrdersRecyclerView.setVisibility(View.GONE);
    }

    private void showOrderDetails(Order order) {
        // À implémenter
        android.widget.Toast.makeText(
                getContext(),
                "Détails: " + order.getCustomerName(),
                android.widget.Toast.LENGTH_SHORT
        ).show();
    }

    private void startDelivery(Order order) {
        String newStatus = "En cours";

        db.collection("orders").document(order.getOrderId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    // Recharger
                    loadActiveDeliveries();

                    android.widget.Toast.makeText(
                            getContext(),
                            "Livraison commencée!",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(
                            getContext(),
                            "Erreur: " + e.getMessage(),
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void completeDelivery(Order order) {
        db.collection("orders").document(order.getOrderId())
                .update(
                        "status", "Livré",
                        "deliveryDate", new java.util.Date()
                )
                .addOnSuccessListener(aVoid -> {
                    updateDeliveryStats();
                    loadActiveDeliveries();

                    android.widget.Toast.makeText(
                            getContext(),
                            "Livraison terminée!",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(
                            getContext(),
                            "Erreur: " + e.getMessage(),
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void updateDeliveryStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("delivery_stats").document(userId)
                .update("completedDeliveries",
                        com.google.firebase.firestore.FieldValue.increment(1))
                .addOnFailureListener(e -> {
                    // Créer si n'existe pas
                    java.util.Map<String, Object> stats = new java.util.HashMap<>();
                    stats.put("userId", userId);
                    stats.put("completedDeliveries", 1);
                    stats.put("lastUpdated", new java.util.Date());

                    db.collection("delivery_stats").document(userId)
                            .set(stats, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadActiveDeliveries();
    }
}