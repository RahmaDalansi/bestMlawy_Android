package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private TextInputEditText edtName, edtEmail, edtPhone, edtAddress, edtPassword, edtConfirmPassword;
    private AutoCompleteTextView spinnerPointDeVente;
    private Chip cbCollaborator, cbCoordinator, cbDeliver;
    private ChipGroup roleChipGroup;
    private ImageView imgProfile;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    // Variables pour stocker les points de vente (nom -> id)
    private List<String> pointsDeVenteNoms = new ArrayList<>();
    private List<String> pointsDeVenteIds = new ArrayList<>();
    private String selectedPointDeVenteId = "";

    // TAG pour les logs
    private static final String TAG = "AjoutActivity";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajout);
        initialiser();
        ecouteurs();
    }

    private void initialiser() {
        // Initialiser Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

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
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        spinnerPointDeVente = findViewById(R.id.spinnerPointDeVente);

        // Initialiser les chips de rôle
        roleChipGroup = findViewById(R.id.roleChipGroup);
        cbCollaborator = findViewById(R.id.cbCollaborator);
        cbCoordinator = findViewById(R.id.cbCoordinator);
        cbDeliver = findViewById(R.id.cbDeliver);

        // Configurer le comportement des chips pour être EXCLUSIFS
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

        // Charger les points de vente depuis Firebase
        loadSalesPoints();
    }

    private void loadSalesPoints() {
        db.collection("SalesPoints")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        pointsDeVenteNoms.clear();
                        pointsDeVenteIds.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String nomPointDeVente = document.getString("name");
                            String pointDeVenteId = document.getId();

                            if (nomPointDeVente != null) {
                                pointsDeVenteNoms.add(nomPointDeVente);
                                pointsDeVenteIds.add(pointDeVenteId);
                            }
                        }

                        // Créer un adaptateur pour l'AutoCompleteTextView
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                pointsDeVenteNoms
                        );
                        spinnerPointDeVente.setAdapter(adapter);

                        // Ajouter un listener pour récupérer l'ID sélectionné
                        spinnerPointDeVente.setOnItemClickListener((parent, view, position, id) -> {
                            if (position >= 0 && position < pointsDeVenteIds.size()) {
                                selectedPointDeVenteId = pointsDeVenteIds.get(position);
                                Log.d(TAG, "Point de vente sélectionné - Nom: " +
                                        pointsDeVenteNoms.get(position) + ", ID: " + selectedPointDeVenteId);
                            }
                        });
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
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imgProfile.setImageBitmap(bitmap);
                Log.d(TAG, "Image sélectionnée avec succès");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void ajouterEmployee() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();
        String selectedPointDeVenteNom = spinnerPointDeVente.getText().toString().trim();

        // Vérifier si un point de vente a été sélectionné
        if (selectedPointDeVenteNom.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner un point de vente", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifier que l'ID du point de vente a été récupéré
        if (selectedPointDeVenteId.isEmpty()) {
            // Essayer de trouver l'ID correspondant au nom sélectionné
            int index = pointsDeVenteNoms.indexOf(selectedPointDeVenteNom);
            if (index >= 0 && index < pointsDeVenteIds.size()) {
                selectedPointDeVenteId = pointsDeVenteIds.get(index);
            } else {
                Toast.makeText(this, "Erreur: point de vente non trouvé", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String role = getSelectedRole();

        // Validation
        if (name.isEmpty()) {
            edtName.setError("Nom est requis");
            return;
        }
        if (email.isEmpty()) {
            edtEmail.setError("Email est requis");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Format d'email invalide");
            return;
        }
        if (phone.isEmpty()) {
            edtPhone.setError("Numéro de téléphone est requis");
            return;
        }
        if (password.isEmpty()) {
            edtPassword.setError("Mot de passe est requis");
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Les mots de passe ne correspondent pas");
            return;
        }
        if (role.isEmpty() || roleChipGroup.getCheckedChipId() == View.NO_ID) {
            Toast.makeText(this, "Veuillez sélectionner un rôle", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir l'image en Base64 si sélectionnée
        String imageBase64;
        if (selectedImageUri != null) {
            imageBase64 = convertImageToBase64();
            if (imageBase64 == null) {
                Toast.makeText(this, "Erreur lors du traitement de l'image", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Image convertie en Base64, taille: " + imageBase64.length());
        } else {
            imageBase64 = "";
            Log.d(TAG, "Aucune image sélectionnée, utilisation de l'image par défaut");
        }

        // Désactiver le bouton pour éviter les clics multiples
        btnAjouter.setEnabled(false);
        btnAjouter.setText("Création en cours...");

        // Créer le compte d'authentification d'abord
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Créer l'employé dans Firestore
                                createEmployeeInFirestore(user.getUid(), name, email, phone, address,
                                        selectedPointDeVenteId, selectedPointDeVenteNom, role, imageBase64);
                            } else {
                                enableButton();
                                Toast.makeText(Ajout.this, "Erreur: utilisateur non créé", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            enableButton();
                            String errorMessage = "Échec de création du compte";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                                if (errorMessage.contains("email address is already in use")) {
                                    errorMessage = "Cet email est déjà utilisé";
                                }
                            }
                            Toast.makeText(Ajout.this, errorMessage, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur création compte: " + errorMessage);
                        }
                    }
                });
    }

    private void createEmployeeInFirestore(String uid, String name, String email, String phone,
                                           String address, String pointDeVenteId, String pointDeVenteNom,
                                           String role, String imageBase64) {

        // Préparer les données pour Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("id", uid);
        data.put("name", name);
        data.put("email", email);
        data.put("phoneNumber", phone);
        data.put("role", role);
        data.put("point_of_sale_id", pointDeVenteId); // Stocker l'ID du point de vente
        data.put("point_of_sale_name", pointDeVenteNom); // Stocker aussi le nom pour l'affichage
        data.put("location", ""); // Laisser vide pour le moment
        data.put("hiredDate", new Date());
        data.put("address", address);
        data.put("authId", uid);
        data.put("createdAt", System.currentTimeMillis());
        data.put("isActive", true);

        // Ajouter l'image seulement si elle n'est pas vide
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            data.put("image", imageBase64);
            Log.d(TAG, "Image Base64 ajoutée aux données");
        } else {
            data.put("image", "");
            Log.d(TAG, "Aucune image ajoutée (champ vide)");
        }

        // Sauvegarder dans la collection Employees
        db.collection("Employees").document(uid)
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "Employé créé avec succès dans Firestore, ID: " + uid);
                        Log.d(TAG, "Point de vente ID: " + pointDeVenteId);
                        Log.d(TAG, "Point de vente Nom: " + pointDeVenteNom);
                        Toast.makeText(Ajout.this, "Employé ajouté avec succès!", Toast.LENGTH_SHORT).show();
                        clearFields();
                        redirectToConsultation();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        enableButton();
                        String errorMessage = "Erreur sauvegarde données: " + e.getMessage();
                        Toast.makeText(Ajout.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, errorMessage);

                        // Supprimer le compte d'authentification si Firestore échoue
                        if (mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Compte auth supprimé après échec Firestore");
                                }
                            });
                        }
                    }
                });
    }

    private String getSelectedRole() {
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

    private String convertImageToBase64() {
        try {
            if (selectedImageUri == null) {
                Log.d(TAG, "selectedImageUri est null");
                return "";
            }

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
            if (bitmap == null) {
                Log.d(TAG, "Bitmap est null");
                return "";
            }

            // Redimensionner l'image pour réduire la taille (optionnel)
            int maxWidth = 800;
            int maxHeight = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            if (width > maxWidth || height > maxHeight) {
                float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
                int newWidth = (int) (width * ratio);
                int newHeight = (int) (height * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Compresser l'image avec qualité 75% pour réduire la taille
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
            Log.d(TAG, "Image convertie, taille Base64: " + base64.length() + " caractères");

            return base64;

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Erreur conversion image: " + e.getMessage());
            Toast.makeText(this, "Erreur conversion image", Toast.LENGTH_SHORT).show();
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Erreur générale conversion: " + e.getMessage());
            return "";
        }
    }

    private void enableButton() {
        btnAjouter.setEnabled(true);
        btnAjouter.setText("SAVE");
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
        if (edtPassword != null) edtPassword.setText("");
        if (edtConfirmPassword != null) edtConfirmPassword.setText("");

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

        // Réinitialiser l'ID du point de vente
        selectedPointDeVenteId = "";

        enableButton();
    }
}