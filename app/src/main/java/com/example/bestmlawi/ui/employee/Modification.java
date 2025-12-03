package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Modification extends Activity {
    private MaterialButton btnRetour, btnModifier, btnSelectImage;
    private TextInputEditText edtName, edtEmail, edtPhone, edtAddress;
    private AutoCompleteTextView spinnerPointDeVente;
    private Chip cbCollaborator, cbCoordinator, cbDeliver;
    private ChipGroup roleChipGroup;
    private ImageView imgProfile;

    // Supprimé: private com.google.android.material.textview.MaterialTextView txtEmployeeInfo;

    private FirebaseFirestore db;
    private String employeeId = "";
    private String employeeName = "";
    private String currentImageBase64 = "";
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private List<String> pointsDeVenteList = new ArrayList<>();
    private ArrayAdapter<String> pointDeVenteAdapter;
    private String selectedPointDeVente = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modification);
        initialize();
        setListeners();
        loadSalesPoints();
        loadEmployeeData();
    }

    private void initialize() {
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        // SUPPRIMÉ: txtEmployeeInfo = findViewById(R.id.txtEmployeeInfo);
        imgProfile = findViewById(R.id.imgProfile);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        // Initialize EditTexts
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        spinnerPointDeVente = findViewById(R.id.spinnerPointDeVente);

        // Initialize Chips
        roleChipGroup = findViewById(R.id.roleChipGroup);
        cbCollaborator = findViewById(R.id.cbCollaborator);
        cbCoordinator = findViewById(R.id.cbCoordinator);
        cbDeliver = findViewById(R.id.cbDeliver);

        // Configure chip behavior for single selection
        roleChipGroup.setSingleSelection(true);

        // Ajouter le listener pour gérer les couleurs des chips
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

        // Initialize buttons
        btnRetour = findViewById(R.id.btnRetour);
        btnModifier = findViewById(R.id.btnModifier);

        // Initialize adapter for Point de Vente
        pointDeVenteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerPointDeVente.setAdapter(pointDeVenteAdapter);
    }

    private void setListeners() {
        btnRetour.setOnClickListener(v -> finish());

        btnSelectImage.setOnClickListener(v -> selectImageFromGallery());

        btnModifier.setOnClickListener(v -> {
            if (validate()) {
                updateEmployee();
            }
        });
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

    private void loadSalesPoints() {
        db.collection("SalesPoints")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        pointsDeVenteList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String nomPointDeVente = document.getString("name");
                            if (nomPointDeVente != null) {
                                pointsDeVenteList.add(nomPointDeVente);
                            }
                        }

                        pointDeVenteAdapter.clear();
                        pointDeVenteAdapter.addAll(pointsDeVenteList);
                        pointDeVenteAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Erreur lors du chargement des points de vente", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadEmployeeData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EMPLOYEE_ID")) {
            // Get employee data
            employeeId = intent.getStringExtra("EMPLOYEE_ID");
            employeeName = intent.getStringExtra("EMPLOYEE_NAME");
            String role = intent.getStringExtra("EMPLOYEE_ROLE");
            String location = intent.getStringExtra("EMPLOYEE_LOCATION");
            String email = intent.getStringExtra("EMPLOYEE_EMAIL");
            String phone = intent.getStringExtra("EMPLOYEE_PHONE");
            String address = intent.getStringExtra("EMPLOYEE_ADDRESS");
            String imageUrl = intent.getStringExtra("EMPLOYEE_IMAGE_URL");

            // Debug: Afficher les données reçues
            Toast.makeText(this, "Données reçues: " +
                            "Name: " + employeeName +
                            ", Email: " + email +
                            ", Phone: " + phone +
                            ", Address: " + address +
                            ", Location: " + location,
                    Toast.LENGTH_LONG).show();

            // Store current image
            currentImageBase64 = imageUrl != null ? imageUrl : "";
            selectedPointDeVente = location != null ? location : "";

            // Fill fields with existing data
            edtName.setText(employeeName);
            edtEmail.setText(email != null ? email : "");
            edtPhone.setText(phone != null ? phone : "");

            // CORRECTION: Afficher l'adresse si elle existe, sinon afficher location
            if (address != null && !address.isEmpty()) {
                edtAddress.setText(address);
            } else if (location != null && !location.isEmpty()) {
                edtAddress.setText(location); // Utiliser location comme fallback
            } else {
                edtAddress.setText("");
            }

            spinnerPointDeVente.setText(selectedPointDeVente);

            // Load existing image if available
            if (currentImageBase64 != null && !currentImageBase64.isEmpty() && currentImageBase64.length() > 100) {
                try {
                    byte[] decodedString = Base64.decode(currentImageBase64, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgProfile.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    // If Base64 decoding fails, use placeholder
                    imgProfile.setImageResource(R.drawable.ic_person_placeholder);
                }
            }

            // Select appropriate role
            if (role != null) {
                switch (role.toLowerCase()) {
                    case "collaborator":
                        cbCollaborator.setChecked(true);
                        // Appliquer la couleur programmatiquement
                        cbCollaborator.setChipBackgroundColorResource(R.color.purple_500);
                        cbCollaborator.setTextColor(getResources().getColor(android.R.color.white));
                        cbCollaborator.setChipIconTintResource(android.R.color.white);
                        break;
                    case "coordinator":
                        cbCoordinator.setChecked(true);
                        cbCoordinator.setChipBackgroundColorResource(R.color.purple_500);
                        cbCoordinator.setTextColor(getResources().getColor(android.R.color.white));
                        cbCoordinator.setChipIconTintResource(android.R.color.white);
                        break;
                    case "deliver":
                        cbDeliver.setChecked(true);
                        cbDeliver.setChipBackgroundColorResource(R.color.purple_500);
                        cbDeliver.setTextColor(getResources().getColor(android.R.color.white));
                        cbDeliver.setChipIconTintResource(android.R.color.white);
                        break;
                    default:
                        cbCollaborator.setChecked(true);
                        cbCollaborator.setChipBackgroundColorResource(R.color.purple_500);
                        cbCollaborator.setTextColor(getResources().getColor(android.R.color.white));
                        cbCollaborator.setChipIconTintResource(android.R.color.white);
                        break;
                }
            } else {
                cbCollaborator.setChecked(true);
                cbCollaborator.setChipBackgroundColorResource(R.color.purple_500);
                cbCollaborator.setTextColor(getResources().getColor(android.R.color.white));
                cbCollaborator.setChipIconTintResource(android.R.color.white);
            }
        } else {
            Toast.makeText(this, "Error: No employee selected", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateEmployee() {
        // Get new values
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String selectedPointDeVente = spinnerPointDeVente.getText().toString().trim();
        String role = getSelectedRole();

        // Validation
        if (selectedPointDeVente.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner un point de vente", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir l'image en Base64 si une nouvelle image est sélectionnée
        String imageBase64 = currentImageBase64;
        if (selectedImageUri != null) {
            imageBase64 = convertImageToBase64();
        }

        // Create updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phoneNumber", phone);
        updates.put("address", address);
        updates.put("role", role);
        updates.put("location", selectedPointDeVente);
        updates.put("point_of_sale_id", ""); // Même que dans Ajout
        updates.put("image", imageBase64 != null ? imageBase64 : "");
        // Note: Nous ne modifions pas la date d'embauche lors de la modification
        // updates.put("hiredDate", FieldValue.serverTimestamp()); // Utilisez ceci si vous voulez mettre à jour la date

        // Update in Firebase
        db.collection("Employees").document(employeeId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(Modification.this, "Employee updated successfully", Toast.LENGTH_SHORT).show();

                        // Redirect to Consultation
                        redirectToConsultation();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Modification.this, "Error updating employee: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

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

    private boolean validate() {
        boolean isValid = true;

        if (edtName.getText().toString().trim().isEmpty()) {
            edtName.setError("Name is required");
            isValid = false;
        }

        if (edtEmail.getText().toString().trim().isEmpty()) {
            edtEmail.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(edtEmail.getText().toString().trim()).matches()) {
            edtEmail.setError("Please enter a valid email");
            isValid = false;
        }

        if (edtPhone.getText().toString().trim().isEmpty()) {
            edtPhone.setError("Phone number is required");
            isValid = false;
        }

        if (edtAddress.getText().toString().trim().isEmpty()) {
            edtAddress.setError("Address is required");
            isValid = false;
        }

        if (spinnerPointDeVente.getText().toString().trim().isEmpty()) {
            spinnerPointDeVente.setError("Please select a point de vente");
            isValid = false;
        }

        if (getSelectedRole().isEmpty()) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
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

    private void redirectToConsultation() {
        Intent intent = new Intent(Modification.this, Consultation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}