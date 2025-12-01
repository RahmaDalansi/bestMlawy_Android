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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Modification extends Activity {
    private EditText edtName, edtEmail, edtPhone, edtLocation;
    private RadioGroup rgRole;
    private RadioButton rbCollaborator, rbCoordinator, rbDeliver;
    private Button btnCancel, btnUpdate, btnSelectImage;
    private TextView txtEmployeeInfo;
    private ImageView imgProfile;

    private FirebaseFirestore db;
    private String employeeId = "";
    private String employeeName = "";
    private String currentImageBase64 = "";
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modification);
        initialize();
        setListeners();
        loadEmployeeData();
    }

    private void initialize() {
        // Initialize TextViews
        txtEmployeeInfo = findViewById(R.id.txtEmployeeInfo);

        // Initialize ImageView and Button
        imgProfile = findViewById(R.id.imgProfile);
        btnSelectImage = findViewById(R.id.btnSelectImage);

        // Initialize EditTexts
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtLocation = findViewById(R.id.edtLocation);

        // Initialize RadioButtons
        rgRole = findViewById(R.id.rgRole);
        rbCollaborator = findViewById(R.id.rbCollaborator);
        rbCoordinator = findViewById(R.id.rbCoordinator);
        rbDeliver = findViewById(R.id.rbDeliver);

        // Initialize buttons
        btnCancel = findViewById(R.id.btnRetour);
        btnUpdate = findViewById(R.id.btnModifier);

        db = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()) {
                    updateEmployee();
                }
            }
        });

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageFromGallery();
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
            String imageUrl = intent.getStringExtra("EMPLOYEE_IMAGE_URL");

            // Store current image
            currentImageBase64 = imageUrl != null ? imageUrl : "";

            // Display employee information
            txtEmployeeInfo.setText("Editing: " + employeeName);

            // Fill fields with existing data
            edtName.setText(employeeName);
            edtEmail.setText(email != null ? email : "");
            edtPhone.setText(phone != null ? phone : "");
            edtLocation.setText(location != null ? location : "");

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
                        rbCollaborator.setChecked(true);
                        break;
                    case "coordinator":
                        rbCoordinator.setChecked(true);
                        break;
                    case "deliver":
                        rbDeliver.setChecked(true);
                        break;
                    default:
                        rbCollaborator.setChecked(true);
                        break;
                }
            } else {
                rbCollaborator.setChecked(true);
            }
        } else {
            Toast.makeText(this, "Error: No employee selected", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateEmployee() {
        // Validate fields
        if (!validate()) {
            return;
        }

        // Get new values
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();
        String role = getSelectedRole();

        // Create updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phoneNumber", phone);
        updates.put("location", location);
        updates.put("role", role);

        // Handle image update
        if (selectedImageUri != null) {
            String newImageBase64 = convertImageToBase64();
            if (newImageBase64 != null) {
                updates.put("image", newImageBase64);
            }
        }

        // Update in Firebase
        db.collection("Employees").document(employeeId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(Modification.this, "Employee updated successfully", Toast.LENGTH_SHORT).show();

                        // Redirect to Consultation
                        Intent intent = new Intent(Modification.this, Consultation.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
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
        if (edtName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (edtEmail.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (rgRole.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getSelectedRole() {
        int selectedId = rgRole.getCheckedRadioButtonId();

        if (selectedId == R.id.rbCollaborator) {
            return "collaborator";
        } else if (selectedId == R.id.rbCoordinator) {
            return "coordinator";
        } else if (selectedId == R.id.rbDeliver) {
            return "deliver";
        }

        return "collaborator";
    }
}