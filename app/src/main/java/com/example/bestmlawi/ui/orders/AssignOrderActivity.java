package com.example.bestmlawi.ui.orders;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bestmlawi.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AssignOrderActivity extends AppCompatActivity {

    private TextView txtOrderId;
    private Spinner spinnerSalesPoint, spinnerDelivery;
    private Button btnAssignOrder;

    private String orderId;
    private String documentId;

    private FirebaseFirestore db;

    // Listes pour stocker les noms
    private ArrayList<String> salesPointNames = new ArrayList<>();
    private ArrayList<String> deliveryNames = new ArrayList<>();

    // Maps pour lier nom → ID
    private HashMap<String, String> nameToSalesPointId = new HashMap<>();
    private HashMap<String, String> nameToDeliveryId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_assign_order);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        orderId = intent.getStringExtra("ORDER_ID");
        documentId = intent.getStringExtra("DOCUMENT_ID");

        // 🔍 Trouver les vues par leurs vrais IDs
        txtOrderId = findViewById(R.id.txtOrderId);
        spinnerSalesPoint = findViewById(R.id.spinnerSalesPoint);
        spinnerDelivery = findViewById(R.id.spinnerDelivery);
        btnAssignOrder = findViewById(R.id.btnAssignOrder);

        txtOrderId.setText("Cmd #" + orderId);

        // 🔽 Charger les données
        loadSalesPoints();
        loadDeliverers();

        // ✅ Bouton assigner
        btnAssignOrder.setOnClickListener(v -> assignOrder());
    }

    private void loadSalesPoints() {
        db.collection("Sale_Point") // ✅ Nom exact de la collection (avec majuscule)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name"); // ✅ Champ "name", pas "Sale_Point"
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

                        // 👇 Log pour debug
                        if (salesPointNames.isEmpty()) {
                            Toast.makeText(this, "Aucun point de vente trouvé", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Points de vente chargés : " + salesPointNames.size(), Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Erreur chargement points de vente: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        task.getException().printStackTrace();
                    }
                });
    }

    private void loadDeliverers() {
        db.collection("Employees") // ✅ Nom exact de la collection (avec majuscule)
                .whereEqualTo("role", "deliver")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name"); // ✅ Champ "name"
                            String id = doc.getId();

                            if (name != null && !name.isEmpty()) {
                                deliveryNames.add(name);
                                nameToDeliveryId.put(name, id);
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, deliveryNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerDelivery.setAdapter(adapter);

                        // 👇 Log pour debug
                        if (deliveryNames.isEmpty()) {
                            Toast.makeText(this, "Aucun livreur trouvé", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Livreurs chargés : " + deliveryNames.size(), Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Erreur chargement livreurs: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        task.getException().printStackTrace();
                    }
                });
    }

    private void assignOrder() {
        // 🔎 Vérifier si un élément est sélectionné
        if (spinnerSalesPoint.getSelectedItem() == null ||
                spinnerDelivery.getSelectedItem() == null) {
            Toast.makeText(this, "Veuillez sélectionner tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedSalesPointName = spinnerSalesPoint.getSelectedItem().toString();
        String selectedDeliveryName = spinnerDelivery.getSelectedItem().toString();

        if (selectedSalesPointName.isEmpty() || selectedDeliveryName.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔗 Récupérer les IDs réels
        String salesPointId = nameToSalesPointId.get(selectedSalesPointName);
        String deliverId = nameToDeliveryId.get(selectedDeliveryName);

        if (salesPointId == null || deliverId == null) {
            Toast.makeText(this, "Erreur interne : ID introuvable", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Mise à jour Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("sales_point_id", salesPointId);
        updates.put("deliverId", deliverId);
        updates.put("deliveryName", selectedDeliveryName);
        updates.put("status", "Prêt");

        db.collection("orders").document(documentId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
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
}