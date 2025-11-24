package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Modification extends Activity {
    private EditText edtName, edtEmail, edtPhone, edtLocation;
    private RadioGroup rgRole;
    private RadioButton rbCollaborator, rbCoordinator, rbDeliver;
    private Button btnRetour, btnModifier;
    private TextView txtEmployeeInfo;

    private FirebaseFirestore db;
    private String employeeId = "";
    private String employeeName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modification);
        initialiser();
        ecouteurs();
        chargerDonneesEmployee();
    }

    private void initialiser() {
        // Initialiser les TextView
        txtEmployeeInfo = findViewById(R.id.txtEmployeeInfo);

        // Initialiser les EditText
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtLocation = findViewById(R.id.edtLocation);

        // Initialiser les RadioButtons
        rgRole = findViewById(R.id.rgRole);
        rbCollaborator = findViewById(R.id.rbCollaborator);
        rbCoordinator = findViewById(R.id.rbCoordinator);
        rbDeliver = findViewById(R.id.rbDeliver);

        // Initialiser les boutons
        btnRetour = findViewById(R.id.btnRetour);
        btnModifier = findViewById(R.id.btnModifier);

        db = FirebaseFirestore.getInstance();
    }

    private void ecouteurs() {
        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnModifier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modifierEmployee();
            }
        });
    }

    private void chargerDonneesEmployee() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EMPLOYEE_ID")) {
            // Récupérer les données de l'employé
            employeeId = intent.getStringExtra("EMPLOYEE_ID");
            employeeName = intent.getStringExtra("EMPLOYEE_NAME");
            String role = intent.getStringExtra("EMPLOYEE_ROLE");
            String location = intent.getStringExtra("EMPLOYEE_LOCATION");
            String email = intent.getStringExtra("EMPLOYEE_EMAIL");
            String phone = intent.getStringExtra("EMPLOYEE_PHONE");
            String address = intent.getStringExtra("EMPLOYEE_ADDRESS");

            // Afficher les informations de l'employé
            txtEmployeeInfo.setText("Modification de : " + employeeName);

            // Remplir les champs avec les données existantes
            edtName.setText(employeeName);
            edtEmail.setText(email != null ? email : "");
            edtPhone.setText(phone != null ? phone : "");
            edtLocation.setText(location != null ? location : "");

            // Sélectionner le rôle approprié
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
            Toast.makeText(this, "Erreur: Aucun employé sélectionné", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void modifierEmployee() {
        // Valider les champs
        if (!valider()) {
            return;
        }

        // Récupérer les nouvelles valeurs
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();
        String role = getSelectedRole();

        // Créer les mises à jour
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phoneNumber", phone);
        updates.put("location", location);
        updates.put("role", role);

        // Mettre à jour dans Firebase
        db.collection("Employees").document(employeeId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(Modification.this, "Employé modifié avec succès", Toast.LENGTH_SHORT).show();

                        // Redirection vers Consultation
                        Intent intent = new Intent(Modification.this, Consultation.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Modification.this, "Erreur lors de la modification: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean valider() {
        if (edtName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (edtEmail.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (rgRole.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Veuillez sélectionner un rôle", Toast.LENGTH_SHORT).show();
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