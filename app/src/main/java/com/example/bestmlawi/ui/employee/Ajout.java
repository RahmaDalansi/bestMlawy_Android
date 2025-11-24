package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Ajout extends Activity {
    private Button btnRetour, btnAjouter;
    private EditText edtName, edtEmail, edtPhone, edtAddress, edtWorkLocation;
    private RadioGroup rgRole;
    private RadioButton rbCollaborator, rbDeliver, rbCoordinator;
    private FirebaseFirestore db;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajout);
        initialiser();
        ecouteurs();
    }

    private void initialiser() {
        btnRetour = findViewById(R.id.btnRetour);
        btnAjouter = findViewById(R.id.btnAjouter);

        // Initialiser les EditText
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtWorkLocation = findViewById(R.id.edtWorkLocation);

        // Initialiser RadioGroup et RadioButtons
        rgRole = findViewById(R.id.rgRole);
        rbCollaborator = findViewById(R.id.rbCollaborator);
        rbDeliver = findViewById(R.id.rbDeliver);
        rbCoordinator = findViewById(R.id.rbCoordinator);

        db = FirebaseFirestore.getInstance();
    }

    private void ecouteurs() {
        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAjouter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ajouterEmployee();
            }
        });
    }

    private void ajouterEmployee() {
        // Récupérer les valeurs des champs
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String workLocation = edtWorkLocation.getText().toString().trim();

        // Récupérer le rôle sélectionné
        String role = getSelectedRole();

        // Validation des champs obligatoires
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

        // Créer l'objet data pour Firebase
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("phoneNumber", phone);
        data.put("role", role);
        data.put("address", address);
        data.put("location", workLocation);
        data.put("hiredDate", new java.util.Date());

        // Ajouter à Firebase
        db.collection("Employees")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(Ajout.this, "Employee added successfully", Toast.LENGTH_SHORT).show();

                        // Redirection vers la page Consultation
                        Intent intent = new Intent(Ajout.this, Consultation.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // Fermer l'activité actuelle
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Error adding employee: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
        return "";
    }

    private void clearFields() {
        edtName.setText("");
        edtEmail.setText("");
        edtPhone.setText("");
        edtAddress.setText("");
        edtWorkLocation.setText("");
        rgRole.clearCheck();
    }
}