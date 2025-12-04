package com.example.bestmlawi.ui.delivery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryOrdersFragment extends Fragment {

    private RecyclerView ordersRecyclerView;
    private DeliveryOrderAdapter orderAdapter;
    private List<Order> orderList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState, tvErrorState;
    private ImageView imgScanQr;

    // Scanner QR Code
    private GmsBarcodeScanner scanner;
    private boolean isScanning = false;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_orders, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        orderList = new ArrayList<>();

        initializeViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        initBarcodeScanner();
        setupQrScanListener();
        loadUserOrders();

        return view;
    }

    private void initializeViews(View view) {
        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvErrorState = view.findViewById(R.id.tv_error_state);
        imgScanQr = view.findViewById(R.id.imgScanQr);

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

    // ==================== QR CODE SCANNER ====================

    private void initBarcodeScanner() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC)
                .enableAutoZoom()
                .build();

        scanner = GmsBarcodeScanning.getClient(requireContext(), options);
    }

    private void setupQrScanListener() {
        if (imgScanQr != null) {
            imgScanQr.setOnClickListener(v -> onScanQRCode());
        }
    }

    private void scanQrCode() {
        if (isScanning) {
            Toast.makeText(getContext(), "Scan déjà en cours...", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    isScanning = false;

                    if (barcode == null) {
                        Toast.makeText(getContext(), "Aucun code détecté", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String rawValue = barcode.getRawValue();
                    if (rawValue == null || rawValue.trim().isEmpty()) {
                        Toast.makeText(getContext(), "Code vide ou invalide", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Vérifier que la commande appartient au livreur connecté
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser == null) {
                        Toast.makeText(getContext(), "Erreur: Utilisateur non connecté", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId = currentUser.getUid();

                    // Récupérer la commande pour vérifier si elle appartient au livreur
                    db.collection("orders").document(rawValue)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String deliverId = documentSnapshot.getString("deliverId");
                                    String currentStatus = documentSnapshot.getString("status");

                                    // Vérifier si la commande est assignée à ce livreur
                                    if (deliverId != null && deliverId.equals(userId)) {
                                        // Marquer comme livrée
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("status", "Livré");
                                        data.put("deliveryDate", new Date());

                                        db.collection("orders").document(rawValue)
                                                .update(data)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(getContext(),
                                                            "✅ Commande #" + rawValue + " marquée comme livrée",
                                                            Toast.LENGTH_LONG).show();
                                                    loadUserOrders(); // Recharger la liste
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(getContext(),
                                                            "❌ Erreur mise à jour : " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    } else {
                                        Toast.makeText(getContext(),
                                                "⚠️ Cette commande n'est pas assignée à vous",
                                                Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(getContext(),
                                            "❌ Commande introuvable",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(),
                                        "❌ Erreur de vérification : " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    isScanning = false;
                    Toast.makeText(getContext(),
                            "❌ Scan échoué : " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean onScanQRCode() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, 1001);
        } else {
            scanQrCode();
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanQrCode();
            } else {
                Toast.makeText(getContext(),
                        "Permission caméra requise pour scanner le QR code",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== ORDERS MANAGEMENT ====================

    private void loadUserOrders() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showErrorState("Veuillez vous connecter");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String userId = currentUser.getUid();

        System.out.println("DEBUG - Recherche des commandes pour deliverId: " + userId);

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

                                Order order = new Order();
                                order.setOrderId(document.getId());

                                String deliveryName = document.getString("deliveryName");
                                String address = document.getString("address");
                                String status = document.getString("status");
                                String deliverId = document.getString("deliverId");
                                String userIdFromDb = document.getString("user_id");

                                order.setCustomerName(deliveryName != null ? deliveryName : "Nom inconnu");
                                order.setDeliveryAddress(address != null ? address : "Adresse inconnue");
                                order.setStatus(status != null ? status : "pending");
                                order.setDeliverId(deliverId);

                                if (userIdFromDb != null) {
                                    order.setCustomerPhone("Client ID: " + userIdFromDb);
                                }

                                Object orderDateObj = document.get("orderDate");
                                if (orderDateObj instanceof com.google.firebase.Timestamp) {
                                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) orderDateObj;
                                    order.setOrderDate(timestamp.toDate());
                                } else if (orderDateObj instanceof Date) {
                                    order.setOrderDate((Date) orderDateObj);
                                } else {
                                    order.setOrderDate(new Date());
                                }

                                System.out.println("DEBUG - Commande " + count + " trouvée:");
                                System.out.println("  ID: " + document.getId());
                                System.out.println("  Statut: " + status);

                                orderList.add(order);

                            } catch (Exception e) {
                                System.out.println("Erreur lors du parsing de la commande " + document.getId() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        Collections.sort(orderList, (o1, o2) -> {
                            if (o1.getOrderDate() != null && o2.getOrderDate() != null) {
                                return o2.getOrderDate().compareTo(o1.getOrderDate());
                            }
                            return 0;
                        });

                        orderAdapter.notifyDataSetChanged();

                        System.out.println("DEBUG - Nombre total de commandes trouvées: " + count);

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
                        Toast.makeText(getContext(), "✅ Commande acceptée", Toast.LENGTH_SHORT).show();
                        loadUserOrders();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void startDelivery(Order order) {
        db.collection("orders").document(order.getOrderId())
                .update("status", "En cours")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "🚚 Livraison démarrée", Toast.LENGTH_SHORT).show();
                    loadUserOrders();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void completeDelivery(Order order) {
        db.collection("orders").document(order.getOrderId())
                .update(
                        "status", "Livré",
                        "deliveryDate", new Date()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "✅ Livraison complétée", Toast.LENGTH_SHORT).show();
                    loadUserOrders();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserOrders();
    }
}