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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
        loadUserOrders(); // Charger seulement les commandes de l'utilisateur connecté

        return view;
    }

    private void initializeViews(View view) {
        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvErrorState = view.findViewById(R.id.tv_error_state);

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
            loadUserOrders();
        });
    }

    private void loadUserOrders() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showErrorState("Veuillez vous connecter");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String userId = currentUser.getUid();

        System.out.println("DEBUG - Recherche des commandes pour deliverId: " + userId);

        // Chercher les commandes où deliverId = userId de l'utilisateur connecté
        db.collection("orders")
                .whereEqualTo("deliverId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (task.isSuccessful()) {
                        orderList.clear();
                        int count = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                count++;

                                // Récupérer les données du document
                                Order order = new Order();

                                // Définir l'ID du document
                                order.setOrderId(document.getId());

                                // Récupérer les champs de base
                                String deliveryName = document.getString("deliveryName");
                                String address = document.getString("address");
                                String status = document.getString("status");
                                String deliverId = document.getString("deliverId");
                                String salesPointId = document.getString("sales_point_id");
                                String userIdFromDb = document.getString("user_id");

                                // Définir les valeurs dans l'objet Order
                                order.setCustomerName(deliveryName != null ? deliveryName : "Nom inconnu");
                                order.setDeliveryAddress(address != null ? address : "Adresse inconnue");
                                order.setStatus(status != null ? status : "pending");
                                order.setDeliverId(deliverId);

                                // Stocker les autres infos si nécessaire
                                if (userIdFromDb != null) {
                                    order.setCustomerPhone("Client ID: " + userIdFromDb);
                                }

                                // Récupérer orderDate (timestamp Firestore)
                                Object orderDateObj = document.get("orderDate");
                                if (orderDateObj instanceof com.google.firebase.Timestamp) {
                                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) orderDateObj;
                                    order.setOrderDate(timestamp.toDate());
                                } else if (orderDateObj instanceof Date) {
                                    order.setOrderDate((Date) orderDateObj);
                                } else {
                                    order.setOrderDate(new Date());
                                }

                                // Debug logging pour chaque commande trouvée
                                System.out.println("DEBUG - Commande " + count + " trouvée:");
                                System.out.println("  ID: " + document.getId());
                                System.out.println("  Nom: " + deliveryName);
                                System.out.println("  Adresse: " + address);
                                System.out.println("  Statut: " + status);
                                System.out.println("  DeliverId: " + deliverId);
                                System.out.println("  Date: " + order.getOrderDate());

                                // Vérifier si le deliverId correspond bien
                                if (deliverId != null && deliverId.equals(userId)) {
                                    System.out.println("  ✓ DeliverId correspond à l'utilisateur connecté");
                                } else {
                                    System.out.println("  ✗ DeliverId NE correspond PAS");
                                }

                                // Ajouter à la liste
                                orderList.add(order);

                            } catch (Exception e) {
                                System.out.println("Erreur lors du parsing de la commande " + document.getId() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        // Trier par date (plus récent en premier)
                        Collections.sort(orderList, (o1, o2) -> {
                            if (o1.getOrderDate() != null && o2.getOrderDate() != null) {
                                return o2.getOrderDate().compareTo(o1.getOrderDate());
                            }
                            return 0;
                        });

                        orderAdapter.notifyDataSetChanged();

                        System.out.println("DEBUG - Nombre total de commandes trouvées: " + count);
                        System.out.println("DEBUG - Nombre de commandes dans la liste: " + orderList.size());

                        if (orderList.isEmpty()) {
                            showEmptyState("Aucune commande assignée à vous");
                        } else {
                            showDataState();
                        }
                    } else {
                        String errorMessage = "Erreur de chargement: ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                            task.getException().printStackTrace();
                        } else {
                            errorMessage += "Erreur inconnue";
                        }
                        showErrorState(errorMessage);
                    }
                });
    }

    // Méthodes pour gérer les états d'affichage
    private void showLoadingState() {
        ordersRecyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvErrorState.setVisibility(View.GONE);
    }

    private void showDataState() {
        ordersRecyclerView.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        tvErrorState.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        ordersRecyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvErrorState.setVisibility(View.GONE);
        tvEmptyState.setText(message);
    }

    private void showErrorState(String errorMessage) {
        ordersRecyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvErrorState.setVisibility(View.VISIBLE);
        tvErrorState.setText(errorMessage);
    }

    private void showOrderDetails(Order order) {
        // TODO: Implémenter l'ouverture des détails de commande
        System.out.println("Détails de la commande: " + order.getOrderId());
    }

    private void acceptOrder(Order order) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("orders").document(order.getOrderId())
                    .update(
                            "status", "Prêt",
                            "deliverId", currentUser.getUid(),
                            "deliveryName", currentUser.getDisplayName()
                    )
                    .addOnSuccessListener(aVoid -> {
                        loadUserOrders(); // Recharger la liste
                    })
                    .addOnFailureListener(e -> {
                        showErrorState("Erreur d'acceptation: " + e.getMessage());
                    });
        }
    }

    private void startDelivery(Order order) {
        db.collection("orders").document(order.getOrderId())
                .update("status", "En cours")
                .addOnSuccessListener(aVoid -> {
                    loadUserOrders();
                })
                .addOnFailureListener(e -> {
                    showErrorState("Erreur de démarrage: " + e.getMessage());
                });
    }

    private void completeDelivery(Order order) {
        db.collection("orders").document(order.getOrderId())
                .update(
                        "status", "Livré",
                        "deliveryDate", new Date()
                )
                .addOnSuccessListener(aVoid -> {
                    loadUserOrders();
                })
                .addOnFailureListener(e -> {
                    showErrorState("Erreur de livraison: " + e.getMessage());
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserOrders(); // Recharger à chaque retour
    }
}