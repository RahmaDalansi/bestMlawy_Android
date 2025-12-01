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
import java.util.Collections;
import java.util.List;

public class DeliveryOrdersFragment extends Fragment {

    private RecyclerView ordersRecyclerView;
    private DeliveryOrderAdapter orderAdapter;
    private List<Order> orderList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState, tvErrorState;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_orders, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        orderList = new ArrayList<>();

        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        loadDeliveryOrders();

        return view;
    }

    private void initializeViews(View view) {
        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvErrorState = view.findViewById(R.id.tv_error_state);

        // Masquer tous les états au début
        showLoadingState();
    }

    private void setupRecyclerView() {
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new DeliveryOrderAdapter(orderList, new DeliveryOrderAdapter.OnOrderClickListener() {
            @Override
            public void onOrderClick(Order order) {
                showOrderDetails(order);
            }

            @Override
            public void onAcceptOrder(Order order) {
                acceptOrder(order);
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
        ordersRecyclerView.setAdapter(orderAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDeliveryOrders();
        });
    }

    private void loadDeliveryOrders() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showErrorState("Utilisateur non connecté");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String userId = currentUser.getUid();

        db.collection("orders")
                .whereEqualTo("deliverId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (task.isSuccessful()) {
                        orderList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = document.toObject(Order.class);
                            order.setOrderId(document.getId());
                            orderList.add(order);

                            // DEBUG
                            System.out.println("DEBUG - Commande: " + order.getOrderId() +
                                    ", DeliverId: " + order.getDeliverId() + // ← Maintenant getDeliverId()
                                    ", Status: " + order.getStatus());
                        }

                        // Trier manuellement par orderDate
                        Collections.sort(orderList, (o1, o2) -> {
                            if (o1.getOrderDate() != null && o2.getOrderDate() != null) {
                                return o2.getOrderDate().compareTo(o1.getOrderDate());
                            }
                            return 0;
                        });

                        orderAdapter.notifyDataSetChanged();

                        if (orderList.isEmpty()) {
                            showEmptyState();
                        } else {
                            showDataState();
                        }
                    } else {
                        String errorMessage = "Erreur de chargement: " +
                                (task.getException() != null ?
                                        task.getException().getMessage() : "Erreur inconnue");
                        showErrorState(errorMessage);
                    }
                });
    }

    // Méthodes pour gérer les différents états d'affichage
    private void showLoadingState() {
        ordersRecyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvErrorState.setVisibility(View.GONE);
        // Le SwipeRefreshLayout montre l'indicateur de chargement
    }

    private void showDataState() {
        ordersRecyclerView.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        tvErrorState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        ordersRecyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvErrorState.setVisibility(View.GONE);
        tvEmptyState.setText("Aucune commande assignée");
    }

    private void showErrorState(String errorMessage) {
        ordersRecyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvErrorState.setVisibility(View.VISIBLE);
        tvErrorState.setText(errorMessage);
    }

    private void showOrderDetails(Order order) {
        // Ouvrir les détails de la commande
        // Intent intent = new Intent(getActivity(), OrderDetailsActivity.class);
        // intent.putExtra("order_id", order.getOrderId());
        // startActivity(intent);
    }

    private void acceptOrder(Order order) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("orders").document(order.getOrderId())
                    .update(
                            "status", "assigned",
                            "deliveryPersonId", currentUser.getUid(),
                            "deliveryPersonName", currentUser.getDisplayName()
                    )
                    .addOnSuccessListener(aVoid -> {
                        loadDeliveryOrders(); // Recharger la liste
                    })
                    .addOnFailureListener(e -> {
                        showErrorState("Erreur d'acceptation: " + e.getMessage());
                    });
        }
    }

    private void startDelivery(Order order) {
        db.collection("orders").document(order.getOrderId())
                .update("status", "in_progress")
                .addOnSuccessListener(aVoid -> {
                    loadDeliveryOrders(); // Recharger la liste
                })
                .addOnFailureListener(e -> {
                    showErrorState("Erreur de démarrage: " + e.getMessage());
                });
    }

    private void completeDelivery(Order order) {
        db.collection("orders").document(order.getOrderId())
                .update(
                        "status", "delivered",
                        "deliveryDate", new java.util.Date()
                )
                .addOnSuccessListener(aVoid -> {
                    // Mettre à jour les statistiques du livreur
                    updateDeliveryStats();
                    loadDeliveryOrders(); // Recharger la liste
                })
                .addOnFailureListener(e -> {
                    showErrorState("Erreur de livraison: " + e.getMessage());
                });
    }

    private void updateDeliveryStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Logique pour mettre à jour les statistiques
            // db.collection("delivery_stats").document(currentUser.getUid())...
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDeliveryOrders(); // Recharger à chaque retour sur le fragment
    }
}