/*
package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bestmlawi.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Suppression extends Activity {
    private Spinner spEmployee;
    private Button btnSupprimer;
    private Button btnRetour;
    private ArrayAdapter<Employee> adpEmployee;
    private FirebaseFirestore db;
    private List<String> firestoreDocumentIds;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suppression);
        initialiser();
        ecouteurs();
        remplir();
    }

    private void initialiser() {
        spEmployee = findViewById(R.id.spEmployee);
        btnSupprimer = findViewById(R.id.btnSupprimer);
        btnRetour = findViewById(R.id.btnRetour);

        adpEmployee = new ArrayAdapter<Employee>(this, android.R.layout.simple_spinner_item);
        adpEmployee.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEmployee.setAdapter(adpEmployee);

        db = FirebaseFirestore.getInstance();
        firestoreDocumentIds = new ArrayList<>();
    }

    private void ecouteurs() {
        btnSupprimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                supprimerEmployee();
            }
        });

        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void remplir() {
        db.collection("Employees")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            adpEmployee.clear();
                            firestoreDocumentIds.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String documentId = document.getId();

                                // Créer l'objet Employee avec les données de Firestore
                                Employee employee = document.toObject(Employee.class);
                                employee.setId(documentId);

                                adpEmployee.add(employee);
                                firestoreDocumentIds.add(documentId);
                            }

                            adpEmployee.notifyDataSetChanged();

                            if (adpEmployee.getCount() == 0) {
                                Toast.makeText(getApplicationContext(), "Aucun employé trouvé", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Erreur lors de la récupération des employés", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    protected void supprimerEmployee() {
        int selectedPosition = spEmployee.getSelectedItemPosition();
        if (selectedPosition >= 0 && selectedPosition < firestoreDocumentIds.size()) {
            String documentId = firestoreDocumentIds.get(selectedPosition);
            Employee selectedEmployee = adpEmployee.getItem(selectedPosition);

            db.collection("Employees").document(documentId)
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getApplicationContext(), "Employé supprimé avec succès", Toast.LENGTH_SHORT).show();

                            // Redirection vers Consultation
                            Intent intent = new Intent(Suppression.this, Consultation.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Erreur lors de la suppression: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(), "Veuillez sélectionner un employé", Toast.LENGTH_SHORT).show();
        }
    }
}

*/