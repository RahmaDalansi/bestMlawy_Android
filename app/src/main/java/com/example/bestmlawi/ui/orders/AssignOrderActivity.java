package com.example.bestmlawi.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bestmlawi.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AssignOrderActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView txtOrderId, txtSelectedDeliverer;
    private Spinner spinnerSalesPoint;
    private Button btnAssignOrder;
    private GoogleMap mMap;

    private String orderId;
    private String documentId;
    private FirebaseFirestore db;

    // Listes et Maps
    private ArrayList<String> salesPointNames = new ArrayList<>();
    private HashMap<String, String> nameToSalesPointId = new HashMap<>();

    // Map pour stocker les informations des livreurs
    private HashMap<Marker, DelivererInfo> markerToDeliverer = new HashMap<>();

    // Set des livreurs actifs (IDs)
    private Set<String> activeDelivererIds = new HashSet<>();

    // Livreur sélectionné
    private String selectedDelivererId;
    private String selectedDelivererName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_assign_order);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        orderId = intent.getStringExtra("ORDER_ID");
        documentId = intent.getStringExtra("DOCUMENT_ID");

        // Initialiser les vues
        txtOrderId = findViewById(R.id.txtOrderId);
        txtSelectedDeliverer = findViewById(R.id.txtSelectedDeliverer);
        spinnerSalesPoint = findViewById(R.id.spinnerSalesPoint);
        btnAssignOrder = findViewById(R.id.btnAssignOrder);

        txtOrderId.setText("Cmd #" + orderId);

        // Initialiser la carte
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Charger les points de vente
        loadSalesPoints();

        // Bouton d'assignation
        btnAssignOrder.setOnClickListener(v -> assignOrder());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configurer la carte
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Position par défaut (Tunisie - coordonnées de votre livreur)
        LatLng defaultPosition = new LatLng(34.731294, 10.728953);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 12));

        // D'abord charger les statuts actifs, puis les livreurs
        loadActiveDeliverers();

        // Listener pour la sélection de marker
        mMap.setOnMarkerClickListener(marker -> {
            DelivererInfo deliverer = markerToDeliverer.get(marker);
            if (deliverer != null) {
                selectedDelivererId = deliverer.id;
                selectedDelivererName = deliverer.name;

                txtSelectedDeliverer.setText(deliverer.name + " (" + deliverer.phoneNumber + ")");
                txtSelectedDeliverer.setTextColor(getResources().getColor(android.R.color.black));
                btnAssignOrder.setEnabled(true);

                // Réinitialiser tous les markers à la couleur par défaut
                for (Marker m : markerToDeliverer.keySet()) {
                    m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                // Mettre en surbrillance le marker sélectionné
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                return true;
            }
            return false;
        });
    }

    private void loadSalesPoints() {
        db.collection("SalesPoints")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            String id = doc.getId();

                            if (name != null && !name.isEmpty()) {
                                salesPointNames.add(name);
                                nameToSalesPointId.put(name, id);
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, salesPointNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerSalesPoint.setAdapter(adapter);

                        if (salesPointNames.isEmpty()) {
                            Toast.makeText(this, "Aucun point de vente trouvé", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Erreur chargement points de vente: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadActiveDeliverers() {
        // Étape 1: Charger tous les statuts actifs depuis delivery_status
        db.collection("delivery_status")
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Récupérer tous les IDs des livreurs actifs
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String userId = doc.getString("userId");
                            if (userId != null) {
                                activeDelivererIds.add(userId);
                            }
                        }

                        // Étape 2: Charger les informations des livreurs
                        loadDeliverersOnMap();
                    } else {
                        Toast.makeText(this, "Erreur chargement statuts: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadDeliverersOnMap() {
        db.collection("Employees")
                .whereEqualTo("role", "deliver")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int delivererCount = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            String id = doc.getId();
                            String phoneNumber = doc.getString("phoneNumber");

                            // Récupérer latitude et longitude
                            Double latitude = doc.getDouble("latitude");
                            Double longitude = doc.getDouble("longitude");

                            // Vérifier que le livreur est actif ET a des coordonnées
                            if (name != null && activeDelivererIds.contains(id) &&
                                    latitude != null && longitude != null) {

                                LatLng position = new LatLng(latitude, longitude);

                                // Créer un marker pour le livreur
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(name)
                                        .snippet("Tél: " + (phoneNumber != null ? phoneNumber : "N/A") + " - Cliquez pour sélectionner")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                                // Stocker les informations du livreur
                                markerToDeliverer.put(marker, new DelivererInfo(id, name, phoneNumber, latitude, longitude));
                                delivererCount++;
                            }
                        }

                        if (delivererCount == 0) {
                            Toast.makeText(this, "Aucun livreur actif disponible avec localisation", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, delivererCount + " livreur(s) actif(s) disponible(s)", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Erreur chargement livreurs: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void assignOrder() {
        if (spinnerSalesPoint.getSelectedItem() == null) {
            Toast.makeText(this, "Veuillez sélectionner un point de vente", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDelivererId == null || selectedDelivererName == null) {
            Toast.makeText(this, "Veuillez sélectionner un livreur sur la carte", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedSalesPointName = spinnerSalesPoint.getSelectedItem().toString();
        String salesPointId = nameToSalesPointId.get(selectedSalesPointName);

        if (salesPointId == null) {
            Toast.makeText(this, "Erreur interne : ID point de vente introuvable", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mise à jour Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("sales_point_id", salesPointId);
        updates.put("deliverId", selectedDelivererId);
        updates.put("deliveryName", selectedDelivererName);
        updates.put("status", "Prêt");

        db.collection("orders").document(documentId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Commande assignée avec succès", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("MESSAGE", "Commande assignée avec succès");
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Erreur: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Classe interne pour stocker les infos des livreurs
    private static class DelivererInfo {
        String id;
        String name;
        String phoneNumber;
        double latitude;
        double longitude;

        DelivererInfo(String id, String name, String phoneNumber, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}