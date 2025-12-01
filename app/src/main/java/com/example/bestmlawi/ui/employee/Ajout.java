package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ajout extends Activity {
    private Button btnRetour, btnAjouter, btnSelectImage;
    private EditText edtName, edtEmail, edtPhone, edtAddress, edtWorkLocation;
    private CheckBox cbCollaborator, cbCoordinator, cbDeliver;
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
        btnRetour = findViewById(R.id.btnRetour);
        btnAjouter = findViewById(R.id.btnAjouter);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        imgProfile = findViewById(R.id.imgProfile);

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtWorkLocation = findViewById(R.id.edtWorkLocation);

        cbCollaborator = findViewById(R.id.cbCollaborator);
        cbCoordinator = findViewById(R.id.cbCoordinator);
        cbDeliver = findViewById(R.id.cbDeliver);

        db = FirebaseFirestore.getInstance();
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
        String workLocation = edtWorkLocation.getText().toString().trim();

        String role = getSelectedRole(); // Changé pour retourner un seul rôle

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
        if (role.isEmpty()) {
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
        employee.setLocation(workLocation);
        employee.setHiredDate(new Date());
        employee.setPointOfSaleId(""); // Vous pouvez modifier selon vos besoins
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
        // Retourne un seul rôle (le premier coché)
        if (cbCollaborator.isChecked()) return "collaborator";
        if (cbCoordinator.isChecked()) return "coordinator";
        if (cbDeliver.isChecked()) return "deliver";
        return "";
    }

    private List<String> getSelectedRoles() {
        // Méthode conservée pour compatibilité, mais utilise getSelectedRole() maintenant
        List<String> selectedRoles = new ArrayList<>();
        if (cbCollaborator.isChecked()) selectedRoles.add("collaborator");
        if (cbCoordinator.isChecked()) selectedRoles.add("coordinator");
        if (cbDeliver.isChecked()) selectedRoles.add("deliver");
        return selectedRoles;
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

    private void redirectToConsultation() {
        Intent intent = new Intent(Ajout.this, Consultation.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void clearFields() {
        edtName.setText("");
        edtEmail.setText("");
        edtPhone.setText("");
        edtAddress.setText("");
        edtWorkLocation.setText("");
        cbCollaborator.setChecked(false);
        cbCoordinator.setChecked(false);
        cbDeliver.setChecked(false);
        imgProfile.setImageResource(R.drawable.ic_person_placeholder);
        selectedImageUri = null;
    }
}