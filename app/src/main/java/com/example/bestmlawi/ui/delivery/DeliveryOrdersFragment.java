package com.example.bestmlawi.ui.deliver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestmlawi.R;
import com.example.bestmlawi.adapters.DeliveryOrderAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class DeliveryOrdersFragment extends Fragment {

    private RecyclerView ordersRecyclerView;
    private DeliveryOrderAdapter orderAdapter;
    private List<Order> orderList;
    private FirebaseFirestore db;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_orders, container, false);

        db = FirebaseFirestore.getInstance();
        orderList = new ArrayList<>();

        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        orderAdapter = new DeliveryOrderAdapter(orderList);
        ordersRecyclerView.setAdapter(orderAdapter);

        loadDeliveryOrders();

        return view;
    }

    private void loadDeliveryOrders() {
        // Charger les commandes assignées à ce livreur
        String currentUserId = "user_id_here"; // À récupérer depuis Firebase Auth

        db.collection("orders")
                .whereEqualTo("deliveryPersonId", currentUserId)
                .whereEqualTo("status", "pending") // ou selon votre logique
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        orderList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = document.toObject(Order.class);
                            orderList.add(order);
                        }
                        orderAdapter.notifyDataSetChanged();
                    }
                });
    }
}