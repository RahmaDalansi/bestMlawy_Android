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
import java.util.List;

public class ActiveDeliveryFragment extends Fragment {

    private RecyclerView activeOrdersRecyclerView;
    private DeliveryOrderAdapter orderAdapter;
    private List<Order> activeOrderList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvEmptyState;
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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout_active);
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
                // Non utilisé dans les livraisons actives - les commandes sont déjà acceptées
            }

            @Override
            public void onStartDelivery(Order order) {
                // Si la commande est "assigned", on peut la démarrer
                if ("assigned".equals(order.getStatus())) {
                    startDelivery(order);
                }
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
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadActiveDeliveries() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Charger uniquement les commandes en cours de livraison (assigned et in_progress)
        db.collection("orders")
                .whereEqualTo("deliveryPersonId", userId)
                .whereIn("status", java.util.Arrays.asList("assigned", "in_progress"))
                .orderBy("orderDate", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        activeOrderList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = document.toObject(Order.class);
                            activeOrderList.add(order);
                        }
                        orderAdapter.notifyDataSetChanged();

                        updateEmptyState();
                    } else {
                        // Gérer l'erreur
                        tvEmptyState.setText("Erreur de chargement");
                        tvEmptyState.setVisibility(View.VISIBLE);
                        activeOrdersRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    private void updateActiveCount() {
        TextView tvActiveCount = getView().findViewById(R.id.tv_active_count);
        if (tvActiveCount != null) {
            int count = activeOrderList.size();
            String text = count + " livraison(s) active(s)";
            tvActiveCount.setText(text);
        }
    }

    private void updateEmptyState() {
        if (activeOrderList.isEmpty()) {
            tvEmptyState.setText("Aucune livraison en cours");
            tvEmptyState.setVisibility(View.VISIBLE);
            activeOrdersRecyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            activeOrdersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showOrderDetails(Order order) {
        // Ouvrir les détails de la commande
        // Intent intent = new Intent(getActivity(), OrderDetailsActivity.class);
        // intent.putExtra("order_id", order.getOrderId());
        // startActivity(intent);

        // Pour l'instant, afficher un toast
        android.widget.Toast.makeText(getContext(),
                "Détails de la commande: " + order.getOrderId(),
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void startDelivery(Order order) {
        // Marquer la commande comme "en cours de livraison"
        db.collection("orders").document(order.getOrderId())
                .update("status", "in_progress")
                .addOnSuccessListener(aVoid -> {
                    // Mettre à jour l'interface
                    loadActiveDeliveries();

                    // Afficher une notification
                    android.widget.Toast.makeText(getContext(),
                            "Livraison commencée!",
                            android.widget.Toast.LENGTH_SHORT).show();

                    // Ici vous pourriez lancer la navigation GPS
                    // startNavigation(order);
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(getContext(),
                            "Erreur: " + e.getMessage(),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void completeDelivery(Order order) {
        // Marquer la commande comme "livrée"
        db.collection("orders").document(order.getOrderId())
                .update(
                        "status", "delivered",
                        "deliveryDate", new java.util.Date()
                )
                .addOnSuccessListener(aVoid -> {
                    // Mettre à jour les statistiques du livreur
                    updateDeliveryStats();

                    // Recharger la liste
                    loadActiveDeliveries();

                    // Afficher une notification de succès
                    android.widget.Toast.makeText(getContext(),
                            "Livraison terminée avec succès!",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(getContext(),
                            "Erreur: " + e.getMessage(),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void updateDeliveryStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Incrémenter le compteur de livraisons complétées
        db.collection("delivery_stats").document(userId)
                .update(
                        "completedDeliveries", com.google.firebase.firestore.FieldValue.increment(1),
                        "currentProgress", com.google.firebase.firestore.FieldValue.increment(1),
                        "lastUpdated", new java.util.Date()
                )
                .addOnSuccessListener(aVoid -> {
                    // Statistiques mises à jour avec succès
                    android.widget.Toast.makeText(getContext(),
                            "Statistiques mises à jour",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // En cas d'erreur, créer le document de stats s'il n'existe pas
                    createDeliveryStats(userId);
                });
    }

    private void createDeliveryStats(String userId) {
        // Créer un document de statistiques initial
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("userId", userId);
        stats.put("completedDeliveries", 1);
        stats.put("monthlyGoal", 50);
        stats.put("currentProgress", 1);
        stats.put("rating", 5.0);
        stats.put("successRate", 100.0);
        stats.put("averageDeliveryTime", 0);
        stats.put("onTimeDeliveries", 1);
        stats.put("lastUpdated", new java.util.Date());

        db.collection("delivery_stats").document(userId)
                .set(stats)
                .addOnSuccessListener(aVoid -> {
                    android.widget.Toast.makeText(getContext(),
                            "Statistiques initialisées",
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }

    private void startNavigation(Order order) {
        // Implémentation de la navigation GPS
        // Cette méthode pourrait lancer Google Maps ou une autre application de navigation
        /*
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + order.getLatitude() + "," + order.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recharger les données à chaque retour sur le fragment
        loadActiveDeliveries();
    }
}