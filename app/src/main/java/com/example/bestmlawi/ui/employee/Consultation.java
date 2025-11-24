package com.example.bestmlewi.ui.employee;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bestmlewi.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Consultation extends Activity {
    private ListView lstEmployee;
    private Button btnRetour, btnAddCollab;
    private TextView txtTitle, txtOurEmployee, txtSearch, txtClearFilters;
    private ArrayAdapter<String> adpEmployee;
    private FirebaseFirestore db;
    private List<Employee> employeeList;
    private List<String> employeeStringList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consultation);
        initialiser();
        ecouteurs();
        remplir();
    }

    private void initialiser() {
        lstEmployee = findViewById(R.id.lstEmployee);
        btnRetour = findViewById(R.id.btnRetour);
        btnAddCollab = findViewById(R.id.btnAddCollab);
        txtTitle = findViewById(R.id.txtTitle);
        txtOurEmployee = findViewById(R.id.txtOurEmployee);
        txtSearch = findViewById(R.id.txtSearch);
        txtClearFilters = findViewById(R.id.txtClearFilters);

        employeeList = new ArrayList<>();
        employeeStringList = new ArrayList<>();

        adpEmployee = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, employeeStringList);
        lstEmployee.setAdapter(adpEmployee);
        db = FirebaseFirestore.getInstance();
    }

    private void ecouteurs() {
        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAddCollab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Consultation.this, "Ajouter un collaborateur", Toast.LENGTH_SHORT).show();
            }
        });

        txtClearFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remplir();
            }
        });

        lstEmployee.setOnItemClickListener((parent, view, position, id) -> {
            Employee employee = employeeList.get(position);
            Toast.makeText(Consultation.this, "Sélectionné: " + employee.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    private void remplir() {
        db.collection("Employees")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            employeeList.clear();
                            employeeStringList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Employee employee = document.toObject(Employee.class);
                                employee.setId(document.getId());
                                employeeList.add(employee);

                                String displayText = employee.getName() + " - " + employee.getRole();
                                if (employee.getLocation() != null && !employee.getLocation().isEmpty()) {
                                    displayText += "\n" + employee.getLocation();
                                }
                                employeeStringList.add(displayText);
                            }

                            adpEmployee.notifyDataSetChanged();

                            if (employeeList.isEmpty()) {
                                // Ajouter des données de test si la collection est vide
                                ajouterDonneesTest();
                            } else {
                                Toast.makeText(getApplicationContext(), "Données chargées depuis Firebase", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Erreur Firebase: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            // En cas d'erreur, charger des données de test
                            ajouterDonneesTest();
                        }
                    }
                });
    }

    private void ajouterDonneesTest() {
        employeeList.clear();
        employeeStringList.clear();

        // Données de test temporaires
        employeeList.add(new Employee("1", "Ahmed Ben Youssef", "ahmed@example.com", "12345678", "collaborator", null, "1", "Sfax, Sakit zite"));
        employeeList.add(new Employee("2", "Nidhal Chebbi", "nidhal@example.com", "23456789", "collaborator", null, "1", "Sfax, Sakit zite"));
        employeeList.add(new Employee("3", "Walid Laaroussi", "walid@example.com", "34567890", "collaborator", null, "1", "Sfax, Sakit zite"));
        employeeList.add(new Employee("4", "Amal Hammami", "amal@example.com", "45678901", "collaborator", null, "1", null));

        for (Employee employee : employeeList) {
            String displayText = employee.getName() + " - " + employee.getRole();
            if (employee.getLocation() != null && !employee.getLocation().isEmpty()) {
                displayText += "\n" + employee.getLocation();
            }
            employeeStringList.add(displayText);
        }

        adpEmployee.notifyDataSetChanged();
        Toast.makeText(this, "Données de test chargées", Toast.LENGTH_SHORT).show();
    }
}