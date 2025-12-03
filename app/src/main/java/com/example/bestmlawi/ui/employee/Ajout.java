package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ajout extends Activity {
    private MaterialButton btnRetour, btnAjouter, btnSelectImage;
    private TextInputEditText edtName, edtEmail, edtPhone, edtAddress;
    private AutoCompleteTextView spinnerPointDeVente;
    private Chip cbCollaborator, cbCoordinator, cbDeliver;
    private ChipGroup roleChipGroup;
    private ImageView imgProfile;
    private FirebaseFirestore db;

    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajout);
        initialiser();
        ecouteurs();
    }

    private void initialiser() {
        // Initialiser Firebase
        db = FirebaseFirestore.getInstance();

        // Initialiser les vues
        btnRetour = findViewById(R.id.btnRetour);
        btnAjouter = findViewById(R.id.btnAjouter);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        imgProfile = findViewById(R.id.imgProfile);

        // Initialiser les champs de texte
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        spinnerPointDeVente = findViewById(R.id.spinnerPointDeVente);

        // Initialiser les chips de rôle
        roleChipGroup = findViewById(R.id.roleChipGroup);
        cbCollaborator = findViewById(R.id.cbCollaborator);
        cbCoordinator = findViewById(R.id.cbCoordinator);
        cbDeliver = findViewById(R.id.cbDeliver);

        // CORRECTION: Configurer le comportement des chips pour être EXCLUSIFS (single selection = true)
        roleChipGroup.setSingleSelection(true);

        // Ajouter le listener pour gérer les couleurs des chips (comme dans Modification)
        roleChipGroup.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
                // Réinitialiser la couleur de tous les chips
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip chip = (Chip) child;
                        chip.setChipBackgroundColorResource(android.R.color.transparent);
                        chip.setTextColor(getResources().getColor(R.color.purple_500));
                        chip.setChipIconTintResource(R.color.purple_500);
                    }
                }

                // Appliquer la couleur au chip sélectionné
                if (!checkedIds.isEmpty()) {
                    Chip selectedChip = group.findViewById(checkedIds.get(0));
                    if (selectedChip != null) {
                        selectedChip.setChipBackgroundColorResource(R.color.purple_500);
                        selectedChip.setTextColor(getResources().getColor(android.R.color.white));
                        selectedChip.setChipIconTintResource(android.R.color.white);
                    }
                }
            }
        });

        // Charger les points de vente depuis Firebase
        loadSalesPoints();
    }

    private void loadSalesPoints() {
        db.collection("SalesPoints")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> pointsDeVente = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String nomPointDeVente = document.getString("name");
                            if (nomPointDeVente != null) {
                                pointsDeVente.add(nomPointDeVente);
                            }
                        }

                        // Créer un adaptateur pour l'AutoCompleteTextView
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                pointsDeVente
                        );
                        spinnerPointDeVente.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "Erreur lors du chargement des points de vente", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void ecouteurs() {
        btnRetour.setOnClickListener(v -> finish());

        btnSelectImage.setOnClickListener(v -> selectImageFromGallery());

        btnAjouter.setOnClickListener(v -> ajouterEmployee());
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgProfile.setImageURI(selectedImageUri);
        }
    }

    private void ajouterEmployee() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String selectedPointDeVente = spinnerPointDeVente.getText().toString().trim();

        // Vérifier si un point de vente a été sélectionné
        if (selectedPointDeVente.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner un point de vente", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = getSelectedRole();

        // Validation
        if (name.isEmpty()) {
            edtName.setError("Name is required");
            return;
        }
        if (email.isEmpty()) {
            edtEmail.setError("Email is required");
            return;
        }
        if (phone.isEmpty()) {
            edtPhone.setError("Phone number is required");
            return;
        }
        // CORRECTION: Vérification plus précise du rôle
        if (role.isEmpty() || roleChipGroup.getCheckedChipId() == View.NO_ID) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir l'image en Base64 si sélectionnée
        String imageBase64 = null;
        if (selectedImageUri != null) {
            imageBase64 = convertImageToBase64();
        }

        // Créer l'objet Employee
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(email);
        employee.setPhoneNumber(phone);
        employee.setRole(role);
        employee.setLocation(selectedPointDeVente);
        employee.setHiredDate(new Date());
        employee.setPointOfSaleId(""); // Vous pouvez stocker l'ID du point de vente ici si nécessaire
        employee.setImageUrl(imageBase64 != null ? imageBase64 : "");

        // Préparer les données pour Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("name", employee.getName());
        data.put("email", employee.getEmail());
        data.put("phoneNumber", employee.getPhoneNumber());
        data.put("role", employee.getRole());
        data.put("location", employee.getLocation());
        data.put("hiredDate", employee.getHiredDate());
        data.put("point_of_sale_id", employee.getPointOfSaleId());
        data.put("image", employee.getImageUrl());
        data.put("address", address); // Ajouter l'adresse séparément

        // Sauvegarder dans Firestore
        db.collection("Employees")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Mettre à jour l'ID de l'employé
                        String employeeId = documentReference.getId();

                        // Optionnel: Mettre à jour le document avec l'ID
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("id", employeeId);
                        documentReference.update(updateData);

                        Toast.makeText(Ajout.this, "Employee added successfully!", Toast.LENGTH_SHORT).show();
                        clearFields();
                        redirectToConsultation();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(Ajout.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getSelectedRole() {
        // Vérifier quel chip est sélectionné
        int selectedId = roleChipGroup.getCheckedChipId();

        if (selectedId == R.id.cbCollaborator) {
            return "collaborator";
        } else if (selectedId == R.id.cbCoordinator) {
            return "coordinator";
        } else if (selectedId == R.id.cbDeliver) {
            return "deliver";
        }

        return "";
    }

    // Méthode supprimée car vous utilisez la sélection unique
    // private List<String> getSelectedRoles() { ... }

    private String convertImageToBase64() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

            // Redimensionner l'image pour réduire la taille
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error converting image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void redirectToConsultation() {
        Intent intent = new Intent(Ajout.this, Consultation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void clearFields() {
        // Réinitialiser les champs de texte
        if (edtName != null) edtName.setText("");
        if (edtEmail != null) edtEmail.setText("");
        if (edtPhone != null) edtPhone.setText("");
        if (edtAddress != null) edtAddress.setText("");

        // Réinitialiser la sélection des rôles
        roleChipGroup.clearCheck();

        // Réinitialiser aussi les couleurs des chips
        for (int i = 0; i < roleChipGroup.getChildCount(); i++) {
            View child = roleChipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setTextColor(getResources().getColor(R.color.purple_500));
                chip.setChipIconTintResource(R.color.purple_500);
            }
        }

        // Réinitialiser l'image de profil
        if (imgProfile != null) {
            imgProfile.setImageResource(R.drawable.ic_person_placeholder);
        }

        // Réinitialiser l'URI de l'image sélectionnée
        selectedImageUri = null;

        // Réinitialiser le sélecteur de point de vente
        if (spinnerPointDeVente != null) {
            spinnerPointDeVente.setText("");
        }
    }
}