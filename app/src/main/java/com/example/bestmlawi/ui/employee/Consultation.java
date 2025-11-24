package com.example.bestmlawi.ui.employee;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bestmlawi.R;
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

        // CHANGER ICI : btnAddCollab → btnAddEmployee
        btnAddCollab = findViewById(R.id.btnAddEmployee);

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
                Intent intent = new Intent(Consultation.this, Ajout.class);
                startActivity(intent);
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
            showEmployeeDialog(employee);
        });
    }

    private void showEmployeeDialog(Employee employee) {
        // Créer le dialog avec AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflater le layout personnalisé
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.employee_dialog, null);
        builder.setView(dialogView);

        // Initialiser les vues du dialog
        TextView txtEmployeeName = dialogView.findViewById(R.id.txtDialogEmployeeName);
        TextView txtEmployeeRole = dialogView.findViewById(R.id.txtDialogEmployeeRole);
        TextView txtEmployeeLocation = dialogView.findViewById(R.id.txtDialogEmployeeLocation);
        Button btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);
        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Remplir les informations de l'employé
        txtEmployeeName.setText(employee.getName());
        txtEmployeeRole.setText(employee.getRole());

        if (employee.getLocation() != null && !employee.getLocation().isEmpty()) {
            txtEmployeeLocation.setText(employee.getLocation());
        } else {
            txtEmployeeLocation.setText("No location specified");
        }

        // Créer et afficher le dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Écouteurs pour les boutons du dialog
        btnViewDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                viewEmployeeDetails(employee);
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                editEmployee(employee);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showDeleteConfirmationDialog(employee);
            }
        });
    }

    private void showDeleteConfirmationDialog(Employee employee) {
        // Créer un dialog de confirmation de suppression
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflater le layout de confirmation de suppression
        LayoutInflater inflater = getLayoutInflater();
        View deleteDialogView = inflater.inflate(R.layout.suppression, null);
        builder.setView(deleteDialogView);

        // Initialiser les vues du dialog de suppression
        TextView txtDeleteTitle = deleteDialogView.findViewById(R.id.txtDeleteTitle);
        TextView txtDeleteMessage = deleteDialogView.findViewById(R.id.txtDeleteMessage);
        Button btnCancelDelete = deleteDialogView.findViewById(R.id.btnCancelDelete);
        Button btnConfirmDelete = deleteDialogView.findViewById(R.id.btnConfirmDelete);

        // Personnaliser le message
        if (txtDeleteTitle != null) {
            txtDeleteTitle.setText("Supprimer l'employé");
        }
        if (txtDeleteMessage != null) {
            txtDeleteMessage.setText("Êtes-vous sûr de vouloir supprimer " + employee.getName() + " ?\n\nCette action est irréversible !");
        }

        // Créer et afficher le dialog
        AlertDialog deleteDialog = builder.create();
        deleteDialog.show();

        // Écouteurs pour les boutons du dialog de suppression
        btnCancelDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog.dismiss();
            }
        });

        btnConfirmDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog.dismiss();
                deleteEmployee(employee);
            }
        });
    }

    private void viewEmployeeDetails(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Employee Details");

        String details = "Name: " + employee.getName() + "\n" +
                "Role: " + employee.getRole() + "\n" +
                "Location: " + (employee.getLocation() != null ? employee.getLocation() : "Not specified") + "\n" +
                "Email: " + (employee.getEmail() != null ? employee.getEmail() : "Not specified") + "\n" +
                "Phone: " + (employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "Not specified") + "\n" +
                "Hired Date: " + (employee.getHiredDate() != null ? employee.getHiredDate().toString() : "Not specified");

        builder.setMessage(details);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void editEmployee(Employee employee) {
        Intent intent = new Intent(Consultation.this, Modification.class);
        intent.putExtra("EMPLOYEE_ID", employee.getId());
        intent.putExtra("EMPLOYEE_NAME", employee.getName());
        intent.putExtra("EMPLOYEE_ROLE", employee.getRole());
        intent.putExtra("EMPLOYEE_LOCATION", employee.getLocation());
        intent.putExtra("EMPLOYEE_EMAIL", employee.getEmail());
        intent.putExtra("EMPLOYEE_PHONE", employee.getPhoneNumber());
        intent.putExtra("EMPLOYEE_PointOfSale", employee.getPointOfSaleId());
        startActivity(intent);
    }

    private void deleteEmployee(Employee employee) {
        // Supprimer l'employé de Firebase
        db.collection("Employees").document(employee.getId())
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Consultation.this, "Employé supprimé avec succès", Toast.LENGTH_SHORT).show();
                            // Recharger la liste
                            remplir();
                        } else {
                            Toast.makeText(Consultation.this, "Erreur lors de la suppression: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
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
                            employeeList.clear();
                            employeeStringList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Convertir le document en objet Employee
                                Employee employee = document.toObject(Employee.class);
                                employee.setId(document.getId());
                                employeeList.add(employee);

                                // Créer le texte à afficher dans la liste
                                String displayText = employee.getName() + " - " + employee.getRole();
                                if (employee.getLocation() != null && !employee.getLocation().isEmpty()) {
                                    displayText += "\n" + employee.getLocation();
                                }
                                employeeStringList.add(displayText);
                            }

                            adpEmployee.notifyDataSetChanged();

                            if (employeeList.isEmpty()) {
                                Toast.makeText(getApplicationContext(), "No employees found", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Data loaded from Firebase", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Firebase error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}